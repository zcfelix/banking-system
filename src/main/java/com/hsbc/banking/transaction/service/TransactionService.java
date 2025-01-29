package com.hsbc.banking.transaction.service;

import com.hsbc.banking.transaction.dto.CreateTransactionRequest;
import com.hsbc.banking.transaction.exception.InsufficientBalanceException;
import com.hsbc.banking.transaction.model.Transaction;
import com.hsbc.banking.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final ExternalAccountService externalAccountService;

    public TransactionService(TransactionRepository transactionRepository,
                              ExternalAccountService externalAccountService) {
        this.transactionRepository = transactionRepository;
        this.externalAccountService = externalAccountService;
    }

    public Transaction createTransaction(CreateTransactionRequest request) {
        Transaction transaction = Transaction.create(
                request.orderId(),
                request.accountId(),
                request.amount(),
                request.type(),
                request.category(),
                request.description()
        );

        // Check if account has sufficient balance for debit transactions
        if (transaction.getType().isDebit() &&
            !externalAccountService.hasSufficientBalance(transaction.getAccountId(), transaction.getAmount())) {
            throw new InsufficientBalanceException(
                    transaction.getAccountId(),
                    "Insufficient balance for transaction amount: " + transaction.getAmount()
            );
        }

        return transactionRepository.save(transaction);
    }
} 