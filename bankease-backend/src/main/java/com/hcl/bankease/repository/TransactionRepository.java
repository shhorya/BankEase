package com.hcl.bankease.repository;

import com.hcl.bankease.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    List<Transaction> findByAccountIdAndTransactionTypeOrderByCreatedAtDesc(
            Long accountId, Transaction.TransactionType transactionType);
}