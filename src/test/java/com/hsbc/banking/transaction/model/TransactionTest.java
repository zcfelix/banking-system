package com.hsbc.banking.transaction.model;

import com.hsbc.banking.transaction.exception.InvalidTransactionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    private static final String VALID_ORDER_ID = "ORD-123456";
    private static final String VALID_ACCOUNT_ID = "ACC-123456";
    private static final BigDecimal VALID_CREDIT_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal VALID_DEBIT_AMOUNT = new BigDecimal("-100.00");
    private static final String VALID_CATEGORY = TransactionCategory.SALARY.name();
    private static final String VALID_DESCRIPTION = "Monthly salary";

    @Test
    void should_create_valid_credit_transaction() {
        // When
        Transaction transaction = Transaction.create(
            VALID_ORDER_ID,
            VALID_ACCOUNT_ID,
            VALID_CREDIT_AMOUNT,
            TransactionType.CREDIT.name(),
            VALID_CATEGORY,
            VALID_DESCRIPTION
        );

        // Then
        assertThat(transaction).isNotNull();
        assertThat(transaction.getOrderId()).isEqualTo(VALID_ORDER_ID);
        assertThat(transaction.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(transaction.getAmount()).isEqualTo(VALID_CREDIT_AMOUNT);
        assertThat(transaction.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(transaction.getCategory()).isEqualTo(TransactionCategory.SALARY);
        assertThat(transaction.getDescription()).isEqualTo(VALID_DESCRIPTION);
    }

    @Test
    void should_create_valid_debit_transaction() {
        // When
        Transaction transaction = Transaction.create(
            VALID_ORDER_ID,
            VALID_ACCOUNT_ID,
            VALID_DEBIT_AMOUNT,
            TransactionType.DEBIT.name(),
            TransactionCategory.SHOPPING.name(),
            VALID_DESCRIPTION
        );

        // Then
        assertThat(transaction).isNotNull();
        assertThat(transaction.getOrderId()).isEqualTo(VALID_ORDER_ID);
        assertThat(transaction.getAccountId()).isEqualTo(VALID_ACCOUNT_ID);
        assertThat(transaction.getAmount()).isEqualTo(VALID_DEBIT_AMOUNT);
        assertThat(transaction.getType()).isEqualTo(TransactionType.DEBIT);
        assertThat(transaction.getCategory()).isEqualTo(TransactionCategory.SHOPPING);
        assertThat(transaction.getDescription()).isEqualTo(VALID_DESCRIPTION);
    }

    @Test
    void should_create_transaction_with_null_description() {
        // When
        Transaction transaction = Transaction.create(
            VALID_ORDER_ID,
            VALID_ACCOUNT_ID,
            VALID_CREDIT_AMOUNT,
            TransactionType.CREDIT.name(),
            VALID_CATEGORY,
            null
        );

        // Then
        assertThat(transaction).isNotNull();
        assertThat(transaction.getDescription()).isNull();
    }

    private static Stream<Arguments> invalidTransactionScenarios() {
        return Stream.of(
            // Invalid Order ID format
            Arguments.of(
                "ORD123456", VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, TransactionType.CREDIT.name(), VALID_CATEGORY, VALID_DESCRIPTION,
                "Order ID must start with 'ORD-' followed by at least 6 digits"
            ),
            Arguments.of(
                "ORD-12345", VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, TransactionType.CREDIT.name(), VALID_CATEGORY, VALID_DESCRIPTION,
                "Order ID must start with 'ORD-' followed by at least 6 digits"
            ),
            Arguments.of(
                null, VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, TransactionType.CREDIT.name(), VALID_CATEGORY, VALID_DESCRIPTION,
                "Order ID must start with 'ORD-' followed by at least 6 digits"
            ),
            
            // Invalid Account ID format
            Arguments.of(
                VALID_ORDER_ID, "ACC12345", VALID_CREDIT_AMOUNT, TransactionType.CREDIT.name(), VALID_CATEGORY, VALID_DESCRIPTION,
                "Account ID must start with 'ACC-' followed by at least 6 digits"
            ),
            Arguments.of(
                VALID_ORDER_ID, "ACC-12345", VALID_CREDIT_AMOUNT, TransactionType.CREDIT.name(), VALID_CATEGORY, VALID_DESCRIPTION,
                "Account ID must start with 'ACC-' followed by at least 6 digits"
            ),
            Arguments.of(
                VALID_ORDER_ID, null, VALID_CREDIT_AMOUNT, TransactionType.CREDIT.name(), VALID_CATEGORY, VALID_DESCRIPTION,
                "Account ID must start with 'ACC-' followed by at least 6 digits"
            ),
            
            // Invalid Amount scenarios
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, new BigDecimal("-100.00"), TransactionType.CREDIT.name(), VALID_CATEGORY, VALID_DESCRIPTION,
                "Amount must be positive for CREDIT transactions"
            ),
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, new BigDecimal("100.00"), TransactionType.DEBIT.name(), VALID_CATEGORY, VALID_DESCRIPTION,
                "Amount must be negative for DEBIT transactions"
            ),
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, new BigDecimal("0.001"), TransactionType.DEBIT.name(), VALID_CATEGORY, VALID_DESCRIPTION,
                "Amount cannot have more than 2 decimal places"
            ),
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, null, TransactionType.CREDIT.name(), VALID_CATEGORY, VALID_DESCRIPTION,
                "Amount cannot be null"
            ),
            
            // Invalid Transaction Type
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, "INVALID_TYPE", VALID_CATEGORY, VALID_DESCRIPTION,
                    "Invalid transaction type. Valid types are: " + Arrays.toString(TransactionType.values())
            ),
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, null, VALID_CATEGORY, VALID_DESCRIPTION,
                "Transaction type cannot be null"
            ),

            // Invalid Category
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, TransactionType.CREDIT.name(), "INVALID_CATEGORY", VALID_DESCRIPTION,
                "Invalid transaction category. Valid categories are: " + Arrays.toString(TransactionCategory.values())
            ),
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, TransactionType.CREDIT.name(), null, VALID_DESCRIPTION,
                "Transaction category cannot be null"
            ),
            
            // Invalid Description Length
            Arguments.of(
                VALID_ORDER_ID, VALID_ACCOUNT_ID, VALID_CREDIT_AMOUNT, TransactionType.CREDIT.name(), VALID_CATEGORY, "a".repeat(101),
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
            Transaction.create(orderId, accountId, amount, type, category, description))
            .isInstanceOf(InvalidTransactionException.class)
            .satisfies(thrown -> {
                InvalidTransactionException ex = (InvalidTransactionException) thrown;
                assertThat(((List<String>)ex.getData().get("errors"))).contains(expectedError);
            });
    }
} 