package com.hsbc.banking.transaction.controller;

import com.hsbc.banking.transaction.dto.ErrorDetail;
import com.hsbc.banking.transaction.model.AppException;
import com.hsbc.banking.transaction.model.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetail> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        logger.info("Validation exception occurred: errors={}", errors);

        return ResponseEntity
                .status(400)
                .body(new ErrorDetail(
                        ErrorCode.INVALID_REQUEST,
                        request.getDescription(false).replace("uri=", ""),
                        LocalDateTime.now(),
                        errors)
                );
    }

}

