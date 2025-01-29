package com.hsbc.banking.transaction.service;

import com.hsbc.banking.transaction.exception.DuplicateTransactionException;
import com.hsbc.banking.transaction.exception.InvalidTransactionException;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionType;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
    private static final String VALID_ORDER_ID = "ORD-123456";
    private static final String VALID_ACCOUNT_ID = "ACC-123456";
    private static final BigDecimal VALID_CREDIT_AMOUNT = new BigDecimal("-100.00");
    private static final BigDecimal VALID_DEBIT_AMOUNT = new BigDecimal("100.00");
    private static final String VALID_CATEGORY = "Salary";
    private static final String VALID_DESCRIPTION = "Monthly salary";

    @BeforeEach
    void setUp() {
        mockTransaction = new Transaction(VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, 
            TransactionType.CREDIT, VALID_CATEGORY, VALID_DESCRIPTION);
        mockTransaction.setId(1L);
    }

    private static Stream<Arguments> invalidTransactionScenarios() {
        return Stream.of(
            // Invalid Order ID format
            Arguments.of(
                "ORD123456", VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, "CREDIT", VALID_CATEGORY, VALID_DESCRIPTION,
                "Order ID must start with 'ORD-' followed by at least 6 digits"
            ),
            Arguments.of(
                "ORD-12345", VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, "CREDIT", VALID_CATEGORY, VALID_DESCRIPTION,
                "Order ID must start with 'ORD-' followed by at least 6 digits"
            ),
            
            // Invalid Account ID format
            Arguments.of(
                VALID_ORDER_ID, "ACC12345", VALID_CREDIT_AMOUNT, "CREDIT", VALID_CATEGORY, VALID_DESCRIPTION,
                "Account ID must start with 'ACC-' followed by at least 6 digits"
            ),
            Arguments.of(
                VALID_ORDER_ID, "ACC-12345", VALID_CREDIT_AMOUNT, "CREDIT", VALID_CATEGORY, VALID_DESCRIPTION,
                "Account ID must start with 'ACC-' followed by at least 6 digits"
            ),
            
            // Invalid Amount scenarios
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, new BigDecimal("100.00"), "CREDIT", VALID_CATEGORY, VALID_DESCRIPTION,
                "Amount must be negative for CREDIT transactions"
            ),
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, new BigDecimal("-100.00"), "DEBIT", VALID_CATEGORY, VALID_DESCRIPTION,
                "Amount must be positive for DEBIT transactions"
            ),
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, new BigDecimal("0.001"), "DEBIT", VALID_CATEGORY, VALID_DESCRIPTION,
                "Amount cannot have more than 2 decimal places"
            ),
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, new BigDecimal("0.009"), "DEBIT", VALID_CATEGORY, VALID_DESCRIPTION,
                "Amount absolute value cannot be less than 0.01"
            ),
            
            // Invalid Transaction Type
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, "INVALID_TYPE", VALID_CATEGORY, VALID_DESCRIPTION,
                "Invalid transaction type"
            ),
            
            // Invalid Description Length
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, "CREDIT", VALID_CATEGORY, "a".repeat(101),
                "Description cannot exceed 100 characters"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("invalidTransactionScenarios")
    void should_throw_validation_exception_for_invalid_input(
            String orderId, String accountId, BigDecimal amount, String type,
            String category, String description, String expectedError) {
        
        assertThatThrownBy(() -> 
            transactionService.createTransaction(orderId, accountId, amount, type, category, description))
            .isInstanceOf(InvalidTransactionException.class)
            .satisfies(thrown -> {
                InvalidTransactionException ex = (InvalidTransactionException) thrown;
                assertThat(((List<String>)ex.getData().get("errors"))).contains(expectedError);
            });
    }

    @Test
    void should_create_valid_credit_transaction_successfully() {
        // Given
        when(transactionRepository.save(any(Transaction.class))).thenReturn(mockTransaction);

        // When
        Transaction result = transactionService.createTransaction(
            VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, "CREDIT", VALID_CATEGORY, VALID_DESCRIPTION
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderId()).isEqualTo(VALID_ORDER_ID);
        assertThat(result.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(result.getAmount()).isEqualTo(VALID_CREDIT_AMOUNT);
        assertThat(result.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(result.getCategory()).isEqualTo(VALID_CATEGORY);
        assertThat(result.getDescription()).isEqualTo(VALID_DESCRIPTION);
    }

    @Test
    void should_create_valid_debit_transaction_successfully() {
        // Given
        Transaction debitTransaction = new Transaction(VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_DEBIT_AMOUNT,
            TransactionType.DEBIT, VALID_CATEGORY, VALID_DESCRIPTION);
        debitTransaction.setId(1L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(debitTransaction);

        // When
        Transaction result = transactionService.createTransaction(
            VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_DEBIT_AMOUNT, "DEBIT", VALID_CATEGORY, VALID_DESCRIPTION
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderId()).isEqualTo(VALID_ORDER_ID);
        assertThat(result.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(result.getAmount()).isEqualTo(VALID_DEBIT_AMOUNT);
        assertThat(result.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(result.getCategory()).isEqualTo(VALID_CATEGORY);
        assertThat(result.getDescription()).isEqualTo(VALID_DESCRIPTION);
    }

    @Test
    void should_allow_null_description() {
        // Given
        Transaction transactionWithNullDescription = new Transaction(VALID_ORDER_ID, VALID_ACCOUNT_ID,
            VALID_CREDIT_AMOUNT, TransactionType.CREDIT, VALID_CATEGORY, null);
        transactionWithNullDescription.setId(1L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transactionWithNullDescription);

        // When
        Transaction result = transactionService.createTransaction(
            VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, "CREDIT", VALID_CATEGORY, null
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isNull();
    }

    @Test
    void should_throw_exception_when_transaction_is_duplicate() {
        // Given
        Map<String, Object> errorData = Map.of(
            "orderId", VALID_ORDER_ID,
            "message", "Transaction with order ID already exists"
        );
        when(transactionRepository.save(any(Transaction.class)))
            .thenThrow(new DuplicateTransactionException(errorData));

        // When/Then
        assertThatThrownBy(() -> 
            transactionService.createTransaction(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, "CREDIT", VALID_CATEGORY, VALID_DESCRIPTION
            ))
            .isInstanceOf(DuplicateTransactionException.class)
            .satisfies(thrown -> {
                DuplicateTransactionException ex = (DuplicateTransactionException) thrown;
                assertThat(ex.getData())
                    .containsEntry("orderId", VALID_ORDER_ID)
                    .containsEntry("message", "Transaction with order ID already exists");
            });
    }
} 