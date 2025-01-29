package com.hsbc.banking.transaction.model;

public enum ErrorCode {
    TRANSACTION_CONFLICT(409),
    INVALID_TRANSACTION(400),
    INVALID_REQUEST(400),
    INSUFFICIENT_BALANCE(400),
    TRANSACTION_NOT_FOUND(404);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
