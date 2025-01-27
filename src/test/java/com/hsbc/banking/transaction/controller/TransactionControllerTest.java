package com.hsbc.banking.transaction.controller;

import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.dto.TransactionResponse;
import com.hsbc.banking.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(TransactionController.class)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        Transaction mockTransaction = new Transaction(
                new BigDecimal("100.00"),
                "Credit",
                "Salary",
                "Monthly salary"
        );
        mockTransaction.setId(1L);

        when(transactionService.createTransaction(
                any(BigDecimal.class),
                any(String.class),
                any(String.class),
                any(String.class)))
                .thenReturn(mockTransaction);
    }

    @Test
    void shouldCreateTransaction() throws Exception {
        String transactionJson =
                "{\"amount\":100.00,\"type\":\"Credit\",\"category\":\"Salary\",\"description\":\"Monthly salary\"}";

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transactionJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value("100.00"))
                .andExpect(jsonPath("$.type").value("Credit"))
                .andExpect(jsonPath("$.category").value("Salary"))
                .andExpect(jsonPath("$.description").value("Monthly salary"));
    }
} 