package com.example.domain.service

import com.example.domain.model.CarryOverPolicy
import com.example.domain.model.TransactionType

data class BudgetStatus(
    val budgetId: String,
    val name: String,
    val limitCents: Long,
    val spentCents: Long,
    val remainingCents: Long,
    val usagePercentage: Float,
    val isExceeded: Boolean
)

object BudgetCalculator {

    data class BudgetTransaction(
        val categoryId: String,
        val type: TransactionType,
        val amountCents: Long,
        val dateMillis: Long
    )

    fun calculateBudgetStatus(
        budgetId: String,
        name: String,
        limitCents: Long,
        categoryIds: Set<String>,
        transactions: List<BudgetTransaction>,
        periodStartMillis: Long,
        periodEndMillis: Long,
        carryOverPolicy: CarryOverPolicy = CarryOverPolicy.NONE,
        previousPeriodSurplusCents: Long = 0L
    ): BudgetStatus {
        val effectiveLimit = when (carryOverPolicy) {
            CarryOverPolicy.NONE -> limitCents
            CarryOverPolicy.CARRY_UNUSED -> limitCents + Math.max(0L, previousPeriodSurplusCents)
            CarryOverPolicy.CARRY_NET -> limitCents + previousPeriodSurplusCents
        }

        var totalSpent = 0L
        for (tx in transactions) {
            if (tx.dateMillis in periodStartMillis..periodEndMillis) {
                if (categoryIds.isEmpty() || categoryIds.contains(tx.categoryId)) {
                    if (tx.type == TransactionType.EXPENSE) {
                        totalSpent += tx.amountCents
                    }
                }
            }
        }

        val remaining = effectiveLimit - totalSpent
        val usagePercentage = if (effectiveLimit > 0) {
            (totalSpent.toFloat() / effectiveLimit.toFloat()) * 100f
        } else {
            0f
        }

        return BudgetStatus(
            budgetId = budgetId,
            name = name,
            limitCents = effectiveLimit,
            spentCents = totalSpent,
            remainingCents = remaining,
            usagePercentage = usagePercentage,
            isExceeded = remaining < 0
        )
    }
}
