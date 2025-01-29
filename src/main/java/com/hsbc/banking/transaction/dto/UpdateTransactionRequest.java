package com.hsbc.banking.transaction.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTransactionRequest(
        @NotNull(message = "Category must not be null")
        String category,

        @Size(max = 100, message = "Description cannot exceed 100 characters")
        String description
) {} 