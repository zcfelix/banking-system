package com.hsbc.banking.transaction.exception;

import com.hsbc.banking.transaction.model.ErrorCode;
import java.util.Map;

public class InsufficientBalanceException extends AppException {
    public InsufficientBalanceException(Map<String, Object> data) {
        super(ErrorCode.INSUFFICIENT_BALANCE, data);
    }

    public InsufficientBalanceException(String accountId, String message) {
        super(ErrorCode.INSUFFICIENT_BALANCE, Map.of(
            "accountId", accountId,
            "message", message
        ));
    }
}