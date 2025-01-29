package com.hsbc.banking.transaction.dto;

import java.math.BigDecimal;

public record CreateTransactionRequest(
    String orderId,
    String accountId,
    BigDecimal amount,
    String type,
    String category,
    String description
) {} 