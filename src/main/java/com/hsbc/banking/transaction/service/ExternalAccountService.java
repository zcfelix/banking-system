package com.hsbc.banking.transaction.service;

import java.math.BigDecimal;

public interface ExternalAccountService {
    /**
     * Check if the account has sufficient balance for the transaction
     *
     * @param accountId the account ID to check
     * @param amount the transaction amount (positive for debit, negative for credit)
     * @return true if the account has sufficient balance, false otherwise
     */
    boolean hasSufficientBalance(String accountId, BigDecimal amount);
} 