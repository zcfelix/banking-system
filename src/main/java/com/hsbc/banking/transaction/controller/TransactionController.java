package com.hsbc.banking.transaction.controller;

import com.hsbc.banking.transaction.dto.CreateTransactionRequest;
import com.hsbc.banking.transaction.dto.TransactionResponse;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        Transaction transaction = transactionService.createTransaction(
                request.orderId(),
                request.accountId(),
                request.amount(),
                request.type(),
                request.category(),
                request.description()
        );
        TransactionResponse response = new TransactionResponse(
                transaction.getId(),
                transaction.getOrderId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getCategory(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
} 