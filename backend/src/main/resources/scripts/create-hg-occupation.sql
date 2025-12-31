-- Audit history table for Occupation (SQL Server).
IF OBJECT_ID('dbo.hg_occupations', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.hg_occupations (
        tx_id UNIQUEIDENTIFIER NULL,
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
    );

    CREATE INDEX IX_hg_occupations_Id_ChangedAt
        ON dbo.hg_occupations (id, changed_at DESC);

    CREATE INDEX IX_hg_occupations_TxId
        ON dbo.hg_occupations (tx_id);
END
