package com.hsbc.banking.transaction.service;

import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction createTransaction(String orderId,
                                         BigDecimal amount,
                                         String type,
                                         String category,
                                         String description) {
        Transaction transaction = new Transaction(orderId, amount, type, category, description);
        return transactionRepository.save(transaction);
    }
} 