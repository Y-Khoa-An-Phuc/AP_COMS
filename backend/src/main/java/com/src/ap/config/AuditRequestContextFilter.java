package com.src.ap.config;

import com.src.ap.audit.AuditContextHolder;
import com.src.ap.audit.AuditRequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuditRequestContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String actor = resolveActor();
        AuditContextHolder.set(new AuditRequestContext(actor));
        try {
            filterChain.doFilter(request, response);
        } finally {
            AuditContextHolder.clear();
        }
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        }
        return "anonymous";
    }
}
