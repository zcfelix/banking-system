package com.hsbc.banking.transaction.controller;

import com.hsbc.banking.transaction.exception.DuplicateTransactionException;
import com.hsbc.banking.transaction.exception.InvalidTransactionException;
import com.hsbc.banking.transaction.model.Transaction;
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

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        Transaction mockTransaction = new Transaction(
                "ORD-001",
                "ACC-001",
                new BigDecimal("100.00"),
                TransactionType.CREDIT,
                "Salary",
                "Monthly salary"
        );
        mockTransaction.setId(1L);

        when(transactionService.createTransaction(
                any(String.class),
                any(String.class),
                any(BigDecimal.class),
                eq("INVALID"),
                any(String.class),
                any(String.class)))
                .thenThrow(new InvalidTransactionException(
                        Map.of("type", "Invalid transaction type. Valid values are: CREDIT, DEBIT, TRANSFER_IN, TRANSFER_OUT, INVESTMENT, INVESTMENT_RETURN, LOAN_DISBURSEMENT, LOAN_REPAYMENT, FEE, INTEREST, CHARGE, REFUND")));

        when(transactionService.createTransaction(
                any(String.class),
                any(String.class),
                any(BigDecimal.class),
                argThat(type -> Arrays.stream(TransactionType.values()).anyMatch(t -> t.name().equalsIgnoreCase(type))),
                any(String.class),
                any(String.class)))
                .thenReturn(mockTransaction);
    }

    @Test
    void should_create_transaction() throws Exception {
        String transactionJson = """
                {
                    "orderId":"ORD-001",
                    "accountId":"ACC-001",
                    "amount":100.00,
                    "type":"CREDIT",
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
                .andExpect(jsonPath("$.accountId").value("ACC-001"))
                .andExpect(jsonPath("$.amount").value("100.00"))
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.category").value("Salary"))
                .andExpect(jsonPath("$.description").value("Monthly salary"));
    }

    @Test
    void should_create_transaction_with_lowercase_type() throws Exception {
        String transactionJson = """
                {
                    "orderId":"ORD-001",
                    "accountId":"ACC-001",
                    "amount":100.00,
                    "type":"credit",
                    "category":"Salary",
                    "description":"Monthly salary"
                }
                """;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CREDIT"));
    }

    @Test
    void should_return_409_when_transaction_is_duplicate() throws Exception {
        when(transactionService.createTransaction(
                any(String.class),
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
                    "accountId":"ACC-001",
                    "amount":100.00,
                    "type":"CREDIT",
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

    @Test
    void should_return_400_when_order_id_is_blank() throws Exception {
        String transactionJson = """
                {
                    "orderId":"",
                    "accountId":"ACC-001",
                    "amount":100.00,
                    "type":"CREDIT",
                    "category":"Salary",
                    "description":"Monthly salary"
                }
                """;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.data.orderId").value("Order ID must not be blank"));
    }

    @Test
    void should_return_400_when_type_is_invalid() throws Exception {
        String transactionJson = """
                {
                    "orderId":"ORD-001",
                    "accountId":"ACC-001",
                    "amount":100.00,
                    "type":"INVALID",
                    "category":"Salary",
                    "description":"Monthly salary"
                }
                """;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSACTION"))
                .andExpect(jsonPath("$.data.type").value("Invalid transaction type. Valid values are: CREDIT, DEBIT, TRANSFER_IN, TRANSFER_OUT, INVESTMENT, INVESTMENT_RETURN, LOAN_DISBURSEMENT, LOAN_REPAYMENT, FEE, INTEREST, CHARGE, REFUND"));
    }

    @Test
    void should_return_400_when_type_is_null() throws Exception {
        String transactionJson = """
                {
                    "orderId":"ORD-001",
                    "accountId":"ACC-001",
                    "amount":100.00,
                    "category":"Salary",
                    "description":"Monthly salary"
                }
                """;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.data.type").value("Transaction type must not be blank"));
    }
} 