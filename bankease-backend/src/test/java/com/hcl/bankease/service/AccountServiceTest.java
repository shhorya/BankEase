package com.hcl.bankease.service;

import com.hcl.bankease.dto.DepositWithdrawRequest;
import com.hcl.bankease.dto.TransferRequest;
import com.hcl.bankease.entity.Account;
import com.hcl.bankease.exception.InsufficientBalanceException;
import com.hcl.bankease.repository.AccountRepository;
import com.hcl.bankease.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository accountRepository;
    @Mock TransactionRepository transactionRepository;
    @InjectMocks AccountService service;

    private Account acc(String number, String balance) {
        return Account.builder().accountNumber(number).balance(new BigDecimal(balance)).build();
    }

    private DepositWithdrawRequest req(String number, String amount) {
        DepositWithdrawRequest r = new DepositWithdrawRequest();
        r.setAccountNumber(number);
        r.setAmount(new BigDecimal(amount));
        return r;
    }

    @Test
    void withdraw_moreThanBalance_throws() {
        when(accountRepository.findForUpdate("AC1")).thenReturn(Optional.of(acc("AC1", "50")));
        assertThrows(InsufficientBalanceException.class, () -> service.withdraw(req("AC1", "100")));
    }

    @Test
    void deposit_increasesBalance() {
        Account a = acc("AC1", "50");
        when(accountRepository.findForUpdate("AC1")).thenReturn(Optional.of(a));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        service.deposit(req("AC1", "100"));
        assertEquals(0, a.getBalance().compareTo(new BigDecimal("150")));
    }

    @Test
    void transfer_toSameAccount_throws() {
        TransferRequest t = new TransferRequest();
        t.setFromAccountNumber("AC1");
        t.setToAccountNumber("AC1");
        t.setAmount(new BigDecimal("10"));
        assertThrows(IllegalArgumentException.class, () -> service.transfer(t));
    }
}