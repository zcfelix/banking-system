package com.hsbc.banking.transaction.service;

import com.hsbc.banking.transaction.dto.CreateTransactionRequest;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.model.TransactionCategory;
import com.hsbc.banking.transaction.model.TransactionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CacheTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    public void testTransactionCache() {
        // 1. Create a new transaction
        CreateTransactionRequest request = new CreateTransactionRequest(
                "ORD-123456",
                "ACC-123456",
                BigDecimal.valueOf(100),
                TransactionType.CREDIT.name(),
                TransactionCategory.SALARY.name(),
                "Test Transaction"
        );
        
        Transaction created = transactionService.createTransaction(request);
        Long transactionId = created.getId();

        // 2. Verify the transaction exists in cache
        CaffeineCache transactionsCache = (CaffeineCache) cacheManager.getCache("transactions");
        assertNotNull(transactionsCache);
        
        // 3. First fetch (will query database)
        Transaction firstFetch = transactionService.getTransaction(transactionId);
        
        // 4. Second fetch (should hit cache)
        Transaction secondFetch = transactionService.getTransaction(transactionId);
        
        // 5. Verify both fetches return the same result
        assertEquals(firstFetch.getId(), secondFetch.getId());
        assertEquals(firstFetch.getOrderId(), secondFetch.getOrderId());
        
        // 6. Verify cache statistics
        assertTrue(transactionsCache.getNativeCache().stats().hitCount() > 0, 
                "Cache should have hits");
    }

    @Test
    public void testCacheEviction() {
        // 1. Create transaction
        CreateTransactionRequest request = new CreateTransactionRequest(
                "ORD-456789",
                "ACC-456789",
                BigDecimal.valueOf(200),
                TransactionType.CREDIT.name(),
                TransactionCategory.SALARY.name(),
                "Test Transaction for Eviction"
        );
        
        Transaction created = transactionService.createTransaction(request);
        Long transactionId = created.getId();

        // 2. Fetch transaction to ensure it's in cache
        transactionService.getTransaction(transactionId);

        // 3. Delete the transaction
        transactionService.deleteTransaction(transactionId);

        // 4. Verify the entry is removed from cache
        CaffeineCache transactionsCache = (CaffeineCache) cacheManager.getCache("transactions");
        assertNotNull(transactionsCache);
        assertNull(transactionsCache.get(transactionId));
    }
} 