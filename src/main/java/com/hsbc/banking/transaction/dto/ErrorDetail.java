package com.hsbc.banking.transaction.dto;

import com.hsbc.banking.transaction.model.ErrorCode;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorDetail(ErrorCode code, String path, LocalDateTime timestamp, Map<String, Object> data) {
}
