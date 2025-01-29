package com.hsbc.banking.transaction.service;

import com.hsbc.banking.transaction.exception.InsufficientBalanceException;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final ExternalAccountService externalAccountService;

    public TransactionService(TransactionRepository transactionRepository,
                            ExternalAccountService externalAccountService) {
        this.transactionRepository = transactionRepository;
        this.externalAccountService = externalAccountService;
    }

    public Transaction createTransaction(String orderId,
                                      String accountId,
                                      BigDecimal amount,
                                      String type,
                                      String category,
                                      String description) {
        Transaction transaction = Transaction.create(
            orderId, 
            accountId, 
            amount, 
            type, 
            category, 
            description
        );

        // Check if account has sufficient balance for debit transactions
        if (transaction.getType().isDebit() && !externalAccountService.hasSufficientBalance(accountId, amount)) {
            throw new InsufficientBalanceException(accountId, "Insufficient balance for transaction amount: " + amount);
        }

        return transactionRepository.save(transaction);
    }
} 