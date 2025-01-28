package com.hsbc.banking.transaction.exception;

import com.hsbc.banking.transaction.model.AppException;
import com.hsbc.banking.transaction.model.ErrorCode;

import java.util.Map;

public class DuplicateTransactionException extends AppException {
    public DuplicateTransactionException(Map<String, Object> data) {
        super(ErrorCode.TRANSACTION_CONFLICT, data);
    }
} 