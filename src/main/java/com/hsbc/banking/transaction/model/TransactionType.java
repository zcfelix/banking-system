package com.hsbc.banking.transaction.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TransactionType {
    // Basic transaction types
    CREDIT("Credit", "Credit/Income transaction"),
    DEBIT("Debit", "Debit/Expense transaction"),

    // Transfer related
    TRANSFER_IN("Transfer In", "Incoming transfer"),
    TRANSFER_OUT("Transfer Out", "Outgoing transfer"),

    // Investment related
    INVESTMENT("Investment", "Investment transaction"),
    INVESTMENT_RETURN("Investment Return", "Investment return/profit"),

    // Loan related
    LOAN_DISBURSEMENT("Loan Disbursement", "Loan payout"),
    LOAN_REPAYMENT("Loan Repayment", "Loan repayment"),

    // Fee related
    FEE("Fee", "Service fee"),
    INTEREST("Interest", "Interest charge/payment"),
    CHARGE("Charge", "General charge"),
    REFUND("Refund", "Refund payment");

    private final String displayName;
    private final String description;

    TransactionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @JsonValue
    public String getName() {
        return name();
    }

    @JsonCreator
    public static TransactionType fromString(String value) {
        return valueOf(value.toUpperCase());
    }

    // Detect if the transaction type is a credit transaction, i.e. money is added to the account from bank's perspective
    public boolean isCredit() {
        return this == CREDIT || this == TRANSFER_IN || 
               this == INVESTMENT_RETURN || this == LOAN_DISBURSEMENT || 
               this == REFUND;
    }

    // Detect if the transaction type is a debit transaction, i.e. money is deducted from the account from bank's perspective
    public boolean isDebit() {
        return this == DEBIT || this == TRANSFER_OUT || 
               this == INVESTMENT || this == LOAN_REPAYMENT || 
               this == FEE || this == CHARGE;
    }
} 