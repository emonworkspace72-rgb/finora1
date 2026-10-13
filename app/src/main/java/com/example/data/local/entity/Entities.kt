package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val name: String,
    val type: String, // CASH, BANK, SAVINGS, CREDIT_CARD, E_WALLET, INVESTMENT
    val currency: String = "BDT",
    val openingBalanceCents: Long = 0L,
    val isArchived: Boolean = false,
    val colorHex: String = "#B7F345",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val name: String,
    val type: String, // EXPENSE, INCOME
    val iconName: String,
    val colorHex: String,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val type: String, // EXPENSE, INCOME, TRANSFER, ADJUSTMENT
    val amountCents: Long,
    val currency: String = "BDT",
    val accountId: String,
    val destinationAccountId: String? = null,
    val categoryId: String,
    val dateMillis: Long,
    val note: String = "",
    val merchant: String = "",
    val recurringRuleId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val name: String,
    val limitCents: Long,
    val currency: String = "BDT",
    val period: String = "MONTHLY",
    val categoryIds: String = "", // Comma-separated or JSON list
    val carryOverPolicy: String = "NONE",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "savings_goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val name: String,
    val targetCents: Long,
    val allocatedCents: Long = 0L,
    val currency: String = "BDT",
    val targetDateMillis: Long? = null,
    val linkedAccountId: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recurring_rules")
data class RecurringEntity(
    @PrimaryKey val id: String,
    val userId: String = "",
    val name: String,
    val amountCents: Long,
    val currency: String = "BDT",
    val frequency: String = "MONTHLY",
    val nextDueMillis: Long,
    val accountId: String,
    val categoryId: String,
    val type: String = "EXPENSE",
    val isBill: Boolean = true,
    val isPaid: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0L,
    val entityType: String, // account, transaction, category, budget, goal, recurring
    val entityId: String,
    val action: String, // UPSERT, DELETE
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
