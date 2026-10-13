package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    fun getIcon(name: String): ImageVector {
        return when (name) {
            "ShoppingBag" -> Icons.Default.ShoppingBag
            "Restaurant" -> Icons.Default.Restaurant
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "Home" -> Icons.Default.Home
            "Bolt" -> Icons.Default.Bolt
            "Checkroom" -> Icons.Default.Checkroom
            "Movie" -> Icons.Default.Movie
            "LocalHospital" -> Icons.Default.LocalHospital
            "Flight" -> Icons.Default.Flight
            "AccountBalanceWallet" -> Icons.Default.AccountBalanceWallet
            "Laptop" -> Icons.Default.Laptop
            "TrendingUp" -> Icons.Default.TrendingUp
            "CardGiftcard" -> Icons.Default.CardGiftcard
            "Payments" -> Icons.Default.Payments
            "AccountBalance" -> Icons.Default.AccountBalance
            "CreditCard" -> Icons.Default.CreditCard
            "SwapHoriz" -> Icons.Default.SwapHoriz
            else -> Icons.Default.Category
        }
    }
}
