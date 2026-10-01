package com.hcl.bankease.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final JdbcTemplate jdbc;

    public void log(String email, String action, String details) {
        jdbc.update("INSERT INTO audit_logs(user_email, action, details) VALUES (?,?,?)",
                email, action, details);
    }
}