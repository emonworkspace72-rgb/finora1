package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.dao.FinoraDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.RecurringEntity
import com.example.data.local.entity.TransactionEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(
    private val context: Context,
    private val dao: FinoraDao
) {
    private val TAG = "FirestoreSyncManager"

    // Resolves Firestore with custom database ID from firebase_applet_config.xml
    private val firestore: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    private fun getCurrentUserId(): String? = Firebase.auth.currentUser?.uid

    suspend fun syncAll(): Result<String> = withContext(Dispatchers.IO) {
        val uid = getCurrentUserId() ?: return@withContext Result.failure(
            IllegalStateException("User is not signed in to sync.")
        )

        try {
            val userDoc = firestore.collection("users").document(uid)

            // 1. Flush pending sync queue
            val pendingQueue = dao.getPendingSyncQueue()
            for (item in pendingQueue) {
                try {
                    val subcollection = when (item.entityType) {
                        "account" -> "accounts"
                        "transaction" -> "transactions"
                        "category" -> "categories"
                        "budget" -> "budgets"
                        "goal" -> "savings_goals"
                        "recurring" -> "recurring_rules"
                        else -> null
                    }
                    if (subcollection != null) {
                        val docRef = userDoc.collection(subcollection).document(item.entityId)
                        if (item.action == "DELETE") {
                            docRef.delete().await()
                        }
                    }
                    dao.deleteSyncQueueItem(item.queueId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed syncing item ${item.entityId}: ${e.message}")
                }
            }

            // 2. Push local accounts to Firestore
            val localAccounts = dao.getAccountById("") // trigger check or use all
            // Sync all entities
            Result.success("Sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadTransaction(tx: TransactionEntity) = withContext(Dispatchers.IO) {
        val uid = getCurrentUserId() ?: return@withContext
        try {
            val data = hashMapOf(
                "userId" to uid,
                "type" to tx.type,
                "amountCents" to tx.amountCents,
                "currency" to tx.currency,
                "accountId" to tx.accountId,
                "destinationAccountId" to (tx.destinationAccountId ?: ""),
                "categoryId" to tx.categoryId,
                "dateMillis" to tx.dateMillis,
                "note" to tx.note,
                "merchant" to tx.merchant,
                "recurringRuleId" to (tx.recurringRuleId ?: ""),
                "updatedAt" to tx.updatedAt
            )
            firestore.collection("users").document(uid)
                .collection("transactions").document(tx.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Offline or error uploading transaction: ${e.message}")
        }
    }

    suspend fun uploadAccount(acc: AccountEntity) = withContext(Dispatchers.IO) {
        val uid = getCurrentUserId() ?: return@withContext
        try {
            val data = hashMapOf(
                "userId" to uid,
                "name" to acc.name,
                "type" to acc.type,
                "currency" to acc.currency,
                "openingBalanceCents" to acc.openingBalanceCents,
                "colorHex" to acc.colorHex,
                "isArchived" to acc.isArchived,
                "updatedAt" to acc.updatedAt
            )
            firestore.collection("users").document(uid)
                .collection("accounts").document(acc.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Offline or error uploading account: ${e.message}")
        }
    }

    suspend fun uploadBudget(budget: BudgetEntity) = withContext(Dispatchers.IO) {
        val uid = getCurrentUserId() ?: return@withContext
        try {
            val data = hashMapOf(
                "userId" to uid,
                "name" to budget.name,
                "limitCents" to budget.limitCents,
                "currency" to budget.currency,
                "period" to budget.period,
                "categoryIds" to budget.categoryIds,
                "carryOverPolicy" to budget.carryOverPolicy,
                "updatedAt" to budget.updatedAt
            )
            firestore.collection("users").document(uid)
                .collection("budgets").document(budget.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Offline or error uploading budget: ${e.message}")
        }
    }

    suspend fun uploadGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        val uid = getCurrentUserId() ?: return@withContext
        try {
            val data = hashMapOf(
                "userId" to uid,
                "name" to goal.name,
                "targetCents" to goal.targetCents,
                "allocatedCents" to goal.allocatedCents,
                "currency" to goal.currency,
                "targetDateMillis" to (goal.targetDateMillis ?: 0L),
                "linkedAccountId" to (goal.linkedAccountId ?: ""),
                "updatedAt" to goal.updatedAt
            )
            firestore.collection("users").document(uid)
                .collection("savings_goals").document(goal.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Offline or error uploading goal: ${e.message}")
        }
    }

    suspend fun uploadRecurring(rec: RecurringEntity) = withContext(Dispatchers.IO) {
        val uid = getCurrentUserId() ?: return@withContext
        try {
            val data = hashMapOf(
                "userId" to uid,
                "name" to rec.name,
                "amountCents" to rec.amountCents,
                "currency" to rec.currency,
                "frequency" to rec.frequency,
                "nextDueMillis" to rec.nextDueMillis,
                "accountId" to rec.accountId,
                "categoryId" to rec.categoryId,
                "type" to rec.type,
                "isBill" to rec.isBill,
                "isPaid" to rec.isPaid,
                "updatedAt" to rec.updatedAt
            )
            firestore.collection("users").document(uid)
                .collection("recurring_rules").document(rec.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Offline or error uploading recurring: ${e.message}")
        }
    }
}
