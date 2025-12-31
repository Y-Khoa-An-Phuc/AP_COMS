package com.src.ap.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditSessionContextService {

    private final JdbcTemplate jdbcTemplate;

    public void applySessionContextForWrite() {
        AuditRequestContext context = AuditContextHolder.getOrCreate("anonymous");
        jdbcTemplate.update("EXEC sp_set_session_context N'actor', ?", context.actor());
    }
}
