package com.hsbc.banking.transaction.dto;

import com.hsbc.banking.transaction.model.ErrorCode;

import java.time.Instant;
import java.util.Map;

public record ErrorDetail(ErrorCode code, String path, Instant timestamp, Map<String, Object> data) {
}
