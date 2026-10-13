package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.FinoraRepository
import com.example.domain.model.TransactionType
import com.example.ui.accounts.AddAccountDialog
import com.example.ui.accounts.TransferDialog
import com.example.ui.accounts.WalletsSheet
import com.example.ui.activity.ActivityScreen
import com.example.ui.components.FinoraTopBar
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.insights.InsightsScreen
import com.example.ui.navigation.FinoraBottomNavBar
import com.example.ui.navigation.FinoraTab
import com.example.ui.planning.PlanningScreen
import com.example.ui.quickadd.QuickAddSheet
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.FinoraTheme
import com.example.ui.wheel.WheelScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: FinoraRepository

    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = FinoraRepository(applicationContext)

        setContent {
            FinoraApp(repository = repository)
        }
    }
}

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun FinoraApp(repository: FinoraRepository) {
    val systemInDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(true) } // Default to dark fintech styling
    var currentCurrency by remember { mutableStateOf("BDT") }

    FinoraTheme(darkTheme = isDarkTheme) {
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }

        // Observe reactive database flows
        val accounts by repository.allActiveAccounts.collectAsStateWithLifecycle(initialValue = emptyList())
        val transactions by repository.allTransactions.collectAsStateWithLifecycle(initialValue = emptyList())
        val categories by repository.allCategories.collectAsStateWithLifecycle(initialValue = emptyList())
        val budgets by repository.allBudgets.collectAsStateWithLifecycle(initialValue = emptyList())
        val goals by repository.allGoals.collectAsStateWithLifecycle(initialValue = emptyList())
        val recurring by repository.allRecurring.collectAsStateWithLifecycle(initialValue = emptyList())

        // Navigation & Dialog states
        var currentTab by remember { mutableStateOf(FinoraTab.HOME) }
        var showSettings by remember { mutableStateOf(false) }
        var showWalletsSheet by remember { mutableStateOf(false) }
        var showTransferDialog by remember { mutableStateOf(false) }
        var showAddAccountDialog by remember { mutableStateOf(false) }

        // Quick Add sheet state
        var showQuickAddSheet by remember { mutableStateOf(false) }
        var quickAddType by remember { mutableStateOf(TransactionType.EXPENSE) }
        var quickAddPreselectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }

        // Undo buffer for last deleted or added transaction
        var lastDeletedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                FinoraTopBar(
                    currentTitle = if (showSettings) "Settings" else when (currentTab) {
                        FinoraTab.HOME -> "Finora"
                        FinoraTab.ACTIVITY -> "Activity"
                        FinoraTab.WHEEL -> "Quick Wheel"
                        FinoraTab.INSIGHTS -> "Insights"
                        FinoraTab.PLANNING -> "Planning"
                    },
                    onWalletsClick = { showWalletsSheet = true },
                    onSettingsClick = { showSettings = !showSettings }
                )
            },
            bottomBar = {
                if (!showSettings) {
                    FinoraBottomNavBar(
                        selectedTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showSettings) {
                    SettingsScreen(
                        currentCurrency = currentCurrency,
                        onCurrencyChanged = { currentCurrency = it },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = it },
                        onTriggerSync = { repository.triggerCloudSync() }
                    )
                } else {
                    when (currentTab) {
                        FinoraTab.HOME -> {
                            DashboardScreen(
                                accounts = accounts,
                                transactions = transactions,
                                categories = categories,
                                budgets = budgets,
                                upcomingBills = recurring.filter { !it.isPaid },
                                onQuickAddClick = { type ->
                                    quickAddType = type
                                    quickAddPreselectedCategory = null
                                    showQuickAddSheet = true
                                },
                                onTransferClick = { showTransferDialog = true },
                                onViewAllActivity = { currentTab = FinoraTab.ACTIVITY },
                                onAccountClick = { showWalletsSheet = true }
                            )
                        }
                        FinoraTab.ACTIVITY -> {
                            ActivityScreen(
                                transactions = transactions,
                                categories = categories,
                                accounts = accounts,
                                onDeleteTransaction = { txId ->
                                    val txToDelete = transactions.find { it.id == txId }
                                    scope.launch {
                                        repository.deleteTransaction(txId)
                                        lastDeletedTransaction = txToDelete
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Transaction deleted",
                                            actionLabel = "UNDO",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed && lastDeletedTransaction != null) {
                                            val t = lastDeletedTransaction!!
                                            if (t.type == TransactionType.EXPENSE.name) {
                                                repository.recordExpense(t.amountCents, t.accountId, t.categoryId, t.dateMillis, t.note, t.merchant, t.currency)
                                            } else if (t.type == TransactionType.INCOME.name) {
                                                repository.recordIncome(t.amountCents, t.accountId, t.categoryId, t.dateMillis, t.note, t.merchant, t.currency)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        FinoraTab.WHEEL -> {
                            WheelScreen(
                                categories = categories,
                                transactions = transactions,
                                onOpenQuickAdd = { type, cat ->
                                    quickAddType = type
                                    quickAddPreselectedCategory = cat
                                    showQuickAddSheet = true
                                }
                            )
                        }
                        FinoraTab.INSIGHTS -> {
                            InsightsScreen(
                                transactions = transactions,
                                categories = categories
                            )
                        }
                        FinoraTab.PLANNING -> {
                            PlanningScreen(
                                budgets = budgets,
                                goals = goals,
                                recurring = recurring,
                                transactions = transactions,
                                categories = categories,
                                accounts = accounts,
                                onAddBudget = { budget ->
                                    scope.launch { repository.upsertBudget(budget) }
                                },
                                onDeleteBudget = { id ->
                                    scope.launch { repository.deleteBudget(id) }
                                },
                                onAddGoal = { goal ->
                                    scope.launch { repository.upsertGoal(goal) }
                                },
                                onContributeGoal = { goalId, cents ->
                                    scope.launch { repository.contributeToGoal(goalId, cents) }
                                },
                                onDeleteGoal = { id ->
                                    scope.launch { repository.deleteGoal(id) }
                                },
                                onAddRecurring = { bill ->
                                    scope.launch { repository.upsertRecurring(bill) }
                                },
                                onMarkBillPaid = { id ->
                                    scope.launch { repository.markBillAsPaid(id) }
                                },
                                onDeleteRecurring = { id ->
                                    scope.launch { repository.deleteRecurring(id) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Quick Add Modal Bottom Sheet
        if (showQuickAddSheet) {
            QuickAddSheet(
                initialType = quickAddType,
                categories = categories,
                accounts = accounts,
                preselectedCategory = quickAddPreselectedCategory,
                onDismiss = { showQuickAddSheet = false },
                onSave = { type, amountCents, accountId, categoryId, note, merchant ->
                    scope.launch {
                        if (type == TransactionType.EXPENSE) {
                            repository.recordExpense(amountCents, accountId, categoryId, note = note, merchant = merchant, currency = currentCurrency)
                        } else {
                            repository.recordIncome(amountCents, accountId, categoryId, note = note, merchant = merchant, currency = currentCurrency)
                        }
                        showQuickAddSheet = false
                        snackbarHostState.showSnackbar(
                            message = "${type.name.lowercase().replaceFirstChar { it.uppercase() }} recorded successfully!",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }

        // Wallets / Accounts Bottom Sheet
        if (showWalletsSheet) {
            WalletsSheet(
                accounts = accounts,
                transactions = transactions,
                onDismiss = { showWalletsSheet = false },
                onAddAccountClick = {
                    showWalletsSheet = false
                    showAddAccountDialog = true
                },
                onTransferClick = {
                    showWalletsSheet = false
                    showTransferDialog = true
                }
            )
        }

        // Transfer Dialog
        if (showTransferDialog) {
            TransferDialog(
                accounts = accounts,
                onDismiss = { showTransferDialog = false },
                onConfirmTransfer = { srcId, dstId, amountCents, note ->
                    scope.launch {
                        repository.recordTransfer(
                            amountCents = amountCents,
                            sourceAccountId = srcId,
                            destAccountId = dstId,
                            note = note,
                            currency = currentCurrency
                        )
                        showTransferDialog = false
                        snackbarHostState.showSnackbar("Transfer completed!")
                    }
                }
            )
        }

        // Add Account Dialog
        if (showAddAccountDialog) {
            AddAccountDialog(
                onDismiss = { showAddAccountDialog = false },
                onSave = { acc ->
                    scope.launch {
                        repository.createOrUpdateAccount(acc)
                        showAddAccountDialog = false
                        snackbarHostState.showSnackbar("Account created!")
                    }
                }
            )
        }
    }
}
