package com.hsbc.banking.transaction.dto;

import java.math.BigDecimal;

public record CreateTransactionRequest(
    String orderId,
    BigDecimal amount,
    String type,
    String category,
    String description
) {} 