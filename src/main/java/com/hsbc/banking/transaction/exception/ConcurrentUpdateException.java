package com.hsbc.banking.transaction.exception;

import java.util.Map;

import static com.hsbc.banking.transaction.model.ErrorCode.CONCURRENT_UPDATE_CONFLICT;

public class ConcurrentUpdateException extends AppException {
    public ConcurrentUpdateException(Map<String, Object> data) {
        super(CONCURRENT_UPDATE_CONFLICT, data);
    }
} 