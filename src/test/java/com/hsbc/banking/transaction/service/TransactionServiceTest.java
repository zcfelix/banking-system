package com.hsbc.banking.transaction.service;

import com.hsbc.banking.transaction.exception.DuplicateTransactionException;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionType;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction mockTransaction;
    private static final String ORDER_ID = "ORD-012345";
    private static final String ACCOUNT_ID = "ACC-012345";
    private static final BigDecimal AMOUNT = new BigDecimal("-100.00");
    private static final String TYPE = TransactionType.CREDIT.name();
    private static final String CATEGORY = "Salary";
    private static final String DESCRIPTION = "Monthly salary";

    @BeforeEach
    void setUp() {
        mockTransaction = Transaction.create(ORDER_ID, ACCOUNT_ID, AMOUNT, TYPE, CATEGORY, DESCRIPTION);
        mockTransaction.setId(1L);
    }

    @Test
    void should_create_transaction_successfully() {
        // Given
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTransaction);

        // When
        Transaction result = transactionService.createTransaction(
                ORDER_ID, ACCOUNT_ID, AMOUNT, TYPE, CATEGORY, DESCRIPTION
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getAmount()).isEqualTo(AMOUNT);
        assertThat(result.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(result.getCategory()).isEqualTo(CATEGORY);
        assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    void should_throw_exception_when_transaction_is_duplicate() {
        // Given
        when(transactionRepository.save(any(Transaction.class)))
                .thenThrow(new DuplicateTransactionException(Map.of(
                        "orderId", ORDER_ID,
                        "message", "Transaction with order ID already exists"
                )));

        // When/Then
        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        ORDER_ID, ACCOUNT_ID, AMOUNT, TYPE, CATEGORY, DESCRIPTION
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