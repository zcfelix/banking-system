package com.hsbc.banking.transaction.service;

import com.hsbc.banking.transaction.exception.DuplicateTransactionException;
import com.hsbc.banking.transaction.exception.InsufficientBalanceException;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionType;
import com.hsbc.banking.transaction.model.TransactionCategory;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ExternalAccountService externalAccountService;

    @InjectMocks
    private TransactionService transactionService;

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

    @Test
    void should_create_transaction_successfully_when_balance_is_sufficient() {
        // Given
        when(externalAccountService.hasSufficientBalance(eq(ACCOUNT_ID), eq(AMOUNT))).thenReturn(true);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTransaction);

        // When
        Transaction result = transactionService.createTransaction(
                ORDER_ID, ACCOUNT_ID, AMOUNT, TransactionType.DEBIT.name(), CATEGORY, DESCRIPTION
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getAmount()).isEqualTo(AMOUNT);
        assertThat(result.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(result.getCategory()).isEqualTo(TransactionCategory.SALARY);
        assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
    }

    @Test
    void should_throw_exception_when_balance_is_insufficient() {
        // Given
        when(externalAccountService.hasSufficientBalance(eq(ACCOUNT_ID), eq(AMOUNT))).thenReturn(false);

        // When/Then
        assertThatThrownBy(() ->
                transactionService.createTransaction(
                        ORDER_ID, ACCOUNT_ID, AMOUNT, TYPE, CATEGORY, DESCRIPTION
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