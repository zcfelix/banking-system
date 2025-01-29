package com.hsbc.banking.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsbc.banking.transaction.dto.CreateTransactionRequest;
import com.hsbc.banking.transaction.dto.UpdateTransactionRequest;
import com.hsbc.banking.transaction.exception.ConcurrentUpdateException;
import com.hsbc.banking.transaction.exception.DuplicateTransactionException;
import com.hsbc.banking.transaction.exception.InsufficientBalanceException;
import com.hsbc.banking.transaction.exception.TransactionNotFoundException;
import com.hsbc.banking.transaction.model.*;
import com.hsbc.banking.transaction.repository.AuditLogRepository;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ExternalAccountService externalAccountService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TransactionService transactionService;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    private Transaction mockTransaction;
    private static final String ORDER_ID = "ORD-012345";
    private static final String ACCOUNT_ID = "ACC-012345";
    private static final BigDecimal AMOUNT = new BigDecimal("-100.00");
    private static final String TYPE = TransactionType.DEBIT.name();
    private static final String CATEGORY = TransactionCategory.SALARY.name();
    private static final String DESCRIPTION = "Monthly salary";

    @BeforeEach
    void setUp() {
        mockTransaction = Transaction.create(ORDER_ID, ACCOUNT_ID, AMOUNT, TYPE, CATEGORY, DESCRIPTION);
        mockTransaction.setId(1L);
    }

    @Nested
    class CreateTransaction {
        @Test
        void should_create_transaction_successfully_when_balance_is_sufficient() {
            // Given
            when(externalAccountService.hasSufficientBalance(eq(ACCOUNT_ID), eq(AMOUNT))).thenReturn(true);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTransaction);

            // When
            Transaction result = transactionService.createTransaction(
                    new CreateTransactionRequest(ORDER_ID, ACCOUNT_ID, AMOUNT, TYPE, CATEGORY, DESCRIPTION)
            );
            LocalDateTime afterCreation = LocalDateTime.now();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
            assertThat(result.getAmount()).isEqualTo(AMOUNT);
            assertThat(result.getType()).isEqualTo(TransactionType.DEBIT);
            assertThat(result.getCategory()).isEqualTo(TransactionCategory.SALARY);
            assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
            assertThat(result.getCreatedAt()).isBefore(afterCreation);
            assertThat(result.getUpdatedAt())
                    .isNotNull()
                    .isEqualTo(result.getCreatedAt());
        }

        @Test
        void should_throw_exception_when_balance_is_insufficient() {
            // Given
            when(externalAccountService.hasSufficientBalance(eq(ACCOUNT_ID), eq(AMOUNT))).thenReturn(false);

            // When/Then
            assertThatThrownBy(() ->
                    transactionService.createTransaction(
                            new CreateTransactionRequest(ORDER_ID, ACCOUNT_ID, AMOUNT, TYPE, CATEGORY, DESCRIPTION)
                    ))
                    .isInstanceOf(InsufficientBalanceException.class)
                    .satisfies(thrown -> {
                        InsufficientBalanceException ex = (InsufficientBalanceException) thrown;
                        assertThat(ex.getData())
                                .containsEntry("accountId", ACCOUNT_ID)
                                .containsEntry("message", "Insufficient balance for transaction amount: " + AMOUNT);
                    });
        }

        @Test
        void should_throw_exception_when_transaction_is_duplicate() {
            // Given
            when(externalAccountService.hasSufficientBalance(eq(ACCOUNT_ID), eq(AMOUNT))).thenReturn(true);
            when(transactionRepository.save(any(Transaction.class)))
                    .thenThrow(new DuplicateTransactionException(Map.of(
                            "orderId", ORDER_ID,
                            "message", "Transaction with order ID already exists"
                    )));

            // When/Then
            assertThatThrownBy(() ->
                    transactionService.createTransaction(
                           new CreateTransactionRequest(ORDER_ID, ACCOUNT_ID, AMOUNT, TYPE, CATEGORY, DESCRIPTION)
                    ))
                    .isInstanceOf(DuplicateTransactionException.class)
                    .satisfies(thrown -> {
                        DuplicateTransactionException ex = (DuplicateTransactionException) thrown;
                        assertThat(ex.getData())
                                .containsEntry("orderId", ORDER_ID)
                                .containsEntry("message", "Transaction with order ID already exists");
                    });
        }
    }

    @Nested
    class DeleteTransaction {
        @Test
        void should_delete_transaction_successfully() throws Exception {
            // Given
            Long transactionId = 1L;
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTransaction));
            when(objectMapper.writeValueAsString(mockTransaction)).thenReturn("transaction json");

            // When
            transactionService.deleteTransaction(transactionId);

            // Then
            verify(transactionRepository).deleteById(transactionId);
            verify(auditLogRepository).save(auditLogCaptor.capture());
            
            AuditLog capturedLog = auditLogCaptor.getValue();
            assertThat(capturedLog.getOperation()).isEqualTo("DELETE");
            assertThat(capturedLog.getEntityType()).isEqualTo("Transaction");
            assertThat(capturedLog.getEntityId()).isEqualTo(String.valueOf(transactionId));
            assertThat(capturedLog.getDetails()).contains("transaction json");
        }

        @Test
        void should_throw_exception_when_deleting_non_existent_transaction() {
            // Given
            Long transactionId = 999L;
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> transactionService.deleteTransaction(transactionId))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .satisfies(thrown -> {
                        TransactionNotFoundException ex = (TransactionNotFoundException) thrown;
                        assertThat(ex.getData())
                                .containsEntry("transactionId", transactionId)
                                .containsEntry("message", "Transaction not found with ID: " + transactionId);
                    });

            verify(transactionRepository, never()).deleteById(any());
            verify(auditLogRepository, never()).save(any());
        }

        @Test
        void should_create_audit_log_even_when_serialization_fails() throws Exception {
            // Given
            Long transactionId = 1L;
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTransaction));
            when(objectMapper.writeValueAsString(mockTransaction)).thenThrow(new RuntimeException("Serialization failed"));

            // When
            transactionService.deleteTransaction(transactionId);

            // Then
            verify(transactionRepository).deleteById(transactionId);
            verify(auditLogRepository).save(auditLogCaptor.capture());
            
            AuditLog capturedLog = auditLogCaptor.getValue();
            assertThat(capturedLog.getOperation()).isEqualTo("DELETE");
            assertThat(capturedLog.getEntityType()).isEqualTo("Transaction");
            assertThat(capturedLog.getEntityId()).isEqualTo(String.valueOf(transactionId));
            assertThat(capturedLog.getDetails()).contains("Failed to serialize transaction: Serialization failed");
        }
    }

    @Nested
    class UpdateTransaction {
        @Test
        void should_update_transaction_successfully() {
            // Given
            Long transactionId = 1L;
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTransaction));
            when(transactionRepository.update(any(Transaction.class))).thenReturn(mockTransaction);

            // When
            Transaction result = transactionService.updateTransaction(transactionId, 
                new UpdateTransactionRequest(TransactionCategory.SHOPPING.name(), "Updated description"));
            LocalDateTime afterUpdate = LocalDateTime.now();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCategory()).isEqualTo(TransactionCategory.SHOPPING);
            assertThat(result.getDescription()).isEqualTo("Updated description");
            assertThat(result.getUpdatedAt())
                    .isNotNull()
                    .isAfter(result.getCreatedAt())
                    .isBefore(afterUpdate);

            verify(transactionRepository).update(any(Transaction.class));
        }

        @Test
        void should_throw_exception_when_updating_non_existent_transaction() {
            // Given
            Long transactionId = 999L;
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> 
                transactionService.updateTransaction(transactionId, 
                    new UpdateTransactionRequest(TransactionCategory.SHOPPING.name(), "Updated description")))
                .isInstanceOf(TransactionNotFoundException.class)
                .satisfies(thrown -> {
                    TransactionNotFoundException ex = (TransactionNotFoundException) thrown;
                    assertThat(ex.getData())
                            .containsEntry("transactionId", transactionId)
                            .containsEntry("message", "Transaction not found with ID: " + transactionId);
                });

            verify(transactionRepository, never()).update(any());
        }

        @Test
        void should_throw_exception_when_concurrent_update_occurs() {
            // Given
            Long transactionId = 1L;
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTransaction));
            when(transactionRepository.update(any(Transaction.class)))
                    .thenThrow(new ConcurrentUpdateException(Map.of(
                            "transactionId", transactionId,
                            "message", "Transaction was updated by another user",
                            "currentVersion", 1L,
                            "requestVersion", 0L
                    )));

            // When/Then
            assertThatThrownBy(() -> 
                transactionService.updateTransaction(transactionId, 
                    new UpdateTransactionRequest(TransactionCategory.SHOPPING.name(), "Updated description")))
                .isInstanceOf(ConcurrentUpdateException.class)
                .satisfies(thrown -> {
                    ConcurrentUpdateException ex = (ConcurrentUpdateException) thrown;
                    assertThat(ex.getData())
                            .containsEntry("transactionId", transactionId)
                            .containsEntry("message", "Transaction was updated by another user")
                            .containsEntry("currentVersion", 1L)
                            .containsEntry("requestVersion", 0L);
                });

            verify(transactionRepository, times(3)).update(any(Transaction.class));
        }

        @Test
        void should_retry_and_succeed_on_concurrent_update() {
            // Given
            when(transactionRepository.findById(mockTransaction.getId())).thenReturn(Optional.of(mockTransaction));

            // Thr first two calls will throw ConcurrentUpdateException and the third call will succeed
            when(transactionRepository.update(any(Transaction.class)))
                    .thenThrow(new ConcurrentUpdateException(Map.of(
                            "transactionId", mockTransaction.getId(),
                            "message", "Transaction was updated by another user"
                    )))
                    .thenThrow(new ConcurrentUpdateException(Map.of(
                            "transactionId", mockTransaction.getId(),
                            "message", "Transaction was updated by another user"
                    )))
                    .thenReturn(mockTransaction);

            // When
            Transaction result = transactionService.updateTransaction(mockTransaction.getId(),
                    new UpdateTransactionRequest(TransactionCategory.SHOPPING.name(), "Updated description"));

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(mockTransaction.getId());
            assertThat(result.getCategory()).isEqualTo(TransactionCategory.SHOPPING);
            assertThat(result.getDescription()).isEqualTo("Updated description");
            verify(transactionRepository, times(3)).update(any(Transaction.class));
        }
    }

    @Nested
    class ListTransactions {
        @Test
        void should_return_paginated_transactions() {
            // Given
            when(transactionRepository.count()).thenReturn(100L);
            when(transactionRepository.findAll(20, 10))
                    .thenReturn(List.of(mockTransaction));

            // When
            Page<Transaction> result = transactionService.listTransactions(3, 10);

            // Then
            assertThat(result.pageNumber()).isEqualTo(3);
            assertThat(result.pageSize()).isEqualTo(10);
            assertThat(result.totalElements()).isEqualTo(100);
            assertThat(result.totalPages()).isEqualTo(10);

            verify(transactionRepository).findAll(20, 10);
            verify(transactionRepository).count();
        }

        @Test
        void should_return_empty_list_when_page_exceeds_total() {
            // Given
            when(transactionRepository.count()).thenReturn(5L);

            // When
            Page<Transaction> result = transactionService.listTransactions(2, 10);

            // Then
            assertThat(result.contents()).isEmpty();
            assertThat(result.pageNumber()).isEqualTo(2);
            assertThat(result.pageSize()).isEqualTo(10);
            assertThat(result.totalElements()).isEqualTo(5);
            assertThat(result.totalPages()).isEqualTo(1);

            verify(transactionRepository, never()).findAll(eq(10), eq(10));
            verify(transactionRepository).count();
        }

        @Test
        void should_return_empty_list_when_page_number_is_invalid() {
            // Given & When
            Page<Transaction> result = transactionService.listTransactions(0, 10);

            // Then
            assertThat(result.contents()).isEmpty();
            assertThat(result.pageNumber()).isEqualTo(0);
            assertThat(result.pageSize()).isEqualTo(10);
            assertThat(result.totalElements()).isEqualTo(0);
            assertThat(result.totalPages()).isEqualTo(0);

            verify(transactionRepository, never()).findAll(anyInt(), anyInt());
            verify(transactionRepository, never()).count();
        }

        @Test
        void should_return_empty_list_when_page_size_is_invalid() {
            // Given & When
            Page<Transaction> result = transactionService.listTransactions(1, 0);

            // Then
            assertThat(result.contents()).isEmpty();
            assertThat(result.pageNumber()).isEqualTo(1);
            assertThat(result.pageSize()).isEqualTo(0);
            assertThat(result.totalElements()).isEqualTo(0);
            assertThat(result.totalPages()).isEqualTo(0);

            verify(transactionRepository, never()).findAll(anyInt(), anyInt());
            verify(transactionRepository, never()).count();
        }

        @Test
        void should_limit_page_size_to_100() {
            // Given
            when(transactionRepository.count()).thenReturn(200L);

            // When
            Page<Transaction> result = transactionService.listTransactions(1, 101);

            // Then
            assertThat(result.pageNumber()).isEqualTo(1);
            assertThat(result.pageSize()).isEqualTo(100);
            assertThat(result.totalElements()).isEqualTo(200);
            assertThat(result.totalPages()).isEqualTo(2);

            verify(transactionRepository).findAll(eq(0), eq(100));
            verify(transactionRepository).count();
        }

    }

    @Nested
    class GetTransaction {
        @Test
        void should_get_transaction_successfully() {
            // Given
            Long transactionId = 1L;
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(mockTransaction));

            // When
            Transaction result = transactionService.getTransaction(transactionId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(transactionId);
            assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
            assertThat(result.getAmount()).isEqualTo(AMOUNT);
            assertThat(result.getType()).isEqualTo(TransactionType.DEBIT);
            assertThat(result.getCategory()).isEqualTo(TransactionCategory.SALARY);
            assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
            assertThat(result.getCreatedAt()).isNotNull();
            assertThat(result.getUpdatedAt()).isNotNull();

            // Verify repository is called
            verify(transactionRepository).findById(transactionId);
        }

        @Test
        void should_throw_exception_when_getting_non_existent_transaction() {
            // Given
            Long transactionId = 999L;
            when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> transactionService.getTransaction(transactionId))
                    .isInstanceOf(TransactionNotFoundException.class)
                    .satisfies(thrown -> {
                        TransactionNotFoundException ex = (TransactionNotFoundException) thrown;
                        assertThat(ex.getData())
                                .containsEntry("transactionId", transactionId)
                                .containsEntry("message", "Transaction not found with ID: " + transactionId);
                    });

            // Verify repository is called
            verify(transactionRepository).findById(transactionId);
        }
    }
}