package com.hsbc.banking.transaction.exception;

import com.hsbc.banking.transaction.model.ErrorCode;

import java.util.Map;

public class TransactionNotFoundException extends AppException {
    public TransactionNotFoundException(Long transactionId) {
        super(ErrorCode.TRANSACTION_NOT_FOUND, 
              Map.of("transactionId", transactionId,
                     "message", "Transaction not found with ID: " + transactionId));
    }
} 