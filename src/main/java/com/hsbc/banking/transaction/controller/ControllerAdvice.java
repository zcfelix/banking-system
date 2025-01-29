package com.hsbc.banking.transaction.controller;

import com.hsbc.banking.transaction.dto.ErrorDetail;
import com.hsbc.banking.transaction.model.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ControllerAdvice {
    private static final Logger logger = LoggerFactory.getLogger(ControllerAdvice.class);

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ErrorDetail> handleAppException(AppException e, WebRequest request) {
        logger.info("Exception occurred: errorCode={}, data={}", e.getErrorCode(), e.getData());
        return ResponseEntity
                .status(e.getErrorCode().getCode())
                .body(new ErrorDetail(
                        e.getErrorCode(),
                        request.getDescription(false).replace("uri=", ""),
                        LocalDateTime.now(),
                        e.getData())
                );
    }
}

