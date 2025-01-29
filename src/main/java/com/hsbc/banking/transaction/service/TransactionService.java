package com.hsbc.banking.transaction.service;

import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionType;
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
                                         String accountId,
                                         BigDecimal amount,
                                         String type,
                                         String category,
                                         String description) {
        validateTransaction(orderId, accountId, amount, type, category, description);
        Transaction transaction = new Transaction(orderId, accountId, amount, TransactionType.valueOf(type), category, description);
        return transactionRepository.save(transaction);
    }

    private void validateTransaction(String orderId,
                                     String accountId,
                                     BigDecimal amount,
                                     String type,
                                     String category,
                                     String description) {
        // check if the type is valid
        TransactionType.valueOf(type.toUpperCase());
    }
} 