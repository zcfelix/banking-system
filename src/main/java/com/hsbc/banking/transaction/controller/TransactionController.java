package com.hsbc.banking.transaction.controller;

import com.hsbc.banking.transaction.dto.*;
import com.hsbc.banking.transaction.model.Page;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Transaction Management", description = "APIs for managing transactions")
@RestController
@RequestMapping("/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "Create a new transaction",
            description = "Creates a new transaction with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDetail.class)
            )),
            @ApiResponse(responseCode = "409", description = "Transaction already exists", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDetail.class)
            ))
    })
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Parameter(description = "Transaction details", required = true)
            @Valid @RequestBody CreateTransactionRequest request) {
        return new ResponseEntity<>(
                TransactionResponse.from(transactionService.createTransaction(request)),
                HttpStatus.CREATED
        );
    }

    @Operation(summary = "Delete a transaction",
            description = "Deletes a transaction by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Transaction deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDetail.class)
            ))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update a transaction",
            description = "Updates an existing transaction with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDetail.class)
            )),
            @ApiResponse(responseCode = "404", description = "Transaction not found", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDetail.class)
            )),
            @ApiResponse(responseCode = "409", description = "Concurrent update conflict", content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorDetail.class)
            ))
    })
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated transaction details", required = true)
            @Valid @RequestBody UpdateTransactionRequest request) {
        return ResponseEntity.ok(
                TransactionResponse.from(transactionService.updateTransaction(id, request))
        );
    }

    @Operation(summary = "List all transactions",
            description = "Returns a paginated list of all transactions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list"),
    })
    @GetMapping
    public ResponseEntity<PageResponse<TransactionResponse>> listTransactions(
            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(value = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        Page<Transaction> page = transactionService.listTransactions(pageNumber, pageSize);
        List<TransactionResponse> content = page.contents().stream()
                .map(TransactionResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new PageResponse<>(content, page.totalElements()));
    }
} 