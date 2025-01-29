package com.hsbc.banking.transaction.integration;

import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionCategory;
import com.hsbc.banking.transaction.model.TransactionType;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import com.hsbc.banking.transaction.repository.AuditLogRepository;
import com.hsbc.banking.transaction.model.AuditLog;
import com.hsbc.banking.transaction.service.ExternalAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.jayway.jsonpath.JsonPath;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
            
            // Verify audit log
            List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId(
                    "Transaction", String.valueOf(savedTransaction.getId()));
            assertThat(auditLogs).hasSize(1);
            AuditLog auditLog = auditLogs.get(0);
            assertThat(auditLog.getOperation()).isEqualTo("DELETE");
            assertThat(auditLog.getDetails()).contains("ORD-123456");
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
}  