package com.hcl.bankease.service;

import com.hcl.bankease.dto.DepositWithdrawRequest;
import com.hcl.bankease.dto.AccountResponse;
import com.hcl.bankease.dto.TransactionResponse;
import com.hcl.bankease.dto.TransferRequest;
import com.hcl.bankease.entity.Account;
import com.hcl.bankease.entity.Transaction;
import com.hcl.bankease.entity.User;
import com.hcl.bankease.exception.InsufficientBalanceException;
import com.hcl.bankease.exception.ResourceNotFoundException;
import com.hcl.bankease.repository.AccountRepository;
import com.hcl.bankease.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Account createAccountForUser(User user, Account.AccountType type) {
        Account account = Account.builder()
                .user(user)
                .accountNumber(generateAccountNumber())
                .accountType(type)
                .balance(BigDecimal.ZERO)
                .currency("INR")
                .status(Account.AccountStatus.ACTIVE)
                .build();
        return accountRepository.save(account);
    }

    public List<AccountResponse> getAccountsForUser(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(this::toAccountResponse)
                .collect(Collectors.toList());
    }

    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = findAccountOrThrow(accountNumber);
        return toAccountResponse(account);
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account fromAccount = findAccountOrThrow(request.getFromAccountNumber());
        Account toAccount = findAccountOrThrow(request.getToAccountNumber());

        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Source account is not active");
        }
        if (toAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Destination account is not active");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in account " + fromAccount.getAccountNumber());
        }

        // Debit sender
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        accountRepository.save(fromAccount);

        // Credit receiver
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        accountRepository.save(toAccount);

        // Ledger entry: debit side
        Transaction debitTxn = Transaction.builder()
                .account(fromAccount)
                .relatedAccount(toAccount)
                .transactionType(Transaction.TransactionType.TRANSFER_OUT)
                .amount(request.getAmount())
                .balanceAfter(fromAccount.getBalance())
                .description(request.getDescription())
                .status(Transaction.TransactionStatus.SUCCESS)
                .build();
        transactionRepository.save(debitTxn);

        // Ledger entry: credit side
        Transaction creditTxn = Transaction.builder()
                .account(toAccount)
                .relatedAccount(fromAccount)
                .transactionType(Transaction.TransactionType.TRANSFER_IN)
                .amount(request.getAmount())
                .balanceAfter(toAccount.getBalance())
                .description(request.getDescription())
                .status(Transaction.TransactionStatus.SUCCESS)
                .build();
        transactionRepository.save(creditTxn);

        return toTransactionResponse(debitTxn);
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        Account account = findAccountOrThrow(accountNumber);
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(account.getId()).stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TransactionResponse deposit(DepositWithdrawRequest request) {
        Account account = findAccountOrThrow(request.getAccountNumber());
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .account(account)
                .transactionType(Transaction.TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .balanceAfter(account.getBalance())
                .description(request.getDescription())
                .status(Transaction.TransactionStatus.SUCCESS)
                .build();
        transactionRepository.save(txn);

        return toTransactionResponse(txn);
    }

    @Transactional
    public TransactionResponse withdraw(DepositWithdrawRequest request) {
        Account account = findAccountOrThrow(request.getAccountNumber());
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is not active");
        }
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in account " + account.getAccountNumber());
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .account(account)
                .transactionType(Transaction.TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .balanceAfter(account.getBalance())
                .description(request.getDescription())
                .status(Transaction.TransactionStatus.SUCCESS)
                .build();
        transactionRepository.save(txn);

        return toTransactionResponse(txn);
    }

    private Account findAccountOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found: " + accountNumber));
    }

    private String generateAccountNumber() {
        String candidate;
        do {
            candidate = "AC" + (100000000L + (long) (Math.random() * 900000000L));
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }

    private AccountResponse toAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType().name())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .status(account.getStatus().name())
                .build();
    }

    private TransactionResponse toTransactionResponse(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .transactionType(txn.getTransactionType().name())
                .amount(txn.getAmount())
                .balanceAfter(txn.getBalanceAfter())
                .description(txn.getDescription())
                .status(txn.getStatus().name())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}