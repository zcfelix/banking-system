package com.hsbc.banking.transaction.controller;

import com.hsbc.banking.transaction.exception.DuplicateTransactionException;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({ControllerAdvice.class})
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        Transaction mockTransaction = new Transaction(
                "ORD-001",
                new BigDecimal("100.00"),
                "Credit",
                "Salary",
                "Monthly salary"
        );
        mockTransaction.setId(1L);

        when(transactionService.createTransaction(
                any(String.class),
                any(BigDecimal.class),
                any(String.class),
                any(String.class),
                any(String.class)))
                .thenReturn(mockTransaction);
    }

    @Test
    void should_create_transaction() throws Exception {
        String transactionJson = """
                {
                    "orderId":"ORD-001",
                    "amount":100.00,
                    "type":"Credit",
                    "category":"Salary",
                    "description":"Monthly salary"
                }
                """;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderId").value("ORD-001"))
                .andExpect(jsonPath("$.amount").value("100.00"))
                .andExpect(jsonPath("$.type").value("Credit"))
                .andExpect(jsonPath("$.category").value("Salary"))
                .andExpect(jsonPath("$.description").value("Monthly salary"));
    }

    @Test
    void should_return_409_when_transaction_is_duplicate() throws Exception {
        when(transactionService.createTransaction(
                any(String.class),
                any(BigDecimal.class),
                any(String.class),
                any(String.class),
                any(String.class)))
                .thenThrow(new DuplicateTransactionException(
                                Map.of("orderId", "ORD-001", "message",
                                        "Transaction with ORD-001 already exists")
                        )
                );

        String transactionJson = """
                {
                    "orderId":"ORD-001",
                    "amount":100.00,
                    "type":"Credit",
                    "category":"Salary",
                    "description":"Monthly salary"
                }
                """;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSACTION_CONFLICT"))
                .andExpect(jsonPath("$.path").value("/transactions"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.orderId").value("ORD-001"))
                .andExpect(jsonPath("$.data.message").value("Transaction with ORD-001 already exists"));
    }
} 