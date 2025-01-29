package com.hsbc.banking.transaction.service;

import com.hsbc.banking.transaction.dto.CreateTransactionRequest;
import com.hsbc.banking.transaction.dto.UpdateTransactionRequest;
import com.hsbc.banking.transaction.exception.InsufficientBalanceException;
import com.hsbc.banking.transaction.exception.InvalidTransactionException;
import com.hsbc.banking.transaction.exception.TransactionNotFoundException;
import com.hsbc.banking.transaction.model.AuditLog;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionCategory;
import com.hsbc.banking.transaction.repository.AuditLogRepository;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        // Find and validate transaction exists
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

        // Create audit log before update
        String oldState;
        try {
            oldState = objectMapper.writeValueAsString(transaction);
        } catch (Exception e) {
            oldState = "Failed to serialize old state: " + e.getMessage();
        }

        // Update allowed fields
        transaction.setCategory(TransactionCategory.fromString(request.category()));
        transaction.setDescription(request.description());
        transaction.setUpdatedAt(LocalDateTime.now());

        // Save updated transaction
        Transaction updatedTransaction = transactionRepository.update(transaction);

        // Create audit log after update
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
    }
} 