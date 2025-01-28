package com.hsbc.banking.transaction.model;

public enum ErrorCode {
    TRANSACTION_CONFLICT(409);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
