package com.hsbc.banking.transaction.controller;

import com.hsbc.banking.transaction.dto.CreateTransactionRequest;
import com.hsbc.banking.transaction.exception.DuplicateTransactionException;
import com.hsbc.banking.transaction.exception.InvalidTransactionException;
import com.hsbc.banking.transaction.exception.TransactionNotFoundException;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionCategory;
import com.hsbc.banking.transaction.model.TransactionType;
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
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    }

    @Test
    void should_create_transaction() throws Exception {
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenReturn(mockTransaction);
        performTransactionCreation("ORD-012345", "CREDIT")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value("ORD-012345"))
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.category").value("SALARY"));
    }

    @Test
    void should_create_transaction_with_lowercase_type() throws Exception {
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenReturn(mockTransaction);
        performTransactionCreation("ORD-012345", "credit")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.category").value("SALARY"));
    }

    @Test
    void should_return_409_when_transaction_is_duplicate() throws Exception {
        when(transactionService.createTransaction(any(CreateTransactionRequest.class))).thenThrow(new DuplicateTransactionException(
                Map.of("orderId", "ORD-012345",
                      "message", "Transaction with ORD-012345 already exists")
        ));
        performTransactionCreation("ORD-012345", "CREDIT")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSACTION_CONFLICT"))
                .andExpect(jsonPath("$.data.orderId").value("ORD-012345"));
    }

    @Test
    void should_return_400_when_order_id_is_blank() throws Exception {
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenReturn(mockTransaction);
        performTransactionCreation("", "CREDIT")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.data.orderId").value("Order ID must not be blank"));
    }

    @Test
    void should_return_400_when_type_is_invalid() throws Exception {
        when(transactionService.createTransaction(any(CreateTransactionRequest.class)))
                .thenThrow(new InvalidTransactionException(
                        Map.of("errors", List.of("Invalid transaction type. Valid types are: " + Arrays.toString(TransactionType.values())))));
        performTransactionCreation("ORD-012345", "INVALID")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSACTION"))
                .andExpect(jsonPath("$.data.errors[0]").value("Invalid transaction type. Valid types are: " + Arrays.toString(TransactionType.values())));
    }

    @Test
    void should_delete_transaction_successfully() throws Exception {
        // Given
        Long transactionId = 1L;
        doNothing().when(transactionService).deleteTransaction(transactionId);

        // When & Then
        mockMvc.perform(delete("/transactions/{id}", transactionId))
                .andExpect(status().isNoContent());

        verify(transactionService).deleteTransaction(transactionId);
    }

    @Test
    void should_return_404_when_deleting_non_existent_transaction() throws Exception {
        // Given
        Long transactionId = 999L;
        doThrow(new TransactionNotFoundException(transactionId))
                .when(transactionService).deleteTransaction(transactionId);

        // When & Then
        mockMvc.perform(delete("/transactions/{id}", transactionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"))
                .andExpect(jsonPath("$.data.transactionId").value(transactionId))
                .andExpect(jsonPath("$.data.message").value("Transaction not found with ID: " + transactionId));

        verify(transactionService).deleteTransaction(transactionId);
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

    private ResultActions performTransactionCreation(String orderId, String type) throws Exception {
        String transactionJson = String.format(BASE_TRANSACTION_JSON, orderId, type);
        return mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionJson));
    }
} 