package com.hsbc.banking.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsbc.banking.transaction.dto.CreateTransactionRequest;
import com.hsbc.banking.transaction.dto.UpdateTransactionRequest;
import com.hsbc.banking.transaction.exception.ConcurrentUpdateException;
import com.hsbc.banking.transaction.exception.InsufficientBalanceException;
import com.hsbc.banking.transaction.exception.InvalidTransactionException;
import com.hsbc.banking.transaction.exception.TransactionNotFoundException;
import com.hsbc.banking.transaction.model.AuditLog;
import com.hsbc.banking.transaction.model.Page;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionCategory;
import com.hsbc.banking.transaction.repository.AuditLogRepository;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final ExternalAccountService externalAccountService;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public TransactionService(TransactionRepository transactionRepository,
                              ExternalAccountService externalAccountService,
                              AuditLogRepository auditLogRepository,
                              ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.externalAccountService = externalAccountService;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    public Transaction createTransaction(CreateTransactionRequest request) {
        Transaction transaction = Transaction.create(
                request.orderId(),
                request.accountId(),
                request.amount(),
                request.type(),
                request.category(),
                request.description()
        );

        // Check if account has sufficient balance for debit transactions
        if (transaction.getType().isDebit() &&
            !externalAccountService.hasSufficientBalance(transaction.getAccountId(), transaction.getAmount())) {
            throw new InsufficientBalanceException(
                    transaction.getAccountId(),
                    "Insufficient balance for transaction amount: " + transaction.getAmount()
            );
        }

        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        // Create audit log before deletion
        String details;
        try {
            details = objectMapper.writeValueAsString(transaction);
        } catch (Exception e) {
            details = "Failed to serialize transaction: " + e.getMessage();
        }

        auditLogRepository.save(new AuditLog(
                "DELETE",
                "Transaction",
                String.valueOf(id),
                "Deleted transaction: " + details
        ));

        transactionRepository.deleteById(id);
    }

    public Transaction updateTransaction(Long id, UpdateTransactionRequest request) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                Transaction transaction = transactionRepository.findById(id)
                        .orElseThrow(() -> new TransactionNotFoundException(id));

                // Validate category
                try {
                    TransactionCategory.fromString(request.category());
                } catch (IllegalArgumentException e) {
                    throw new InvalidTransactionException(Map.of(
                            "errors", List.of("Invalid transaction category. Valid categories are: " + 
                                    java.util.Arrays.toString(TransactionCategory.values()))
                    ));
                }

                // Record the old state
                String oldState;
                try {
                    oldState = objectMapper.writeValueAsString(transaction);
                } catch (Exception e) {
                    oldState = "Failed to serialize old state: " + e.getMessage();
                }

                // Only update category and description
                transaction.setCategory(TransactionCategory.fromString(request.category()));
                transaction.setDescription(request.description());
                transaction.setUpdatedAt(LocalDateTime.now());

                // Do the update
                Transaction updatedTransaction = transactionRepository.update(transaction);

                // Record the audit log only after the update is successful
                String newState;
                try {
                    newState = objectMapper.writeValueAsString(updatedTransaction);
                } catch (Exception e) {
                    newState = "Failed to serialize new state: " + e.getMessage();
                }

                auditLogRepository.save(new AuditLog(
                        "UPDATE",
                        "Transaction",
                        String.valueOf(id),
                        "Updated transaction from: " + oldState + " to: " + newState
                ));

                return updatedTransaction;
            } catch (ConcurrentUpdateException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw e;
                }
                // Add some random delay before retrying
                try {
                    Thread.sleep((long) (Math.random() * 100));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted while retrying update", ie);
                }
            }
        }
        
        throw new IllegalStateException("Should never reach here");
    }

    public Page<Transaction> listTransactions(int pageNumber, int pageSize) {
        // Validate pageNumber and pageSize, return empty list if invalid
        if (pageNumber < 1 || pageSize <= 0) {
            return Page.of(List.of(), pageNumber, pageSize, 0);
        }

        // Limit pageSize to 100
        int limitedPageSize = Math.min(pageSize, 100);

        // Calculate offset based on pageNumber and pageSize
        int offset = (pageNumber - 1) * limitedPageSize;
        
        // Get total number of elements
        long totalElements = transactionRepository.count();
        
        // Return empty list if offset is greater than total elements
        if (offset >= totalElements) {
            return Page.of(List.of(), pageNumber, limitedPageSize, totalElements);
        }
        
        // Get transactions
        List<Transaction> transactions = transactionRepository.findAll(offset, limitedPageSize);
        
        return Page.of(transactions, pageNumber, limitedPageSize, totalElements);
    }
} 