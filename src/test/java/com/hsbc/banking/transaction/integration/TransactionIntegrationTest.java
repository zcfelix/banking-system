package com.hsbc.banking.transaction.integration;

import com.hsbc.banking.transaction.model.AuditLog;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionCategory;
import com.hsbc.banking.transaction.model.TransactionType;
import com.hsbc.banking.transaction.repository.AuditLogRepository;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import com.hsbc.banking.transaction.service.ExternalAccountService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @MockBean
    private ExternalAccountService externalAccountService;

    private static final String CREATE_CREDIT_TRANSACTION_REQUEST = """
            {
                "orderId": "ORD-123456",
                "accountId": "ACC-123456",
                "amount": 100.00,
                "type": "CREDIT",
                "category": "SALARY",
                "description": "Monthly salary payment"
            }
            """;

    private static final String CREATE_DEBIT_TRANSACTION_REQUEST = """
            {
                "orderId": "ORD-123456",
                "accountId": "ACC-123456",
                "amount": -100.00,
                "type": "DEBIT",
                "category": "SHOPPING",
                "description": "Shopping payment"
            }
            """;

    @BeforeEach
    void setUp() {
        transactionRepository.clear();
    }

    @Nested
    class CreateTransaction {
        @Test
        void should_create_credit_transaction_successfully() throws Exception {
            // When
            LocalDateTime beforeCreation = LocalDateTime.now();
            mockMvc.perform(post("/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_CREDIT_TRANSACTION_REQUEST))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderId").value("ORD-123456"))
                    .andExpect(jsonPath("$.accountId").value("ACC-123456"))
                    .andExpect(jsonPath("$.amount").value("100.00"))
                    .andExpect(jsonPath("$.type").value("CREDIT"))
                    .andExpect(jsonPath("$.category").value("SALARY"))
                    .andExpect(jsonPath("$.description").value("Monthly salary payment"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(result -> {
                        String createdAt = JsonPath.read(result.getResponse().getContentAsString(), "$.createdAt");
                        String updatedAt = JsonPath.read(result.getResponse().getContentAsString(), "$.updatedAt");
                        assertThat(updatedAt).isEqualTo(createdAt);
                    });
            LocalDateTime afterCreation = LocalDateTime.now();

            // Then
            Transaction savedTransaction = transactionRepository.findByOrderId("ORD-123456")
                    .orElseThrow(() -> new AssertionError("Transaction not found"));
            assertThat(savedTransaction.getCreatedAt()).isAfter(beforeCreation).isBefore(afterCreation);
            assertThat(savedTransaction.getUpdatedAt())
                    .isNotNull()
                    .isEqualTo(savedTransaction.getCreatedAt());
        }

        @Test
        void should_create_debit_transaction_when_balance_is_sufficient() throws Exception {
            // Given
            when(externalAccountService.hasSufficientBalance(eq("ACC-123456"), any(BigDecimal.class)))
                    .thenReturn(true);

            // When
            mockMvc.perform(post("/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_DEBIT_TRANSACTION_REQUEST))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.orderId").value("ORD-123456"))
                    .andExpect(jsonPath("$.accountId").value("ACC-123456"))
                    .andExpect(jsonPath("$.amount").value("-100.00"))
                    .andExpect(jsonPath("$.type").value("DEBIT"))
                    .andExpect(jsonPath("$.category").value("SHOPPING"))
                    .andExpect(jsonPath("$.description").value("Shopping payment"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(result -> {
                        String createdAt = JsonPath.read(result.getResponse().getContentAsString(), "$.createdAt");
                        String updatedAt = JsonPath.read(result.getResponse().getContentAsString(), "$.updatedAt");
                        assertThat(updatedAt).isEqualTo(createdAt);
                    });

            // Then
            verify(externalAccountService).hasSufficientBalance(eq("ACC-123456"), eq(new BigDecimal("-100.00")));

            Transaction savedTransaction = transactionRepository.findByOrderId("ORD-123456")
                    .orElseThrow(() -> new AssertionError("Transaction not found"));

            assertThat(savedTransaction.getOrderId()).isEqualTo("ORD-123456");
            assertThat(savedTransaction.getAccountId()).isEqualTo("ACC-123456");
            assertThat(savedTransaction.getAmount()).isEqualByComparingTo(new BigDecimal("-100.00"));
            assertThat(savedTransaction.getType()).isEqualTo(TransactionType.DEBIT);
            assertThat(savedTransaction.getCategory()).isEqualTo(TransactionCategory.SHOPPING);
            assertThat(savedTransaction.getDescription()).isEqualTo("Shopping payment");
            assertThat(savedTransaction.getUpdatedAt())
                    .isNotNull()
                    .isEqualTo(savedTransaction.getCreatedAt());
        }

        @Test
        void should_reject_transaction_when_insufficient_balance() throws Exception {
            // Given
            when(externalAccountService.hasSufficientBalance(eq("ACC-123456"), any(BigDecimal.class)))
                    .thenReturn(false);

            // When/Then
            mockMvc.perform(post("/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_DEBIT_TRANSACTION_REQUEST))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"))
                    .andExpect(jsonPath("$.data.accountId").value("ACC-123456"))
                    .andExpect(jsonPath("$.data.message").value("Insufficient balance for transaction amount: -100.00"));

            // Verify no transaction was saved
            assertThat(transactionRepository.findByOrderId("ORD-123456")).isEmpty();
        }
    }

    @Nested
    class DeleteTransaction {
        @Test
        void should_delete_transaction_successfully() throws Exception {
            // Given - Create a transaction first
            mockMvc.perform(post("/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_CREDIT_TRANSACTION_REQUEST))
                    .andExpect(status().isCreated());

            Transaction savedTransaction = transactionRepository.findByOrderId("ORD-123456")
                    .orElseThrow(() -> new AssertionError("Transaction not found"));

            // When
            mockMvc.perform(delete("/transactions/{id}", savedTransaction.getId()))
                    .andExpect(status().isNoContent());

            // Then
            assertThat(transactionRepository.findById(savedTransaction.getId())).isEmpty();

            // Verify audit log - find the most recent DELETE operation
            List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId(
                    "Transaction", String.valueOf(savedTransaction.getId()));
            AuditLog deleteLog = auditLogs.stream()
                    .filter(log -> "DELETE".equals(log.getOperation()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Delete audit log not found"));

            assertThat(deleteLog.getOperation()).isEqualTo("DELETE");
            assertThat(deleteLog.getDetails()).contains("ORD-123456");
        }

        @Test
        void should_return_404_when_deleting_non_existent_transaction() throws Exception {
            // When/Then
            mockMvc.perform(delete("/transactions/{id}", 999))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.transactionId").value(999))
                    .andExpect(jsonPath("$.data.message").value("Transaction not found with ID: 999"));
        }
    }

    @Nested
    class UpdateTransaction {
        private static final String UPDATE_TRANSACTION_REQUEST = """
                {
                    "orderId": "ORD-999999",
                    "accountId": "ACC-999999",
                    "amount": 999.99,
                    "type": "DEBIT",
                    "category": "SHOPPING",
                    "description": "Updated description"
                }
                """;

        @Test
        void should_update_transaction_successfully_and_ignore_immutable_fields() throws Exception {
            // Given - Create a transaction first
            mockMvc.perform(post("/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CREATE_CREDIT_TRANSACTION_REQUEST))
                    .andExpect(status().isCreated());

            Transaction savedTransaction = transactionRepository.findByOrderId("ORD-123456")
                    .orElseThrow(() -> new AssertionError("Transaction not found"));
            LocalDateTime originalCreatedAt = savedTransaction.getCreatedAt();
            LocalDateTime originalUpdatedAt = savedTransaction.getUpdatedAt();

            // When
            mockMvc.perform(put("/transactions/{id}", savedTransaction.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_TRANSACTION_REQUEST))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedTransaction.getId()))
                    .andExpect(jsonPath("$.orderId").value(savedTransaction.getOrderId()))
                    .andExpect(jsonPath("$.accountId").value(savedTransaction.getAccountId()))
                    .andExpect(jsonPath("$.amount").value(savedTransaction.getAmount()))
                    .andExpect(jsonPath("$.type").value(savedTransaction.getType().name()))
                    .andExpect(jsonPath("$.category").value("SHOPPING"))
                    .andExpect(jsonPath("$.description").value("Updated description"))
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists())
                    .andExpect(result -> {
                        String updatedAt = JsonPath.read(result.getResponse().getContentAsString(), "$.updatedAt");
                        assertThat(updatedAt).isNotEqualTo(originalUpdatedAt.toString());
                    });

            // Then
            Transaction updatedTransaction = transactionRepository.findById(savedTransaction.getId())
                    .orElseThrow(() -> new AssertionError("Transaction not found"));
            assertThat(updatedTransaction.getCreatedAt()).isEqualTo(originalCreatedAt);
            assertThat(updatedTransaction.getUpdatedAt()).isAfter(originalUpdatedAt);
            assertThat(updatedTransaction.getCategory()).isEqualTo(TransactionCategory.SHOPPING);
            assertThat(updatedTransaction.getDescription()).isEqualTo("Updated description");

            // Verify immutable fields remain unchanged
            assertThat(updatedTransaction.getOrderId()).isEqualTo("ORD-123456");
            assertThat(updatedTransaction.getAccountId()).isEqualTo("ACC-123456");
            assertThat(updatedTransaction.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(updatedTransaction.getType()).isEqualTo(TransactionType.CREDIT);

            // Verify audit log - find the most recent UPDATE operation
            List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId(
                    "Transaction", String.valueOf(savedTransaction.getId()));
            AuditLog updateLog = auditLogs.stream()
                    .filter(log -> "UPDATE".equals(log.getOperation()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Update audit log not found"));
            assertThat(updateLog.getOperation()).isEqualTo("UPDATE");
            assertThat(updateLog.getDetails()).contains("SALARY");
        }

        @Test
        void should_return_404_when_updating_non_existent_transaction() throws Exception {
            // When/Then
            mockMvc.perform(put("/transactions/{id}", 999)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(UPDATE_TRANSACTION_REQUEST))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"))
                    .andExpect(jsonPath("$.data.transactionId").value(999))
                    .andExpect(jsonPath("$.data.message").value("Transaction not found with ID: 999"));
        }
    }

    @Nested
    class ListTransactions {
        @Test
        void should_return_paginated_transactions() throws Exception {
            // Given - Create multiple transactions
            for (int i = 1; i <= 15; i++) {
                String orderId = String.format("ORD-%06d", i);
                String accountId = String.format("ACC-%06d", i);
                String request = CREATE_CREDIT_TRANSACTION_REQUEST
                        .replace("ORD-123456", orderId)
                        .replace("ACC-123456", accountId);

                mockMvc.perform(post("/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                        .andExpect(status().isCreated());
            }

            // When & Then - First page
            mockMvc.perform(get("/transactions")
                            .param("pageNumber", "1")
                            .param("pageSize", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contents").isArray())
                    .andExpect(jsonPath("$.contents", hasSize(5)))
                    .andExpect(jsonPath("$.totalSize").value(15));

            // When & Then - Last page
            mockMvc.perform(get("/transactions")
                            .param("pageNumber", "3")
                            .param("pageSize", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contents").isArray())
                    .andExpect(jsonPath("$.contents", hasSize(5)))
                    .andExpect(jsonPath("$.totalSize").value(15));
        }

        @Test
        void should_return_empty_page_when_no_transactions() throws Exception {
            mockMvc.perform(get("/transactions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contents").isArray())
                    .andExpect(jsonPath("$.contents", hasSize(0)))
                    .andExpect(jsonPath("$.totalSize").value(0));
        }

        @Test
        void should_return_empty_page_when_page_size_is_invalid() throws Exception {
            mockMvc.perform(get("/transactions")
                            .param("pageNumber", "1")
                            .param("pageSize", "0"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contents").isArray())
                    .andExpect(jsonPath("$.contents", hasSize(0)))
                    .andExpect(jsonPath("$.totalSize").value(0));
        }

        @Test
        void should_return_empty_page_when_page_number_is_invalid() throws Exception {
            mockMvc.perform(get("/transactions")
                            .param("pageNumber", "0")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contents").isArray())
                    .andExpect(jsonPath("$.contents", hasSize(0)))
                    .andExpect(jsonPath("$.totalSize").value(0));
        }

        @Test
        void should_return_empty_page_when_page_exceeds_total() throws Exception {
            // Given - Create 5 transactions
            for (int i = 1; i <= 5; i++) {
                String orderId = String.format("ORD-%06d", i);
                String accountId = String.format("ACC-%06d", i);
                String request = CREATE_CREDIT_TRANSACTION_REQUEST
                        .replace("ORD-123456", orderId)
                        .replace("ACC-123456", accountId);

                mockMvc.perform(post("/transactions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                        .andExpect(status().isCreated());
            }

            // When & Then - Request page beyond total records
            mockMvc.perform(get("/transactions")
                            .param("pageNumber", "2")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contents").isArray())
                    .andExpect(jsonPath("$.contents", hasSize(0)))
                    .andExpect(jsonPath("$.totalSize").value(5));
        }
    }
}  