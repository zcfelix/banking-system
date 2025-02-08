package com.hsbc.banking.transaction.repository;

import com.hsbc.banking.transaction.exception.ConcurrentUpdateException;
import com.hsbc.banking.transaction.exception.DuplicateTransactionException;
import com.hsbc.banking.transaction.exception.TransactionNotFoundException;
import com.hsbc.banking.transaction.model.Transaction;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemoryTransactionRepositoryImpl implements TransactionRepository {
    private final ConcurrentSkipListMap<Long, Transaction> transactions = new ConcurrentSkipListMap<>();
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
    public synchronized Transaction update(Transaction transaction) {
        Transaction existingTransaction = transactions.get(transaction.getId());
        if (existingTransaction == null) {
            throw new TransactionNotFoundException(transaction.getId());
        }

        // check if the version of the existing transaction matches the version of the request
        if (!existingTransaction.getVersion().equals(transaction.getVersion())) {
            throw new ConcurrentUpdateException(Map.of(
                "transactionId", transaction.getId(),
                "message", "Transaction was updated by another user",
                "currentVersion", existingTransaction.getVersion(),
                "requestVersion", transaction.getVersion()
            ));
        }

        // update the transaction and increment the version
        transaction.incrementVersion();
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
    public List<Transaction> findAll(int offset, int limit) {
        return transactions.values().stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return transactions.size();
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
