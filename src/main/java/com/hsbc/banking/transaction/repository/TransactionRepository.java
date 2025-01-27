package com.hsbc.banking.transaction.repository;

import com.hsbc.banking.transaction.model.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(Long id);
    List<Transaction> findAll();
} 