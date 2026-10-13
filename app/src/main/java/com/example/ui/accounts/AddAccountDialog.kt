package com.example.ui.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AccountEntity
import com.example.domain.model.AccountType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.BANK) }
    var openingBalanceInput by remember { mutableStateOf("0") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Financial Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name (e.g. Chase Checking)") },
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input"),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AccountType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = openingBalanceInput,
                    onValueChange = { openingBalanceInput = it },
                    label = { Text(if (selectedType.isLiability) "Initial Balance Owed (0 for fresh account)" else "Opening Balance (0 for fresh account)") },
                    modifier = Modifier.fillMaxWidth().testTag("account_opening_balance_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val balanceCents = Math.round((openingBalanceInput.toDoubleOrNull() ?: 0.0) * 100)
                    if (name.isNotBlank()) {
                        val color = when (selectedType) {
                            AccountType.CASH -> "#B7F345"
                            AccountType.BANK -> "#38BDF8"
                            AccountType.CREDIT_CARD -> "#F43F5E"
                            AccountType.SAVINGS -> "#A78BFA"
                            AccountType.E_WALLET -> "#FBBF24"
                            AccountType.INVESTMENT -> "#34D399"
                        }
                        onSave(
                            AccountEntity(
                                id = UUID.randomUUID().toString(),
                                name = name,
                                type = selectedType.name,
                                currency = "BDT",
                                openingBalanceCents = balanceCents,
                                colorHex = color
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("save_account_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
