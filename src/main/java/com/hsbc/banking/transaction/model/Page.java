package com.hsbc.banking.transaction.model;

import java.util.List;

public record Page<T>(
        List<T> contents,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {
    public static <T> Page<T> of(List<T> contents, int pageNumber, int pageSize, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        return new Page<>(contents, pageNumber, pageSize, totalElements, totalPages);
    }
}
