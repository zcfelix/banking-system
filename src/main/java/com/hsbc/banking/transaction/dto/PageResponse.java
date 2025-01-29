package com.hsbc.banking.transaction.dto;

import java.util.List;

public record PageResponse<T>(List<T> contents, Long totalSize) {
}