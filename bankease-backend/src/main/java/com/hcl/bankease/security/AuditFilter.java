package com.hcl.bankease.security;

import com.hcl.bankease.service.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuditFilter extends OncePerRequestFilter {
    private final AuditService audit;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(req, res);
        String uri = req.getRequestURI();
        if ("POST".equals(req.getMethod()) && uri.startsWith("/api/") && !uri.startsWith("/api/auth")) {
            Authentication a = (Authentication) req.getUserPrincipal();
            if (a != null && a.isAuthenticated()) {
                audit.log(a.getName(), "POST " + uri, "status=" + res.getStatus());
            }
        }
    }
}