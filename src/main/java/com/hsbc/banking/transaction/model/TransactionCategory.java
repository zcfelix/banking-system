package com.hsbc.banking.transaction.model;

public enum TransactionCategory {
    // Income categories
    SALARY("Salary", "Regular employment income"),
    BONUS("Bonus", "Additional employment compensation"),
    INVESTMENT_INCOME("Investment Income", "Income from investments"),
    INTEREST_EARNED("Interest Earned", "Interest earned from savings or investments"),

    // Expense categories
    UTILITIES("Utilities", "Bills for electricity, water, gas etc."),
    RENT("Rent", "Housing rental payment"),
    MORTGAGE("Mortgage", "Housing loan payment"),
    FOOD_DINING("Food & Dining", "Restaurants and groceries"),
    TRANSPORTATION("Transportation", "Public transport and vehicle expenses"),
    SHOPPING("Shopping", "Retail purchases"),
    HEALTHCARE("Healthcare", "Medical and healthcare expenses"),
    EDUCATION("Education", "Tuition and education related expenses"),

    // Transfer categories
    INTERNAL_TRANSFER("Internal Transfer", "Transfer between own accounts"),
    EXTERNAL_TRANSFER("External Transfer", "Transfer to other bank accounts"),

    // Financial service categories
    LOAN_DISBURSEMENT("Loan Disbursement", "Incoming loan amount"),
    LOAN_PAYMENT("Loan Payment", "Loan repayment"),
    INVESTMENT_BUY("Investment Buy", "Purchase of investments"),
    INVESTMENT_SELL("Investment Sell", "Sale of investments"),

    // Fee categories
    BANK_FEE("Bank Fee", "Bank service charges"),
    ATM_FEE("ATM Fee", "ATM withdrawal fees"),
    LATE_PAYMENT_FEE("Late Payment Fee", "Fee for late payments"),

    // Other
    OTHER("Other", "Uncategorized transaction");

    private final String displayName;
    private final String description;

    TransactionCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static TransactionCategory fromString(String category) {
        return valueOf(category.toUpperCase());
    }

    @Override
    public String toString() {
        return displayName;
    }
} 