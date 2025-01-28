package com.hsbc.banking.transaction.repository;

import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.exception.DuplicateTransactionException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryTransactionRepository implements TransactionRepository {
    private final List<Transaction> transactions = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Transaction save(Transaction transaction) {
        checkDuplicate(transaction);
        transaction.setId(idGenerator.getAndIncrement());
        transactions.add(transaction);
        return transaction;
    }


    @Override
    public Optional<Transaction> findById(Long id) {
        return transactions.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Transaction> findAll() {
        return new ArrayList<>(transactions);
    }

    private void checkDuplicate(Transaction transaction) {
        boolean isDuplicate = transactions.stream()
                .anyMatch(t -> t.getOrderId().equals(transaction.getOrderId()));

        if (isDuplicate) {
            throw new DuplicateTransactionException(
                    Map.of("orderId", transaction.getOrderId(),
                            "message", "Transaction with order ID already exists")
            );
        }
    }
}
