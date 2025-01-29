package com.hsbc.banking.transaction.controller;

import com.hsbc.banking.transaction.dto.CreateTransactionRequest;
import com.hsbc.banking.transaction.dto.TransactionResponse;
import com.hsbc.banking.transaction.dto.UpdateTransactionRequest;
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
        return new ResponseEntity<>(
                TransactionResponse.from(transactionService.createTransaction(request)),
                HttpStatus.CREATED
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request) {
        return ResponseEntity.ok(
                TransactionResponse.from(transactionService.updateTransaction(id, request))
        );
    }
} 