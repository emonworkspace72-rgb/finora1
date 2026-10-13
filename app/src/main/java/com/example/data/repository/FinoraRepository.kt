package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.DefaultData
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.RecurringEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.remote.FirestoreSyncManager
import com.example.domain.model.AccountType
import com.example.domain.model.Money
import com.example.domain.model.TransactionType
import com.example.domain.service.BalanceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class FinoraRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.finoraDao()
    private val syncManager = FirestoreSyncManager(context, dao)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Ensure default accounts and categories are present
        scope.launch {
            val categories = dao.getAllCategories().first()
            if (categories.isEmpty()) {
                DefaultData.DEFAULT_ACCOUNTS.forEach { dao.upsertAccount(it) }
                dao.insertCategoriesIfEmpty(DefaultData.DEFAULT_CATEGORIES)
            }
        }
    }

    // --- Accounts ---
    val allActiveAccounts: Flow<List<AccountEntity>> = dao.getAllActiveAccounts()
    val allAccounts: Flow<List<AccountEntity>> = dao.getAllAccounts()

    suspend fun createOrUpdateAccount(account: AccountEntity) {
        dao.upsertAccount(account)
        syncManager.uploadAccount(account)
    }

    suspend fun deleteAccount(id: String) {
        dao.deleteAccountById(id)
        dao.enqueueSync(SyncQueueEntity(entityType = "account", entityId = id, action = "DELETE", payloadJson = "{}"))
    }

    // --- Categories ---
    val allCategories: Flow<List<CategoryEntity>> = dao.getAllCategories()
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> = dao.getCategoriesByType(type)

    suspend fun createOrUpdateCategory(category: CategoryEntity) {
        dao.upsertCategory(category)
    }

    // --- Transactions ---
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()

    fun getTransactionsInRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> =
        dao.getTransactionsInRange(startMillis, endMillis)

    suspend fun recordExpense(
        amountCents: Long,
        accountId: String,
        categoryId: String,
        dateMillis: Long = System.currentTimeMillis(),
        note: String = "",
        merchant: String = "",
        currency: String = "BDT"
    ): TransactionEntity {
        require(amountCents > 0) { "Expense amount must be positive" }
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = TransactionType.EXPENSE.name,
            amountCents = amountCents,
            currency = currency,
            accountId = accountId,
            categoryId = categoryId,
            dateMillis = dateMillis,
            note = note,
            merchant = merchant
        )
        dao.upsertTransaction(tx)
        syncManager.uploadTransaction(tx)
        return tx
    }

    suspend fun recordIncome(
        amountCents: Long,
        accountId: String,
        categoryId: String,
        dateMillis: Long = System.currentTimeMillis(),
        note: String = "",
        merchant: String = "",
        currency: String = "BDT"
    ): TransactionEntity {
        require(amountCents > 0) { "Income amount must be positive" }
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = TransactionType.INCOME.name,
            amountCents = amountCents,
            currency = currency,
            accountId = accountId,
            categoryId = categoryId,
            dateMillis = dateMillis,
            note = note,
            merchant = merchant
        )
        dao.upsertTransaction(tx)
        syncManager.uploadTransaction(tx)
        return tx
    }

    suspend fun recordTransfer(
        amountCents: Long,
        sourceAccountId: String,
        destAccountId: String,
        categoryId: String = "cat_transfer",
        dateMillis: Long = System.currentTimeMillis(),
        note: String = "",
        currency: String = "BDT"
    ): TransactionEntity {
        require(amountCents > 0) { "Transfer amount must be positive" }
        require(sourceAccountId != destAccountId) { "Source and destination accounts must be distinct" }

        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = TransactionType.TRANSFER.name,
            amountCents = amountCents,
            currency = currency,
            accountId = sourceAccountId,
            destinationAccountId = destAccountId,
            categoryId = categoryId,
            dateMillis = dateMillis,
            note = note
        )
        dao.upsertTransaction(tx)
        syncManager.uploadTransaction(tx)
        return tx
    }

    suspend fun deleteTransaction(id: String) {
        dao.deleteTransactionById(id)
        dao.enqueueSync(SyncQueueEntity(entityType = "transaction", entityId = id, action = "DELETE", payloadJson = "{}"))
    }

    // --- Budgets ---
    val allBudgets: Flow<List<BudgetEntity>> = dao.getAllBudgets()

    suspend fun upsertBudget(budget: BudgetEntity) {
        dao.upsertBudget(budget)
        syncManager.uploadBudget(budget)
    }

    suspend fun deleteBudget(id: String) {
        dao.deleteBudgetById(id)
        dao.enqueueSync(SyncQueueEntity(entityType = "budget", entityId = id, action = "DELETE", payloadJson = "{}"))
    }

    // --- Goals ---
    val allGoals: Flow<List<GoalEntity>> = dao.getAllGoals()

    suspend fun upsertGoal(goal: GoalEntity) {
        dao.upsertGoal(goal)
        syncManager.uploadGoal(goal)
    }

    suspend fun contributeToGoal(goalId: String, addCents: Long) {
        val currentGoals = allGoals.first()
        val goal = currentGoals.find { it.id == goalId } ?: return
        val updated = goal.copy(allocatedCents = goal.allocatedCents + addCents, updatedAt = System.currentTimeMillis())
        upsertGoal(updated)
    }

    suspend fun deleteGoal(id: String) {
        dao.deleteGoalById(id)
        dao.enqueueSync(SyncQueueEntity(entityType = "goal", entityId = id, action = "DELETE", payloadJson = "{}"))
    }

    // --- Recurring ---
    val allRecurring: Flow<List<RecurringEntity>> = dao.getAllRecurring()

    suspend fun upsertRecurring(rec: RecurringEntity) {
        dao.upsertRecurring(rec)
        syncManager.uploadRecurring(rec)
    }

    suspend fun markBillAsPaid(recurringId: String) {
        val all = allRecurring.first()
        val rec = all.find { it.id == recurringId } ?: return

        // Record completed expense transaction
        recordExpense(
            amountCents = rec.amountCents,
            accountId = rec.accountId,
            categoryId = rec.categoryId,
            dateMillis = System.currentTimeMillis(),
            note = "Bill Payment: ${rec.name}",
            merchant = rec.name,
            currency = rec.currency
        )

        // Mark paid or advance to next due date
        val updated = rec.copy(isPaid = true, updatedAt = System.currentTimeMillis())
        upsertRecurring(updated)
    }

    suspend fun deleteRecurring(id: String) {
        dao.deleteRecurringById(id)
        dao.enqueueSync(SyncQueueEntity(entityType = "recurring", entityId = id, action = "DELETE", payloadJson = "{}"))
    }

    suspend fun triggerCloudSync(): Result<String> {
        return syncManager.syncAll()
    }
}
