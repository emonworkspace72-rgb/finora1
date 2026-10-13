package com.example.domain.service

import com.example.domain.model.AccountType
import com.example.domain.model.Money
import com.example.domain.model.TransactionType

data class AccountBalanceSummary(
    val accountId: String,
    val accountName: String,
    val accountType: AccountType,
    val currency: String,
    val currentBalanceCents: Long
)

data class NetWorthSummary(
    val totalAssetsCents: Long,
    val totalLiabilitiesCents: Long,
    val netWorthCents: Long,
    val availableCashCents: Long,
    val currency: String
)

object BalanceCalculator {

    data class TransactionRecord(
        val id: String,
        val type: TransactionType,
        val amountCents: Long,
        val accountId: String,
        val destinationAccountId: String? = null
    )

    data class AccountRecord(
        val id: String,
        val name: String,
        val type: AccountType,
        val currency: String,
        val openingBalanceCents: Long,
        val isArchived: Boolean = false
    )

    fun calculateAccountBalance(
        account: AccountRecord,
        transactions: List<TransactionRecord>
    ): Long {
        var balance = account.openingBalanceCents

        if (account.type.isLiability) {
            // For liability (Credit Card):
            // Expenses increase amount owed (+)
            // Incoming transfers (payments made toward card) reduce amount owed (-)
            for (tx in transactions) {
                if (tx.accountId == account.id) {
                    when (tx.type) {
                        TransactionType.EXPENSE -> balance += tx.amountCents
                        TransactionType.ADJUSTMENT -> balance += tx.amountCents
                        TransactionType.INCOME -> balance -= tx.amountCents
                        TransactionType.TRANSFER -> balance += tx.amountCents
                    }
                } else if (tx.destinationAccountId == account.id && tx.type == TransactionType.TRANSFER) {
                    // Payment from cash into credit card reduces credit card balance
                    balance -= tx.amountCents
                }
            }
        } else {
            // For asset accounts (Cash, Bank, Savings, E-Wallet):
            for (tx in transactions) {
                if (tx.accountId == account.id) {
                    when (tx.type) {
                        TransactionType.INCOME -> balance += tx.amountCents
                        TransactionType.EXPENSE -> balance -= tx.amountCents
                        TransactionType.TRANSFER -> balance -= tx.amountCents
                        TransactionType.ADJUSTMENT -> balance += tx.amountCents
                    }
                } else if (tx.destinationAccountId == account.id && tx.type == TransactionType.TRANSFER) {
                    // Internal transfer in
                    balance += tx.amountCents
                }
            }
        }

        return balance
    }

    fun calculateNetWorth(
        accounts: List<AccountRecord>,
        transactions: List<TransactionRecord>,
        primaryCurrency: String = "USD"
    ): NetWorthSummary {
        var totalAssets = 0L
        var totalLiabilities = 0L
        var availableCash = 0L

        val activeAccounts = accounts.filter { !it.isArchived }

        for (acc in activeAccounts) {
            val balance = calculateAccountBalance(acc, transactions)
            if (acc.type.isLiability) {
                totalLiabilities += balance
            } else {
                totalAssets += balance
                if (acc.type == AccountType.CASH || acc.type == AccountType.BANK || acc.type == AccountType.E_WALLET) {
                    availableCash += balance
                }
            }
        }

        val netWorth = totalAssets - totalLiabilities

        return NetWorthSummary(
            totalAssetsCents = totalAssets,
            totalLiabilitiesCents = totalLiabilities,
            netWorthCents = netWorth,
            availableCashCents = availableCash,
            currency = primaryCurrency
        )
    }
}
