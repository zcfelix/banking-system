package com.hsbc.banking.transaction.repository;

import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.exception.DuplicateTransactionException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {
    private final Map<Long, Transaction> transactions = new ConcurrentHashMap<>();
    private final Map<String, Transaction> orderIdIndex = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Transaction save(Transaction transaction) {
        // check if transaction with order ID already exists, which means it's a duplicate transaction
        if (orderIdIndex.containsKey(transaction.getOrderId())) {
            throw new DuplicateTransactionException(
                    Map.of("orderId", transaction.getOrderId(),
                            "message", "Transaction with order ID already exists")
            );
        }

        Long id = idGenerator.getAndIncrement();
        transaction.setId(id);
        
        transactions.put(id, transaction);
        orderIdIndex.put(transaction.getOrderId(), transaction);
        
        return transaction;
    }

    @Override
    public Transaction update(Transaction transaction) {
        transactions.put(transaction.getId(), transaction);
        return transaction;
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return Optional.ofNullable(transactions.get(id));
    }

    @Override
    public Optional<Transaction> findByOrderId(String orderId) {
        return Optional.ofNullable(orderIdIndex.get(orderId));
    }

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<>(transactions.values());
    }

    @Override
    public void deleteById(Long id) {
        Transaction transaction = transactions.get(id);
        if (transaction != null) {
            transactions.remove(id);
            orderIdIndex.remove(transaction.getOrderId());
        }
    }

    @Override
    public void clear() {
        transactions.clear();
        orderIdIndex.clear();
        idGenerator.set(1);
    }

}
