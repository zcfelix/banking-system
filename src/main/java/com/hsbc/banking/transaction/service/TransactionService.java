package com.hsbc.banking.transaction.service;

import com.hsbc.banking.transaction.exception.InvalidTransactionException;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionType;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class TransactionService {
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("^ORD-\\d{6,}$");
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^ACC-\\d{6,}$");
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final int MAX_DESCRIPTION_LENGTH = 100;

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
        List<String> validationErrors = validateTransaction(orderId, accountId, amount, type, category, description);
        
        if (!validationErrors.isEmpty()) {
            throw new InvalidTransactionException(Map.of("errors", validationErrors));
        }

        Transaction transaction = new Transaction(orderId, accountId, amount, TransactionType.valueOf(type), category, description);
        return transactionRepository.save(transaction);
    }

    private List<String> validateTransaction(String orderId,
                                          String accountId,
                                          BigDecimal amount,
                                          String type,
                                          String category,
                                          String description) {
        List<String> errors = new ArrayList<>();

        // Validate orderId
        if (orderId == null || !ORDER_ID_PATTERN.matcher(orderId).matches()) {
            errors.add("Order ID must start with 'ORD-' followed by at least 6 digits");
        }

        // Validate accountId
        if (accountId == null || !ACCOUNT_ID_PATTERN.matcher(accountId).matches()) {
            errors.add("Account ID must start with 'ACC-' followed by at least 6 digits");
        }

        // Validate amount
        if (amount != null) {
            if (amount.scale() > 2) {
                errors.add("Amount cannot have more than 2 decimal places");
            }
            
            if (amount.abs().compareTo(MIN_AMOUNT) < 0) {
                errors.add("Amount absolute value cannot be less than 0.01");
            }
        } else {
            errors.add("Amount cannot be null");
        }

        // Validate type and amount sign
        try {
            TransactionType transactionType = TransactionType.valueOf(type);
            if (amount != null) {
                if (transactionType == TransactionType.CREDIT && amount.compareTo(BigDecimal.ZERO) >= 0) {
                    errors.add("Amount must be negative for CREDIT transactions");
                } else if (transactionType == TransactionType.DEBIT && amount.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Amount must be positive for DEBIT transactions");
                }
            }
        } catch (IllegalArgumentException e) {
            errors.add("Invalid transaction type");
        }

        // Validate description
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            errors.add("Description cannot exceed 100 characters");
        }

        return errors;
    }
} 