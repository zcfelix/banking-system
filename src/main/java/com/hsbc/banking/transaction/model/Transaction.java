package com.hsbc.banking.transaction.model;

import com.hsbc.banking.transaction.exception.InvalidTransactionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

public class Transaction {
    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("^ORD-\\d{6,}$");
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^ACC-\\d{6,}$");
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final int MAX_DESCRIPTION_LENGTH = 100;

    private Long id;
    private String orderId;
    private String accountId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionCategory category;
    private String description;
    private LocalDateTime createdAt;

    private Transaction(String orderId, String accountId, BigDecimal amount, TransactionType type, TransactionCategory category, String description) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public static Transaction create(String orderId, String accountId, BigDecimal amount, String type, String category, String description) {
        List<String> errors = validate(orderId, accountId, amount, type, category, description);
        if (!errors.isEmpty()) {
            throw new InvalidTransactionException(Map.of("errors", errors));
        }
        return new Transaction(orderId, accountId, amount, TransactionType.fromString(type), TransactionCategory.fromString(category), description);
    }

    private static List<String> validate(String orderId, String accountId, BigDecimal amount, String type, String category, String description) {
        List<String> errors = new ArrayList<>();
        TransactionType transactionType = null;

        // Validate orderId
        if (orderId == null || !ORDER_ID_PATTERN.matcher(orderId).matches()) {
            errors.add("Order ID must start with 'ORD-' followed by at least 6 digits");
        }

        // Validate accountId
        if (accountId == null || !ACCOUNT_ID_PATTERN.matcher(accountId).matches()) {
            errors.add("Account ID must start with 'ACC-' followed by at least 6 digits");
        }

        // Validate type
        if (type == null) {
            errors.add("Transaction type cannot be null");
        } else {
            try {
                transactionType = TransactionType.fromString(type);
            } catch (IllegalArgumentException e) {
                errors.add("Invalid transaction type. Valid types are: " + Arrays.toString(TransactionType.values()));
            }
        }

        // Validate category
        if (category == null) {
            errors.add("Transaction category cannot be null");
        } else {
            try {
                TransactionCategory transactionCategory = TransactionCategory.fromString(category);
            } catch (IllegalArgumentException e) {
                errors.add("Invalid transaction category. Valid categories are: " + Arrays.toString(TransactionCategory.values()));
            }
        }

        // Validate amount
        if (amount != null) {
            if (amount.scale() > 2) {
                errors.add("Amount cannot have more than 2 decimal places");
            }

            if (amount.abs().compareTo(MIN_AMOUNT) < 0) {
                errors.add("Amount absolute value cannot be less than 0.01");
            }

            // Validate amount sign based on transaction type
            if (transactionType != null) {
                if (transactionType == TransactionType.CREDIT && amount.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Amount must be positive for CREDIT transactions");
                } else if (transactionType == TransactionType.DEBIT && amount.compareTo(BigDecimal.ZERO) >= 0) {
                    errors.add("Amount must be negative for DEBIT transactions");
                }
            }
        } else {
            errors.add("Amount cannot be null");
        }

        // Validate description
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            errors.add("Description cannot exceed 100 characters");
        }

        return errors;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public void setCategory(TransactionCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
} 