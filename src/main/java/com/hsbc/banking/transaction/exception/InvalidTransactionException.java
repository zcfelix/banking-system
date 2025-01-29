package com.hsbc.banking.transaction.exception;

import com.hsbc.banking.transaction.model.ErrorCode;

import java.util.Map;

public class InvalidTransactionException extends AppException {
    public InvalidTransactionException(Map<String, Object> data) {
        super(ErrorCode.INVALID_TRANSACTION, data);
    }
} 