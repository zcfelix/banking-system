package com.hsbc.banking.transaction.service;

import com.hsbc.banking.transaction.dto.CreateTransactionRequest;
import com.hsbc.banking.transaction.exception.InsufficientBalanceException;
import com.hsbc.banking.transaction.exception.TransactionNotFoundException;
import com.hsbc.banking.transaction.model.AuditLog;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.repository.AuditLogRepository;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

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
} 