package com.hsbc.banking.transaction.dto;

import com.hsbc.banking.transaction.model.TransactionType;
import java.math.BigDecimal;

public record CreateTransactionRequest(
    String orderId,
    String accountId,
    BigDecimal amount,
    TransactionType type,
    String category,
    String description
) {} 