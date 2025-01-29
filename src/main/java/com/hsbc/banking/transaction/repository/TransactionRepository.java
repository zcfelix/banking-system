package com.hsbc.banking.transaction.repository;

import com.hsbc.banking.transaction.model.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Transaction update(Transaction transaction);
    Optional<Transaction> findById(Long id);
    Optional<Transaction> findByOrderId(String orderId);
    List<Transaction> findAll();
    void deleteById(Long id);
    void clear();
} 