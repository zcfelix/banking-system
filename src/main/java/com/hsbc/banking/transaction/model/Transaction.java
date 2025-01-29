package com.hsbc.banking.transaction.model;

import com.hsbc.banking.transaction.exception.InvalidTransactionException;

import java.math.BigDecimal;
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
    private String category;
    private String description;

    private Transaction(String orderId, String accountId, BigDecimal amount, TransactionType type, String category, String description) {
        this.orderId = orderId;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
    }

    public static Transaction create(String orderId, String accountId, BigDecimal amount, String type, String category, String description) {
        List<String> errors = validate(orderId, accountId, amount, type, category, description);
        if (!errors.isEmpty()) {
            throw new InvalidTransactionException(Map.of("errors", errors));
        }
        return new Transaction(orderId, accountId, amount, TransactionType.fromString(type), category, description);
    }

    private static List<String> validate(String orderId, String accountId, BigDecimal amount, String type, String category, String description) {
        List<String> errors = new ArrayList<>();

        // Validate orderId
        if (orderId == null || !ORDER_ID_PATTERN.matcher(orderId).matches()) {
            errors.add("Order ID must start with 'ORD-' followed by at least 6 digits");
        }

        // Validate accountId
        if (accountId == null || !ACCOUNT_ID_PATTERN.matcher(accountId).matches()) {
            errors.add("Account ID must start with 'ACC-' followed by at least 6 digits");
        }

        // Validate type
        if (type != null) {
            if (Arrays.stream(TransactionType.values()).noneMatch(t -> t.name().equalsIgnoreCase(type))) {
                errors.add("Invalid transaction type. Valid values are: " + Arrays.toString(TransactionType.values()));
            }
        } else {
            errors.add("Transaction type cannot be null");
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
            if (type != null) {
                TransactionType convertedType = TransactionType.fromString(type);
                if (convertedType == TransactionType.CREDIT && amount.compareTo(BigDecimal.ZERO) >= 0) {
                    errors.add("Amount must be negative for CREDIT transactions");
                } else if (convertedType == TransactionType.DEBIT && amount.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Amount must be positive for DEBIT transactions");
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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