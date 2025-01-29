package com.hsbc.banking.transaction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

public record TransactionResponse(
    Long id,
    String orderId,
    String accountId,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "#.00")
    BigDecimal amount,
    String type,
    String category,
    String description
) {} 