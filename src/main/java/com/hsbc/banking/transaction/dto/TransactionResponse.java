package com.hsbc.banking.transaction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionType;
import com.hsbc.banking.transaction.model.TransactionCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
    Long id,
    String orderId,
    String accountId,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "#.00")
    BigDecimal amount,
    TransactionType type,
    TransactionCategory category,
    String description,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getOrderId(),
            transaction.getAccountId(),
            transaction.getAmount(),
            transaction.getType(),
            transaction.getCategory(),
            transaction.getDescription(),
            transaction.getCreatedAt(),
            transaction.getUpdatedAt()
        );
    }
}