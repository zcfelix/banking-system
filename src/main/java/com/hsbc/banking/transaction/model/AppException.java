package com.hsbc.banking.transaction.model;

import java.util.Map;

public abstract class AppException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> data;

    public AppException(ErrorCode errorCode, Map<String, Object> data) {
        super();
        this.errorCode = errorCode;
        this.data = data;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getData() {
        return data;
    }
}

