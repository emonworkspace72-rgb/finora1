package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumericKeypad(
    currentInput: String,
    onInputChanged: (String) -> Unit,
    onCalculateOrDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("1", "2", "3", "+"),
        listOf("4", "5", "6", "-"),
        listOf("7", "8", "9", "*"),
        listOf(".", "0", "DEL", "=")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    KeypadButton(
                        key = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (key) {
                                "DEL" -> {
                                    if (currentInput.isNotEmpty()) {
                                        onInputChanged(currentInput.dropLast(1))
                                    }
                                }
                                "=" -> {
                                    val evaluated = evaluateSimpleExpression(currentInput)
                                    onInputChanged(evaluated)
                                    onCalculateOrDone()
                                }
                                "+", "-", "*", "/" -> {
                                    if (currentInput.isNotEmpty() && !isOperator(currentInput.last())) {
                                        onInputChanged(currentInput + key)
                                    }
                                }
                                "." -> {
                                    val lastToken = currentInput.split('+', '-', '*', '/').lastOrNull() ?: ""
                                    if (!lastToken.contains(".")) {
                                        onInputChanged(currentInput + ".")
                                    }
                                }
                                else -> {
                                    // Digit
                                    if (currentInput == "0") {
                                        onInputChanged(key)
                                    } else {
                                        onInputChanged(currentInput + key)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOperator = key in listOf("+", "-", "*", "/", "=")
    val isAction = key == "="

    val backgroundColor = when {
        isAction -> MaterialTheme.colorScheme.primary
        isOperator -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isAction -> MaterialTheme.colorScheme.onPrimary
        isOperator -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .testTag("keypad_key_$key"),
        contentAlignment = Alignment.Center
    ) {
        if (key == "DEL") {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = contentColor
            )
        } else {
            Text(
                text = key,
                color = contentColor,
                fontSize = if (isOperator) 22.sp else 20.sp,
                fontWeight = if (isOperator || isAction) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private fun isOperator(c: Char): Boolean = c == '+' || c == '-' || c == '*' || c == '/'

private fun evaluateSimpleExpression(expr: String): String {
    if (expr.isBlank()) return "0"
    return try {
        // Simple 2-operand or sequential parser
        val tokens = mutableListOf<String>()
        var current = StringBuilder()
        for (ch in expr) {
            if (isOperator(ch)) {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current = StringBuilder()
                }
                tokens.add(ch.toString())
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) tokens.add(current.toString())

        if (tokens.isEmpty()) return expr

        var result = tokens[0].toDoubleOrNull() ?: 0.0
        var i = 1
        while (i < tokens.size - 1) {
            val op = tokens[i]
            val nextVal = tokens[i + 1].toDoubleOrNull() ?: 0.0
            result = when (op) {
                "+" -> result + nextVal
                "-" -> result - nextVal
                "*" -> result * nextVal
                "/" -> if (nextVal != 0.0) result / nextVal else result
                else -> result
            }
            i += 2
        }
        val formatted = if (result % 1.0 == 0.0) {
            result.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", result)
        }
        formatted
    } catch (e: Exception) {
        expr
    }
}
