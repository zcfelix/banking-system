package com.hsbc.banking.transaction.controller;

import com.hsbc.banking.transaction.exception.DuplicateTransactionException;
import com.hsbc.banking.transaction.exception.InvalidTransactionException;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionType;
import com.hsbc.banking.transaction.model.TransactionCategory;
import com.hsbc.banking.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({ControllerAdvice.class})
public class TransactionControllerTest {

    private static final String BASE_TRANSACTION_JSON = """
            {
                "orderId":"%s",
                "accountId":"ACC-012345",
                "amount":100.00,
                "type":"%s",
                "category":"SALARY",
                "description":"Monthly salary"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    private Transaction mockTransaction;

    @BeforeEach
    void setUp() {
        mockTransaction = createMockTransaction();
        setupTransactionServiceMock();
    }

    @Test
    void should_create_transaction() throws Exception {
        performTransactionCreation("ORD-012345", "CREDIT")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value("ORD-012345"))
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.category").value("SALARY"));
    }

    @Test
    void should_create_transaction_with_lowercase_type() throws Exception {
        performTransactionCreation("ORD-012345", "credit")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.category").value("SALARY"));
    }

    @Test
    void should_return_409_when_transaction_is_duplicate() throws Exception {
        setupDuplicateTransactionMock();
        performTransactionCreation("ORD-012345", "CREDIT")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSACTION_CONFLICT"))
                .andExpect(jsonPath("$.data.orderId").value("ORD-012345"));
    }

    @Test
    void should_return_400_when_order_id_is_blank() throws Exception {
        performTransactionCreation("", "CREDIT")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.data.orderId").value("Order ID must not be blank"));
    }

    @Test
    void should_return_400_when_type_is_invalid() throws Exception {
        performTransactionCreation("ORD-012345", "INVALID")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSACTION"))
                .andExpect(jsonPath("$.data.errors[0]").value("Invalid transaction type. Valid types are: " + Arrays.toString(TransactionType.values())));
    }

    private Transaction createMockTransaction() {
        Transaction transaction = Transaction.create(
                "ORD-012345",
                "ACC-012345",
                new BigDecimal("100.00"),
                TransactionType.CREDIT.name(),
                TransactionCategory.SALARY.name(),
                "Monthly salary"
        );
        transaction.setId(1L);
        return transaction;
    }

    private void setupTransactionServiceMock() {
        when(transactionService.createTransaction(
                any(String.class),
                any(String.class),
                any(BigDecimal.class),
                eq("INVALID"),
                any(String.class),
                any(String.class)))
                .thenThrow(new InvalidTransactionException(
                        Map.of("errors", java.util.List.of("Invalid transaction type. Valid types are: " + Arrays.toString(TransactionType.values())))));

        when(transactionService.createTransaction(
                any(String.class),
                any(String.class),
                any(BigDecimal.class),
                argThat(type -> Arrays.stream(TransactionType.values()).anyMatch(t -> t.name().equalsIgnoreCase(type))),
                any(String.class),
                any(String.class)))
                .thenReturn(mockTransaction);
    }

    private void setupDuplicateTransactionMock() {
        when(transactionService.createTransaction(
                any(String.class),
                any(String.class),
                any(BigDecimal.class),
                any(String.class),
                any(String.class),
                any(String.class)))
                .thenThrow(new DuplicateTransactionException(
                        Map.of("orderId", "ORD-012345",
                              "message", "Transaction with ORD-012345 already exists")
                ));
    }

    private ResultActions performTransactionCreation(String orderId, String type) throws Exception {
        String transactionJson = String.format(BASE_TRANSACTION_JSON, orderId, type);
        return mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionJson));
    }
} 