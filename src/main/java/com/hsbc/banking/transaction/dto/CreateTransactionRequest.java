package com.hsbc.banking.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        @NotBlank(message = "Order ID must not be blank")
        String orderId,

        @NotBlank(message = "Account ID must not be blank")
        String accountId,

        @NotNull(message = "Amount must not be null")
        BigDecimal amount,

        @NotBlank(message = "Transaction type must not be blank")
        String type,

        @NotBlank(message = "Category must not be blank")
        String category,

        String description
) {
}