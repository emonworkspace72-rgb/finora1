package com.example.domain.model

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
    ADJUSTMENT
}

enum class AccountType(val displayName: String, val isLiability: Boolean) {
    CASH("Cash Wallet", false),
    BANK("Bank Account", false),
    SAVINGS("Savings Account", false),
    CREDIT_CARD("Credit Card", true),
    E_WALLET("E-Wallet", false),
    INVESTMENT("Investment", false)
}

enum class BudgetPeriod(val displayName: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

enum class CarryOverPolicy(val displayName: String) {
    NONE("Reset Each Period"),
    CARRY_UNUSED("Carry Surplus"),
    CARRY_NET("Carry Surplus & Deficit")
}

enum class RecurrenceFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    BI_WEEKLY("Every 2 Weeks"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

enum class BillStatus {
    UPCOMING,
    DUE_TODAY,
    OVERDUE,
    PAID
}
