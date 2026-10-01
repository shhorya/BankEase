package com.hcl.bankease.controller;

import com.hcl.bankease.dto.DepositWithdrawRequest;
import com.hcl.bankease.dto.AccountResponse;
import com.hcl.bankease.dto.TransactionResponse;
import com.hcl.bankease.dto.TransferRequest;
import com.hcl.bankease.entity.User;
import com.hcl.bankease.service.AccountService;
import com.hcl.bankease.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(accountService.getAccountsForUser(user.getId()));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber, Authentication auth) {
        accountService.assertOwner(accountNumber, auth.getName());
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @GetMapping("/{accountNumber}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @PathVariable String accountNumber, Authentication auth) {
        accountService.assertOwner(accountNumber, auth.getName());
        return ResponseEntity.ok(accountService.getTransactionHistory(accountNumber));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request, Authentication auth) {
        accountService.assertOwner(request.getFromAccountNumber(), auth.getName());
        return ResponseEntity.ok(accountService.transfer(request));
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody DepositWithdrawRequest request, Authentication auth) {
        accountService.assertOwner(request.getAccountNumber(), auth.getName());
        return ResponseEntity.ok(accountService.deposit(request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody DepositWithdrawRequest request, Authentication auth) {
        accountService.assertOwner(request.getAccountNumber(), auth.getName());
        return ResponseEntity.ok(accountService.withdraw(request));
    }
}