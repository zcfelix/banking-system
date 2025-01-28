package com.hsbc.banking.transaction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.Objects;

public class TransactionResponse {
    private Long id;
    private String orderId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "#.00")
    private BigDecimal amount;
    private String type;
    private String category;
    private String description;

    public TransactionResponse(Long id,
                               String orderId,
                               BigDecimal amount,
                               String type,
                               String category,
                               String description) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
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
        TransactionResponse that = (TransactionResponse) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(orderId, that.orderId) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(type, that.type) &&
               Objects.equals(category, that.category) &&
               Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderId, amount, type, category, description);
    }
} 