package com.example.ui.quickadd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.Money
import com.example.domain.model.TransactionType
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.NumericKeypad

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    initialType: TransactionType,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    preselectedCategory: CategoryEntity? = null,
    onDismiss: () -> Unit,
    onSave: (type: TransactionType, amountCents: Long, accountId: String, categoryId: String, note: String, merchant: String) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var transactionType by remember { mutableStateOf(initialType) }
    var inputAmount by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(preselectedCategory ?: categories.firstOrNull { it.type == transactionType.name } ?: categories.firstOrNull())
    }
    var selectedAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var note by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var showExtraFields by remember { mutableStateOf(false) }

    val filteredCategories = categories.filter { it.type == transactionType.name }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: Type toggle & close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TypeToggleButton(
                        text = "Expense",
                        isSelected = transactionType == TransactionType.EXPENSE,
                        selectedColor = Color(0xFFFB7185),
                        onClick = {
                            transactionType = TransactionType.EXPENSE
                            selectedCategory = categories.firstOrNull { it.type == "EXPENSE" }
                        }
                    )
                    TypeToggleButton(
                        text = "Income",
                        isSelected = transactionType == TransactionType.INCOME,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            transactionType = TransactionType.INCOME
                            selectedCategory = categories.firstOrNull { it.type == "INCOME" }
                        }
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.testTag("quickadd_close_btn")) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Large Display Amount
            val displayValue = if (inputAmount.isBlank()) "0.00" else inputAmount
            val amountColor = if (transactionType == TransactionType.EXPENSE) {
                Color(0xFFFB7185)
            } else {
                MaterialTheme.colorScheme.primary
            }

            val currencyCode = selectedAccount?.currency ?: "BDT"
            val currencySymbol = com.example.domain.model.CurrencyRegistry.getCurrency(currencyCode).symbol

            Text(
                text = "${if (transactionType == TransactionType.EXPENSE) "-" else "+"} $currencySymbol $displayValue",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    color = amountColor
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quickadd_amount_display")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Account selection chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts) { acc ->
                    val isSelected = acc.id == selectedAccount?.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedAccount = acc }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("account_chip_${acc.id}")
                    ) {
                        Text(
                            text = acc.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category selection chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCategories) { cat ->
                    val isSelected = cat.id == selectedCategory?.id
                    val catColor = try {
                        Color(android.graphics.Color.parseColor(cat.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) catColor.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) catColor else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("category_chip_${cat.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = CategoryIconHelper.getIcon(cat.iconName),
                            contentDescription = cat.name,
                            tint = catColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Optional note toggle
            if (showExtraFields) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Description") },
                    modifier = Modifier.fillMaxWidth().testTag("quickadd_note_input"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Payee / Merchant (Optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("quickadd_merchant_input"),
                    singleLine = true
                )
            } else {
                Text(
                    text = "+ Add Note / Merchant",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable { showExtraFields = true }
                )
            }

            // Calculator Keypad
            NumericKeypad(
                currentInput = inputAmount,
                onInputChanged = { inputAmount = it },
                onCalculateOrDone = {
                    // Trigger save if valid
                }
            )

            // Primary Save Action
            val parsedCents = try {
                val cleaned = inputAmount.replace(",", "")
                val doubleVal = cleaned.toDoubleOrNull() ?: 0.0
                (doubleVal * 100).toLong()
            } catch (e: Exception) {
                0L
            }

            Button(
                onClick = {
                    val acc = selectedAccount ?: accounts.firstOrNull() ?: return@Button
                    val cat = selectedCategory ?: categories.firstOrNull() ?: return@Button
                    if (parsedCents > 0) {
                        onSave(transactionType, parsedCents, acc.id, cat.id, note, merchant)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("quickadd_save_button"),
                enabled = parsedCents > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Save ${transactionType.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun TypeToggleButton(
    text: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) selectedColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("toggle_${text.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}
