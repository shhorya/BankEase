package com.hcl.bankease.controller;

import com.hcl.bankease.dto.DepositWithdrawRequest;
import com.hcl.bankease.dto.TransactionResponse;
import com.hcl.bankease.entity.Transaction;
import com.hcl.bankease.exception.ResourceNotFoundException;
import com.hcl.bankease.service.AccountService;
import com.hcl.bankease.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExtraController {

    private final JdbcTemplate jdbc;
    private final AccountService accountService;
    private final UserService userService;

    public record BillReq(@NotBlank String accountNumber, @NotNull Long billerId,
                          @NotNull @DecimalMin("0.01") BigDecimal amount) {}
    public record LoanReq(@NotBlank String accountNumber, @NotNull @DecimalMin("1000") BigDecimal principal,
                          @NotNull @Min(3) @Max(120) Integer tenureMonths) {}
    public record InvestReq(@NotBlank String accountNumber, @NotBlank String type,
                            @NotNull @DecimalMin("100") BigDecimal amount) {}

    // ---------- Bills ----------
    @GetMapping("/billers")
    public List<Map<String, Object>> billers() {
        return jdbc.queryForList("SELECT id, name, category FROM billers");
    }

    @PostMapping("/bills/pay")
    public TransactionResponse pay(@Valid @RequestBody BillReq r, Authentication auth) {
        accountService.assertOwner(r.accountNumber(), auth.getName());
        List<String> names = jdbc.queryForList("SELECT name FROM billers WHERE id=?", String.class, r.billerId());
        if (names.isEmpty()) throw new ResourceNotFoundException("Biller not found");
        return accountService.debit(req(r.accountNumber(), r.amount(), "Bill: " + names.get(0)),
                Transaction.TransactionType.BILL_PAYMENT);
    }

    // ---------- Loans ----------
    @Transactional
    @PostMapping("/loans/apply")
    public Map<String, Object> apply(@Valid @RequestBody LoanReq r, Authentication auth) {
        accountService.assertOwner(r.accountNumber(), auth.getName());
        double rate = 10.5 / 12 / 100;
        int n = r.tenureMonths();
        double p = r.principal().doubleValue();
        double emi = p * rate * Math.pow(1 + rate, n) / (Math.pow(1 + rate, n) - 1);
        BigDecimal emiBd = BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);

        jdbc.update("INSERT INTO loans(user_id, account_number, principal, tenure_months, interest_rate, emi, status) "
                        + "VALUES (?,?,?,?,?,?,'PENDING')",
                uid(auth), r.accountNumber(), r.principal(), n, 10.5, emiBd);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        String status = "PENDING"; // above 5,00,000 needs admin approval
        if (r.principal().compareTo(new BigDecimal("500000")) <= 0) {
            disburse(id, r.accountNumber(), r.principal());
            status = "APPROVED";
        }
        return Map.of("id", id, "status", status, "emi", emiBd);
    }

    @GetMapping("/loans")
    public List<Map<String, Object>> myLoans(Authentication auth) {
        return jdbc.queryForList("SELECT id, account_number, principal, tenure_months, emi, status, created_at "
                + "FROM loans WHERE user_id=? ORDER BY id DESC", uid(auth));
    }

    @GetMapping("/loans/pending")
    public List<Map<String, Object>> pending(Authentication auth) {
        requireAdmin(auth);
        return jdbc.queryForList("SELECT id, account_number, principal, tenure_months, emi, status "
                + "FROM loans WHERE status='PENDING' ORDER BY id");
    }

    @Transactional
    @PostMapping("/loans/{id}/approve")
    public Map<String, Object> approve(@PathVariable long id, Authentication auth) {
        requireAdmin(auth);
        var rows = jdbc.queryForList(
                "SELECT account_number, principal, status FROM loans WHERE id=? FOR UPDATE", id);
        if (rows.isEmpty()) throw new ResourceNotFoundException("Loan not found");
        var row = rows.get(0);
        if (!"PENDING".equals(row.get("status"))) throw new IllegalStateException("Loan is not pending");
        disburse(id, (String) row.get("account_number"), (BigDecimal) row.get("principal"));
        return Map.of("id", id, "status", "APPROVED");
    }

    // ---------- Investments ----------
    @Transactional
    @PostMapping("/investments")
    public Map<String, Object> invest(@Valid @RequestBody InvestReq r, Authentication auth) {
        if (!List.of("FD", "SIP", "MUTUAL_FUND").contains(r.type()))
            throw new IllegalArgumentException("Invalid investment type");
        accountService.assertOwner(r.accountNumber(), auth.getName());
        accountService.debit(req(r.accountNumber(), r.amount(), "Investment: " + r.type()),
                Transaction.TransactionType.WITHDRAWAL);
        jdbc.update("INSERT INTO investments(user_id, type, amount) VALUES (?,?,?)",
                uid(auth), r.type(), r.amount());
        return Map.of("status", "INVESTED");
    }

    @GetMapping("/investments")
    public List<Map<String, Object>> myInvestments(Authentication auth) {
        return jdbc.queryForList("SELECT id, type, amount, status, created_at FROM investments "
                + "WHERE user_id=? ORDER BY id DESC", uid(auth));
    }

    // ---------- Audit ----------
    @GetMapping("/audit")
    public List<Map<String, Object>> audit(Authentication auth) {
        return jdbc.queryForList("SELECT action, details, created_at FROM audit_logs "
                + "WHERE user_email=? ORDER BY id DESC LIMIT 50", auth.getName());
    }

    // ---------- helpers ----------
    private void disburse(long id, String acc, BigDecimal amount) {
        accountService.deposit(req(acc, amount, "Loan disbursed #" + id));
        jdbc.update("UPDATE loans SET status='APPROVED' WHERE id=?", id);
    }

    private DepositWithdrawRequest req(String acc, BigDecimal amt, String desc) {
        DepositWithdrawRequest d = new DepositWithdrawRequest();
        d.setAccountNumber(acc);
        d.setAmount(amt);
        d.setDescription(desc);
        return d;
    }

    private Long uid(Authentication auth) {
        return userService.findByEmail(auth.getName()).getId();
    }

    private void requireAdmin(Authentication auth) {
        if (auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN")))
            throw new AccessDeniedException("Admin only");
    }
}