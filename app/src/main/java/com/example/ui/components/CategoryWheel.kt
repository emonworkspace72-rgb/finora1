package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.Money
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

data class CategorySpendItem(
    val category: CategoryEntity,
    val spendCents: Long,
    val percentage: Float
)

@Composable
fun CategoryWheel(
    categories: List<CategorySpendItem>,
    centerBalanceText: String,
    centerSubtext: String,
    onCategoryClick: (CategoryEntity) -> Unit,
    onPlusClick: () -> Unit,
    onMinusClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp),
        contentAlignment = Alignment.Center
    ) {
        val width = maxWidth.value
        val height = maxHeight.value
        val minDim = min(width, height)
        val ringRadius = (minDim / 2f) * 0.72f
        val donutRadius = (minDim / 2f) * 0.44f

        // 1. Draw central Donut Chart slices
        Canvas(modifier = Modifier.size((donutRadius * 2).dp)) {
            val strokeWidth = 24.dp.toPx()
            val canvasSize = size.minDimension
            val arcSize = Size(canvasSize - strokeWidth, canvasSize - strokeWidth)
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            if (categories.isEmpty() || categories.all { it.spendCents == 0L }) {
                // Empty state ring
                drawArc(
                    color = Color.DarkGray.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                var currentAngle = -90f
                for (item in categories) {
                    val sweep = (item.percentage / 100f) * 360f
                    if (sweep > 0.5f) {
                        val color = try {
                            Color(android.graphics.Color.parseColor(item.category.colorHex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                        drawArc(
                            color = color,
                            startAngle = currentAngle,
                            sweepAngle = sweep - 2f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        currentAngle += sweep
                    }
                }
            }
        }

        // 2. Center Text (Balance & Period)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(
                text = centerSubtext,
                style = MaterialTheme.colorScheme.onSurfaceVariant.let {
                    MaterialTheme.typography.bodySmall.copy(color = it, fontSize = 11.sp)
                }
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = centerBalanceText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )
        }

        // 3. Category Nodes positioned in a circle around the donut ring
        val displayCategories = categories.take(10) // Display top 10 around wheel
        val totalCount = displayCategories.size
        if (totalCount > 0) {
            displayCategories.forEachIndexed { index, item ->
                val angleRad = Math.toRadians((index * (360.0 / totalCount) - 90.0))
                val xOffset = (ringRadius * cos(angleRad)).toInt()
                val yOffset = (ringRadius * sin(angleRad)).toInt()

                val categoryColor = try {
                    Color(android.graphics.Color.parseColor(item.category.colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(xOffset.dp.roundToPx(), yOffset.dp.roundToPx()) }
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, categoryColor, CircleShape)
                        .clickable { onCategoryClick(item.category) }
                        .testTag("wheel_cat_${item.category.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryIconHelper.getIcon(item.category.iconName),
                        contentDescription = item.category.name,
                        tint = categoryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    // 4. Quick Minus & Plus Action Buttons (Monefy style)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Minus Button (Expense)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFFB7185).copy(alpha = 0.15f))
                .border(2.dp, Color(0xFFFB7185), CircleShape)
                .clickable(onClick = onMinusClick)
                .testTag("wheel_minus_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Quick Expense",
                tint = Color(0xFFFB7185),
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "Tap category or +/-",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Plus Button (Income)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable(onClick = onPlusClick)
                .testTag("wheel_plus_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Quick Income",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
