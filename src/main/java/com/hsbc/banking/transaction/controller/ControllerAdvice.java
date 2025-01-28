package com.hsbc.banking.transaction.controller;

import com.hsbc.banking.transaction.dto.ErrorDetail;
import com.hsbc.banking.transaction.model.AppException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ControllerAdvice {
    private static final Logger logger = LoggerFactory.getLogger(ControllerAdvice.class);

    private final HttpServletRequest request;

    public ControllerAdvice(HttpServletRequest request) {
        this.request = request;
    }

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ErrorDetail> handleAppException(AppException e) {
        logger.info("Exception occurred: errorCode={}, data={}", e.getErrorCode(), e.getData());
        return ResponseEntity
                .status(e.getErrorCode().getCode())
                .body(new ErrorDetail(
                        e.getErrorCode(),
                        request.getRequestURI(),
                        Instant.now(),
                        e.getData())
                );
    }
}

