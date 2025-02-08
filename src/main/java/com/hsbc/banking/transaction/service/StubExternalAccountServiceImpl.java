package com.hsbc.banking.transaction.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class StubExternalAccountServiceImpl implements ExternalAccountService {
    @Override
    public boolean hasSufficientBalance(String accountId, BigDecimal amount) {
        return true;
    }
}
