package com.example.domain.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Currency information and precision registry.
 */
data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String,
    val decimals: Int = 2
)

object CurrencyRegistry {
    val SUPPORTED_CURRENCIES = listOf(
        CurrencyInfo("BDT", "৳", "Bangladeshi Taka", 2),
        CurrencyInfo("USD", "$", "US Dollar", 2),
        CurrencyInfo("EUR", "€", "Euro", 2),
        CurrencyInfo("GBP", "£", "British Pound", 2),
        CurrencyInfo("JPY", "¥", "Japanese Yen", 0),
        CurrencyInfo("CAD", "CA$", "Canadian Dollar", 2),
        CurrencyInfo("AUD", "AU$", "Australian Dollar", 2),
        CurrencyInfo("CHF", "CHF", "Swiss Franc", 2),
        CurrencyInfo("CNY", "¥", "Chinese Yuan", 2),
        CurrencyInfo("INR", "₹", "Indian Rupee", 2),
        CurrencyInfo("BRL", "R$", "Brazilian Real", 2)
    )

    fun getCurrency(code: String): CurrencyInfo {
        return SUPPORTED_CURRENCIES.find { it.code.equals(code, ignoreCase = true) }
            ?: CurrencyInfo(code.uppercase(), code.uppercase(), code.uppercase(), 2)
    }
}

/**
 * Exact monetary representation storing minor units (cents) as a 64-bit Long.
 * Completely avoids binary floating-point representation drift.
 */
data class Money(
    val cents: Long,
    val currencyCode: String = "BDT"
) : Comparable<Money> {

    operator fun plus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot add distinct currencies: $currencyCode and ${other.currencyCode}" }
        return Money(cents + other.cents, currencyCode)
    }

    operator fun minus(other: Money): Money {
        require(currencyCode == other.currencyCode) { "Cannot subtract distinct currencies: $currencyCode and ${other.currencyCode}" }
        return Money(cents - other.cents, currencyCode)
    }

    operator fun times(factor: Long): Money = Money(cents * factor, currencyCode)

    operator fun unaryMinus(): Money = Money(-cents, currencyCode)

    override fun compareTo(other: Money): Int {
        require(currencyCode == other.currencyCode) { "Cannot compare distinct currencies: $currencyCode and ${other.currencyCode}" }
        return cents.compareTo(other.cents)
    }

    fun format(includeSymbol: Boolean = true, includeSign: Boolean = false): String {
        val currency = CurrencyRegistry.getCurrency(currencyCode)
        val isNegative = cents < 0
        val absCents = Math.abs(cents)

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        val formattedNumber = if (currency.decimals == 0) {
            val formatter = DecimalFormat("#,##0", symbols)
            formatter.format(absCents)
        } else {
            val divisor = Math.pow(10.0, currency.decimals.toDouble())
            val doubleVal = absCents.toDouble() / divisor
            val pattern = "#,##0." + "0".repeat(currency.decimals)
            val formatter = DecimalFormat(pattern, symbols)
            formatter.format(doubleVal)
        }

        val signPrefix = when {
            isNegative -> "-"
            includeSign && cents > 0 -> "+"
            else -> ""
        }

        return if (includeSymbol) {
            "$signPrefix${currency.symbol}$formattedNumber"
        } else {
            "$signPrefix$formattedNumber"
        }
    }

    companion object {
        fun zero(currencyCode: String = "BDT"): Money = Money(0L, currencyCode)

        fun fromMajor(amount: Double, currencyCode: String = "BDT"): Money {
            val currency = CurrencyRegistry.getCurrency(currencyCode)
            val multiplier = Math.pow(10.0, currency.decimals.toDouble())
            val cents = Math.round(amount * multiplier)
            return Money(cents, currencyCode)
        }
    }
}
