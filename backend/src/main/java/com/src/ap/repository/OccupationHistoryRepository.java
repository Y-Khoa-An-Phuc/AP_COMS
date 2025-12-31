package com.src.ap.repository;

import com.src.ap.dto.occupation.OccupationHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OccupationHistoryRepository {

    private static final String HISTORY_SQL = """
            SELECT
                tx_id,
                op,
                id,
                Oid AS oid,
                name,
                Oname AS oname,
                description,
                Odescription AS odescription,
                created_at,
                Ocreated_at AS ocreated_at,
                updated_at,
                Oupdated_at AS oupdated_at,
                changed_at,
                actor
            FROM dbo.hg_occupations
            WHERE id = ?
            ORDER BY changed_at DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<OccupationHistoryResponse> rowMapper = (rs, rowNum) -> OccupationHistoryResponse.builder()
            .txId(rs.getLong("tx_id"))
            .op(rs.getString("op"))
            .id(rs.getLong("id"))
            .oId(rs.getString("oid"))
            .name(rs.getString("name"))
            .oName(rs.getString("oname"))
            .description(rs.getString("description"))
            .oDescription(rs.getString("odescription"))
            .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
            .oCreatedAt(rs.getString("ocreated_at"))
            .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
            .oUpdatedAt(rs.getString("oupdated_at"))
            .changedAt(rs.getTimestamp("changed_at") != null ? rs.getTimestamp("changed_at").toLocalDateTime() : null)
            .actor(rs.getString("actor"))
            .build();

    public List<OccupationHistoryResponse> findByOccupationId(Long occupationId) {
        return jdbcTemplate.query(HISTORY_SQL, rowMapper, occupationId);
    }

    public Map<Long, List<OccupationHistoryResponse>> findByOccupationIds(List<Long> occupationIds) {
        if (occupationIds == null || occupationIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = occupationIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = """
                SELECT
                    tx_id,
                    op,
                    id,
                    Oid AS oid,
                    name,
                    Oname AS oname,
                    description,
                    Odescription AS odescription,
                    created_at,
                    Ocreated_at AS ocreated_at,
                    updated_at,
                    Oupdated_at AS oupdated_at,
                    changed_at,
                    actor
                FROM dbo.hg_occupations
                WHERE id IN (%s)
                ORDER BY id, changed_at DESC
                """.formatted(placeholders);

        List<OccupationHistoryResponse> allHistories = jdbcTemplate.query(
                sql,
                rowMapper,
                occupationIds.toArray()
        );

        return allHistories.stream()
                .collect(Collectors.groupingBy(OccupationHistoryResponse::getId));
    }
}
