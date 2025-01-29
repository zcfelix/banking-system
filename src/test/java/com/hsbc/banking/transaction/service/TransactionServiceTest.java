package com.hsbc.banking.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsbc.banking.transaction.dto.CreateTransactionRequest;
import com.hsbc.banking.transaction.exception.DuplicateTransactionException;
import com.hsbc.banking.transaction.exception.InsufficientBalanceException;
import com.hsbc.banking.transaction.exception.TransactionNotFoundException;
import com.hsbc.banking.transaction.model.AuditLog;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionType;
import com.hsbc.banking.transaction.model.TransactionCategory;
import com.hsbc.banking.transaction.repository.AuditLogRepository;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private Transaction mockTransaction;
    private static final String ORDER_ID = "ORD-012345";
    private static final String ACCOUNT_ID = "ACC-012345";
    private static final BigDecimal AMOUNT = new BigDecimal("-100.00");
    private static final String TYPE = TransactionType.DEBIT.name();
    private static final String CATEGORY = TransactionCategory.SALARY.name();
    private static final String DESCRIPTION = "Monthly salary";

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    @BeforeEach
    void setUp() {
        mockTransaction = Transaction.create(ORDER_ID, ACCOUNT_ID, AMOUNT, TYPE, CATEGORY, DESCRIPTION);
        mockTransaction.setId(1L);
    }

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
        assertThat(capturedLog.getDetails()).contains("Serialization failed");
    }
}