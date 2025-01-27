package com.hsbc.banking.transaction.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class CreateTransactionRequest {
    private BigDecimal amount;
    private String type;
    private String category;
    private String description;

    // Getters and Setters
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
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
        CreateTransactionRequest that = (CreateTransactionRequest) o;
        return Objects.equals(amount, that.amount) &&
               Objects.equals(type, that.type) &&
               Objects.equals(category, that.category) &&
               Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, type, category, description);
    }
} 