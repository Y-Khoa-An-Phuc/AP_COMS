package com.src.ap.integration;

import com.src.ap.audit.AuditContextHolder;
import com.src.ap.audit.AuditRequestContext;
import com.src.ap.dto.occupation.OccupationHistoryResponse;
import com.src.ap.dto.occupation.OccupationRequest;
import com.src.ap.dto.occupation.OccupationResponse;
import com.src.ap.repository.OccupationHistoryRepository;
import com.src.ap.repository.OccupationRepository;
import com.src.ap.service.OccupationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OccupationHistoryAuditIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OccupationService occupationService;

    @Autowired
    private OccupationRepository occupationRepository;

    @Autowired
    private OccupationHistoryRepository occupationHistoryRepository;

    @BeforeAll
    void verifySqlServerAndSetupAudit() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            assumeTrue(product != null && product.toLowerCase().contains("microsoft"),
                    "SQL Server required for audit trigger tests");
        }

        jdbcTemplate.execute("IF OBJECT_ID('dbo.trg_Occupation_Audit', 'TR') IS NOT NULL DROP TRIGGER dbo.trg_Occupation_Audit;");
        jdbcTemplate.execute("IF OBJECT_ID('dbo.hg_occupations', 'U') IS NOT NULL DROP TABLE dbo.hg_occupations;");

        jdbcTemplate.execute("""
                CREATE TABLE dbo.hg_occupations (
                    tx_id BIGINT IDENTITY(1,1) PRIMARY KEY,
                    op CHAR(1) NOT NULL,
                    id BIGINT NOT NULL,
                    Oid CHAR(1) NULL,
                    name NVARCHAR(255) NULL,
                    Oname CHAR(1) NULL,
                    description NVARCHAR(2000) NULL,
                    Odescription CHAR(1) NULL,
                    created_at DATETIME2 NULL,
                    Ocreated_at CHAR(1) NULL,
                    updated_at DATETIME2 NULL,
                    Oupdated_at CHAR(1) NULL,
                    changed_at DATETIME2 NOT NULL CONSTRAINT DF_hg_occupations_changed_at DEFAULT SYSUTCDATETIME(),
                    actor NVARCHAR(128) NULL
                )
                """);

        jdbcTemplate.execute("CREATE INDEX IX_hg_occupations_Id_ChangedAt ON dbo.hg_occupations (id, changed_at DESC);");

        jdbcTemplate.execute("""
                CREATE TRIGGER dbo.trg_Occupation_Audit
                ON dbo.occupations
                AFTER INSERT, UPDATE, DELETE
                AS
                BEGIN
                    SET NOCOUNT ON;

                    DECLARE @actor NVARCHAR(128) = TRY_CAST(SESSION_CONTEXT(N'actor') AS NVARCHAR(128));

                    INSERT INTO dbo.hg_occupations (
                        op, id, Oid,
                        name, Oname,
                        description, Odescription,
                        created_at, Ocreated_at,
                        updated_at, Oupdated_at,
                        actor
                    )
                    SELECT
                        'C', i.id, 'M',
                        i.name, 'M',
                        i.description, 'M',
                        i.created_at, 'M',
                        i.updated_at, 'M',
                        @actor
                    FROM inserted i
                    LEFT JOIN deleted d ON d.id = i.id
                    WHERE d.id IS NULL;

                    INSERT INTO dbo.hg_occupations (
                        op, id, Oid,
                        name, Oname,
                        description, Odescription,
                        created_at, Ocreated_at,
                        updated_at, Oupdated_at,
                        actor
                    )
                    SELECT
                        'U', i.id, NULL,
                        i.name,
                        CASE
                            WHEN (i.name <> d.name) OR (i.name IS NULL AND d.name IS NOT NULL) OR (i.name IS NOT NULL AND d.name IS NULL)
                                THEN 'M'
                            ELSE NULL
                        END,
                        i.description,
                        CASE
                            WHEN (i.description <> d.description) OR (i.description IS NULL AND d.description IS NOT NULL) OR (i.description IS NOT NULL AND d.description IS NULL)
                                THEN 'M'
                            ELSE NULL
                        END,
                        i.created_at,
                        CASE
                            WHEN (i.created_at <> d.created_at) OR (i.created_at IS NULL AND d.created_at IS NOT NULL) OR (i.created_at IS NOT NULL AND d.created_at IS NULL)
                                THEN 'M'
                            ELSE NULL
                        END,
                        i.updated_at,
                        CASE
                            WHEN (i.updated_at <> d.updated_at) OR (i.updated_at IS NULL AND d.updated_at IS NOT NULL) OR (i.updated_at IS NOT NULL AND d.updated_at IS NULL)
                                THEN 'M'
                            ELSE NULL
                        END,
                        @actor
                    FROM inserted i
                    INNER JOIN deleted d ON d.id = i.id;

                    INSERT INTO dbo.hg_occupations (
                        op, id, Oid,
                        name, Oname,
                        description, Odescription,
                        created_at, Ocreated_at,
                        updated_at, Oupdated_at,
                        actor
                    )
                    SELECT
                        'D', d.id, NULL,
                        d.name, NULL,
                        d.description, NULL,
                        d.created_at, NULL,
                        d.updated_at, NULL,
                        @actor
                    FROM deleted d
                    LEFT JOIN inserted i ON i.id = d.id
                    WHERE i.id IS NULL;
                END
                """);
    }

    @BeforeEach
    void cleanTables() {
        occupationRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM dbo.HG_Occupation");
    }

    @Test
    void createOccupationWritesAudit() {
        OccupationResponse created = withAuditContext(() ->
                occupationService.createOccupation(new OccupationRequest("Human Resources", "Core functions")));

        List<OccupationHistoryResponse> history = occupationHistoryRepository.findByOccupationId(created.getId());
        assertThat(history).hasSize(1);
        OccupationHistoryResponse entry = history.get(0);
        assertThat(entry.getOp()).isEqualTo("C");
        assertThat(entry.getOId()).isEqualTo("M");
        assertThat(entry.getOName()).isEqualTo("M");
        assertThat(entry.getODescription()).isEqualTo("M");
        assertThat(entry.getOCreatedAt()).isEqualTo("M");
        assertThat(entry.getOUpdatedAt()).isEqualTo("M");
    }

    @Test
    void updateNameOnlyMarksNameAndUpdatedAt() {
        OccupationResponse created = withAuditContext(() ->
                occupationService.createOccupation(new OccupationRequest("Operations", "Ops desc")));

        withAuditContext(() ->
                occupationService.updateOccupation(created.getId(), new OccupationRequest("Operations Team", "Ops desc")));

        List<OccupationHistoryResponse> history = occupationHistoryRepository.findByOccupationId(created.getId());
        OccupationHistoryResponse updateEntry = history.stream()
                .filter(entry -> "U".equals(entry.getOp()))
                .findFirst()
                .orElseThrow();

        assertThat(updateEntry.getOName()).isEqualTo("M");
        assertThat(updateEntry.getODescription()).isNull();
        assertThat(updateEntry.getOCreatedAt()).isNull();
        assertThat(updateEntry.getOUpdatedAt()).isEqualTo("M");
    }

    @Test
    void deleteOccupationWritesSnapshot() {
        OccupationResponse created = withAuditContext(() ->
                occupationService.createOccupation(new OccupationRequest("Legal", "Legal desc")));

        withAuditContext(() -> {
            occupationService.deleteOccupation(created.getId());
            return null;
        });

        List<OccupationHistoryResponse> history = occupationHistoryRepository.findByOccupationId(created.getId());
        OccupationHistoryResponse deleteEntry = history.stream()
                .filter(entry -> "D".equals(entry.getOp()))
                .findFirst()
                .orElseThrow();

        assertThat(deleteEntry.getName()).isEqualTo("Legal");
        assertThat(deleteEntry.getOName()).isNull();
        assertThat(deleteEntry.getODescription()).isNull();
        assertThat(deleteEntry.getOCreatedAt()).isNull();
        assertThat(deleteEntry.getOUpdatedAt()).isNull();
    }

    private <T> T withAuditContext(AuditCallable<T> callable) {
        AuditContextHolder.set(new AuditRequestContext("test-user"));
        try {
            return callable.call();
        } finally {
            AuditContextHolder.clear();
        }
    }

    @FunctionalInterface
    private interface AuditCallable<T> {
        T call();
    }
}
