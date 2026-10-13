package com.example.ui.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.RecurringEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.BillStatus
import com.example.domain.model.Money
import com.example.domain.model.TransactionType
import com.example.domain.service.BudgetCalculator
import com.example.domain.service.RecurrenceEngine
import com.example.ui.components.CategoryIconHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun PlanningScreen(
    budgets: List<BudgetEntity>,
    goals: List<GoalEntity>,
    recurring: List<RecurringEntity>,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onAddBudget: (BudgetEntity) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onAddGoal: (GoalEntity) -> Unit,
    onContributeGoal: (String, Long) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onAddRecurring: (RecurringEntity) -> Unit,
    onMarkBillPaid: (String) -> Unit,
    onDeleteRecurring: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("BUDGETS") } // BUDGETS, GOALS, BILLS
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }
    var contributeGoalTarget by remember { mutableStateOf<GoalEntity?>(null) }
    var contributeAmountInput by remember { mutableStateOf("") }

    val now = System.currentTimeMillis()
    val monthStart = remember(now) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.timeInMillis
    }
    val monthEnd = remember(now) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.timeInMillis
    }

    val budgetTxs = remember(transactions) {
        transactions.map {
            BudgetCalculator.BudgetTransaction(
                categoryId = it.categoryId,
                type = TransactionType.valueOf(it.type),
                amountCents = it.amountCents,
                dateMillis = it.dateMillis
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("planning_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        "BUDGETS" -> showAddBudgetDialog = true
                        "GOALS" -> showAddGoalDialog = true
                        "BILLS" -> showAddBillDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("planning_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlanningTab("Budgets", selectedTab == "BUDGETS") { selectedTab = "BUDGETS" }
                PlanningTab("Goals", selectedTab == "GOALS") { selectedTab = "GOALS" }
                PlanningTab("Bills", selectedTab == "BILLS") { selectedTab = "BILLS" }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                "BUDGETS" -> {
                    if (budgets.isEmpty()) {
                        EmptyPlanningState(
                            message = "No budgets created yet.\nSet up a monthly budget to track your spending limits!",
                            buttonText = "+ Create Budget",
                            onClick = { showAddBudgetDialog = true }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(budgets) { budget ->
                                val categoryIds = if (budget.categoryIds.isNotBlank()) {
                                    budget.categoryIds.split(",").toSet()
                                } else emptySet()

                                val status = BudgetCalculator.calculateBudgetStatus(
                                    budgetId = budget.id,
                                    name = budget.name,
                                    limitCents = budget.limitCents,
                                    categoryIds = categoryIds,
                                    transactions = budgetTxs,
                                    periodStartMillis = monthStart,
                                    periodEndMillis = monthEnd
                                )

                                BudgetCard(
                                    budget = budget,
                                    status = status,
                                    onDelete = { onDeleteBudget(budget.id) }
                                )
                            }
                        }
                    }
                }
                "GOALS" -> {
                    if (goals.isEmpty()) {
                        EmptyPlanningState(
                            message = "No savings goals created yet.\nTrack vacation funds, emergency reserves, or new purchases!",
                            buttonText = "+ Create Goal",
                            onClick = { showAddGoalDialog = true }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(goals) { goal ->
                                GoalCard(
                                    goal = goal,
                                    onContribute = { contributeGoalTarget = goal },
                                    onDelete = { onDeleteGoal(goal.id) }
                                )
                            }
                        }
                    }
                }
                "BILLS" -> {
                    if (recurring.isEmpty()) {
                        EmptyPlanningState(
                            message = "No recurring bills set up.\nTrack Netflix, rent, utilities, and never miss a payment!",
                            buttonText = "+ Add Bill",
                            onClick = { showAddBillDialog = true }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(recurring) { bill ->
                                BillCard(
                                    bill = bill,
                                    onMarkPaid = { onMarkBillPaid(bill.id) },
                                    onDelete = { onDeleteRecurring(bill.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 1. Add Budget Dialog
    if (showAddBudgetDialog) {
        var budgetName by remember { mutableStateOf("") }
        var limitInput by remember { mutableStateOf("") }
        var selectedCatId by remember { mutableStateOf(categories.firstOrNull { it.type == "EXPENSE" }?.id ?: "") }

        AlertDialog(
            onDismissRequest = { showAddBudgetDialog = false },
            title = { Text("New Monthly Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = budgetName,
                        onValueChange = { budgetName = it },
                        label = { Text("Budget Name (e.g. Groceries)") },
                        modifier = Modifier.fillMaxWidth().testTag("budget_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        label = { Text("Monthly Limit Amount") },
                        modifier = Modifier.fillMaxWidth().testTag("budget_limit_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limitCents = Math.round((limitInput.toDoubleOrNull() ?: 0.0) * 100)
                        if (budgetName.isNotBlank() && limitCents > 0) {
                            onAddBudget(
                                BudgetEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = budgetName,
                                    limitCents = limitCents,
                                    currency = "BDT",
                                    categoryIds = selectedCatId
                                )
                            )
                            showAddBudgetDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_budget_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBudgetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 2. Add Savings Goal Dialog
    if (showAddGoalDialog) {
        var goalName by remember { mutableStateOf("") }
        var targetInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("New Savings Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        label = { Text("Goal Name (e.g. Emergency Fund)") },
                        modifier = Modifier.fillMaxWidth().testTag("goal_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetInput,
                        onValueChange = { targetInput = it },
                        label = { Text("Target Amount") },
                        modifier = Modifier.fillMaxWidth().testTag("goal_target_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetCents = Math.round((targetInput.toDoubleOrNull() ?: 0.0) * 100)
                        if (goalName.isNotBlank() && targetCents > 0) {
                            onAddGoal(
                                GoalEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = goalName,
                                    targetCents = targetCents,
                                    allocatedCents = 0L,
                                    currency = "BDT"
                                )
                            )
                            showAddGoalDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_goal_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 3. Contribute to Goal Dialog
    contributeGoalTarget?.let { goal ->
        AlertDialog(
            onDismissRequest = { contributeGoalTarget = null },
            title = { Text("Contribute to ${goal.name}") },
            text = {
                OutlinedTextField(
                    value = contributeAmountInput,
                    onValueChange = { contributeAmountInput = it },
                    label = { Text("Deposit Amount") },
                    modifier = Modifier.fillMaxWidth().testTag("contribute_amount_input"),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cents = Math.round((contributeAmountInput.toDoubleOrNull() ?: 0.0) * 100)
                        if (cents > 0) {
                            onContributeGoal(goal.id, cents)
                            contributeGoalTarget = null
                            contributeAmountInput = ""
                        }
                    },
                    modifier = Modifier.testTag("confirm_contribute_button")
                ) {
                    Text("Add Deposit")
                }
            },
            dismissButton = {
                TextButton(onClick = { contributeGoalTarget = null }) { Text("Cancel") }
            }
        )
    }

    // 4. Add Recurring Bill Dialog
    if (showAddBillDialog) {
        var billName by remember { mutableStateOf("") }
        var billAmountInput by remember { mutableStateOf("") }
        val defaultAccount = accounts.firstOrNull()?.id ?: ""
        val defaultCategory = categories.firstOrNull { it.type == "EXPENSE" }?.id ?: ""

        AlertDialog(
            onDismissRequest = { showAddBillDialog = false },
            title = { Text("New Recurring Bill") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = billName,
                        onValueChange = { billName = it },
                        label = { Text("Bill Name (e.g. Electricity, WiFi, Rent)") },
                        modifier = Modifier.fillMaxWidth().testTag("bill_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = billAmountInput,
                        onValueChange = { billAmountInput = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth().testTag("bill_amount_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountCents = Math.round((billAmountInput.toDoubleOrNull() ?: 0.0) * 100)
                        if (billName.isNotBlank() && amountCents > 0) {
                            onAddRecurring(
                                RecurringEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = billName,
                                    amountCents = amountCents,
                                    currency = "BDT",
                                    nextDueMillis = System.currentTimeMillis() + 86400000L * 7, // 7 days from now
                                    accountId = defaultAccount,
                                    categoryId = defaultCategory
                                )
                            )
                            showAddBillDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_bill_button")
                ) {
                    Text("Add Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBillDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BudgetCard(
    budget: BudgetEntity,
    status: com.example.domain.service.BudgetStatus,
    onDelete: () -> Unit
) {
    val isOverBudget = status.isExceeded
    val progressColor = if (isOverBudget) Color(0xFFFB7185) else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = budget.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Monthly limit: ${Money(status.limitCents).format()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (status.usagePercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.0f", status.usagePercentage)}% used",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverBudget) Color(0xFFFB7185) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isOverBudget) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = if (isOverBudget) "Over by ${Money(-status.remainingCents).format()}" else "Remaining: ${Money(status.remainingCents).format()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOverBudget) Color(0xFFFB7185) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    onContribute: () -> Unit,
    onDelete: () -> Unit
) {
    val pct = if (goal.targetCents > 0) (goal.allocatedCents.toFloat() / goal.targetCents.toFloat()) * 100f else 0f
    val isCompleted = goal.allocatedCents >= goal.targetCents

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = "Goal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Target: ${Money(goal.targetCents).format()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (pct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved ${Money(goal.allocatedCents).format()} (${String.format(Locale.US, "%.0f", pct)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onContribute,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.testTag("goal_deposit_${goal.id}")
                ) {
                    Text(
                        text = if (isCompleted) "Completed ✓" else "+ Deposit",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BillCard(
    bill: RecurringEntity,
    onMarkPaid: () -> Unit,
    onDelete: () -> Unit
) {
    val status = RecurrenceEngine.determineBillStatus(bill.nextDueMillis, bill.isPaid)
    val statusColor = when (status) {
        BillStatus.PAID -> Color(0xFF34D399)
        BillStatus.OVERDUE -> Color(0xFFFB7185)
        BillStatus.DUE_TODAY -> Color(0xFFFBBF24)
        BillStatus.UPCOMING -> MaterialTheme.colorScheme.primary
    }

    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val dueDateText = dateFormat.format(Date(bill.nextDueMillis))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Due: $dueDateText • ${Money(bill.amountCents).format()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = status.name.replace("_", " "),
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!bill.isPaid) {
                Button(
                    onClick = onMarkPaid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("bill_pay_${bill.id}")
                ) {
                    Text("Pay", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Paid ✓", color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PlanningTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp)
            .testTag("planning_tab_${title.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun EmptyPlanningState(
    message: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(buttonText)
        }
    }
}
