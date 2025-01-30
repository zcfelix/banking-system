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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 100;
    
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

    @Cacheable(value = "transactions", key = "#id")
    public Transaction getTransaction(Long id) {
        logger.info("Fetching transaction from repository with id: {}", id);
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @CachePut(value = "transactions", key = "#result.id")
    public Transaction createTransaction(CreateTransactionRequest request) {
        logger.info("Creating new transaction and updating cache");
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

    @CacheEvict(value = "transactions", key = "#id")
    public void deleteTransaction(Long id) {
        logger.info("Deleting transaction from cache and repository with id: {}", id);
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

    @CachePut(value = "transactions", key = "#id")
    public Transaction updateTransaction(Long id, UpdateTransactionRequest request) {
        logger.info("Updating transaction in cache and repository with id: {}", id);
        int retryCount = 0;
        
        while (retryCount < MAX_RETRIES) {
            try {
                return doUpdateTransaction(id, request);
            } catch (ConcurrentUpdateException e) {
                retryCount++;
                if (retryCount >= MAX_RETRIES) {
                    logger.error("Failed to update transaction after {} retries", MAX_RETRIES);
                    throw e;
                }
                try {
                    Thread.sleep((long) (Math.random() * RETRY_DELAY_MS));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted while retrying update", ie);
                }
            }
        }
        
        throw new IllegalStateException("Should never reach here");
    }

    private Transaction doUpdateTransaction(Long id, UpdateTransactionRequest request) {
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
        String oldState = serializeTransaction(transaction);

        // Only update category and description
        transaction.setCategory(TransactionCategory.fromString(request.category()));
        transaction.setDescription(request.description());
        transaction.setUpdatedAt(LocalDateTime.now());

        // Do the update
        Transaction updatedTransaction = transactionRepository.update(transaction);

        // Record the audit log
        String newState = serializeTransaction(updatedTransaction);
        recordAuditLog(id, oldState, newState);

        return updatedTransaction;
    }

    private String serializeTransaction(Transaction transaction) {
        try {
            return objectMapper.writeValueAsString(transaction);
        } catch (Exception e) {
            logger.warn("Failed to serialize transaction: {}", e.getMessage());
            return "Failed to serialize transaction: " + e.getMessage();
        }
    }

    private void recordAuditLog(Long id, String oldState, String newState) {
        auditLogRepository.save(new AuditLog(
                "UPDATE",
                "Transaction",
                String.valueOf(id),
                "Updated transaction from: " + oldState + " to: " + newState
        ));
    }

    @Cacheable(value = "transactionPages", key = "#pageNumber + '-' + #pageSize")
    public Page<Transaction> listTransactions(int pageNumber, int pageSize) {
        logger.info("Fetching transaction page from repository: page={}, size={}", pageNumber, pageSize);
        // Validate pageNumber and pageSize, return empty list if invalid
        if (pageNumber < 1 || pageSize <= 0) {
            return Page.of(List.of(), pageNumber, pageSize, 0);
        }

        // Limit pageSize to MAX_PAGE_SIZE
        int limitedPageSize = Math.min(pageSize, MAX_PAGE_SIZE);

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