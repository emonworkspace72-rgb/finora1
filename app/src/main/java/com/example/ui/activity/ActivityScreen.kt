package com.example.ui.activity

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.model.Money
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryIconHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    onDeleteTransaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, EXPENSE, INCOME, TRANSFER
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Filter transactions
    val filtered = transactions.filter { tx ->
        val matchesType = when (selectedFilter) {
            "EXPENSE" -> tx.type == TransactionType.EXPENSE.name
            "INCOME" -> tx.type == TransactionType.INCOME.name
            "TRANSFER" -> tx.type == TransactionType.TRANSFER.name
            else -> true
        }

        val category = categories.find { it.id == tx.categoryId }
        val matchesSearch = if (searchQuery.isBlank()) true else {
            tx.note.contains(searchQuery, ignoreCase = true) ||
                    tx.merchant.contains(searchQuery, ignoreCase = true) ||
                    (category?.name?.contains(searchQuery, ignoreCase = true) ?: false)
        }

        matchesType && matchesSearch
    }

    // Group by Date formatted string
    val dateFormat = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
    val todayFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val todayCode = todayFormat.format(Date())

    val groupedTransactions = remember(filtered) {
        filtered.groupBy { tx ->
            val txCode = todayFormat.format(Date(tx.dateMillis))
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val yestCode = todayFormat.format(cal.time)

            when (txCode) {
                todayCode -> "Today"
                yestCode -> "Yesterday"
                else -> dateFormat.format(Date(tx.dateMillis))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("activity_screen")
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by payee, category, or note...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("activity_search_field"),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Pills
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filters = listOf(
                "ALL" to "All",
                "EXPENSE" to "Expenses",
                "INCOME" to "Income",
                "TRANSFER" to "Transfers"
            )
            items(filters) { (key, label) ->
                val isSelected = selectedFilter == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { selectedFilter = key }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("filter_$key")
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Transaction List grouped by date
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "No transactions matching '$searchQuery'" else "No transactions in this filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groupedTransactions.forEach { (dateGroup, list) ->
                    item {
                        Text(
                            text = dateGroup,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(list) { tx ->
                        val category = categories.find { it.id == tx.categoryId }
                        val account = accounts.find { it.id == tx.accountId }
                        TransactionRowItem(
                            tx = tx,
                            category = category,
                            account = account,
                            onClick = { selectedTransaction = tx }
                        )
                    }
                }
            }
        }
    }

    // Transaction Details Sheet
    selectedTransaction?.let { tx ->
        val category = categories.find { it.id == tx.categoryId }
        val account = accounts.find { it.id == tx.accountId }
        val destAccount = tx.destinationAccountId?.let { destId -> accounts.find { it.id == destId } }

        ModalBottomSheet(
            onDismissRequest = { selectedTransaction = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Details",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("detail_delete_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFB7185)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DetailItem(label = "Amount", value = Money(tx.amountCents).format())
                DetailItem(label = "Type", value = tx.type)
                DetailItem(label = "Category", value = category?.name ?: "N/A")
                DetailItem(label = "Account", value = account?.name ?: "N/A")
                if (destAccount != null) {
                    DetailItem(label = "Transferred To", value = destAccount.name)
                }
                if (tx.merchant.isNotBlank()) {
                    DetailItem(label = "Payee / Merchant", value = tx.merchant)
                }
                if (tx.note.isNotBlank()) {
                    DetailItem(label = "Note", value = tx.note)
                }
                DetailItem(
                    label = "Date & Time",
                    value = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault()).format(Date(tx.dateMillis))
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { selectedTransaction = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && selectedTransaction != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction? All associated account balances will be updated.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val txId = selectedTransaction?.id
                        if (txId != null) {
                            onDeleteTransaction(txId)
                        }
                        showDeleteConfirmDialog = false
                        selectedTransaction = null
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", color = Color(0xFFFB7185))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TransactionRowItem(
    tx: TransactionEntity,
    category: CategoryEntity?,
    account: AccountEntity?,
    onClick: () -> Unit
) {
    val isExpense = tx.type == TransactionType.EXPENSE.name
    val isIncome = tx.type == TransactionType.INCOME.name

    val categoryColor = category?.colorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { null }
    } ?: MaterialTheme.colorScheme.primary

    val amountColor = when {
        isExpense -> Color(0xFFFB7185)
        isIncome -> Color(0xFF34D399)
        else -> Color(0xFF38BDF8)
    }

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(tx.dateMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("activity_tx_${tx.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconHelper.getIcon(category?.iconName ?: "Category"),
                    contentDescription = category?.name,
                    tint = categoryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (tx.merchant.isNotBlank()) tx.merchant else category?.name ?: "Transaction",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${account?.name ?: "Account"} • $formattedTime" + if (tx.note.isNotBlank()) " • ${tx.note}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Text(
                text = (if (isExpense) "- " else if (isIncome) "+ " else "") + Money(tx.amountCents).format(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            )
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
    }
}
