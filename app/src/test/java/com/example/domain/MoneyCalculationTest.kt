package com.example.domain

import com.example.domain.model.AccountType
import com.example.domain.model.CarryOverPolicy
import com.example.domain.model.Money
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.TransactionType
import com.example.domain.service.BalanceCalculator
import com.example.domain.service.BudgetCalculator
import com.example.domain.service.RecurrenceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MoneyCalculationTest {

    @Test
    fun testMoneyArithmeticAndFormatting() {
        val m1 = Money(15000L, "USD") // $150.00
        val m2 = Money(2550L, "USD")  // $25.50

        val sum = m1 + m2
        assertEquals(17550L, sum.cents)
        assertEquals("$175.50", sum.format())

        val diff = m1 - m2
        assertEquals(12450L, diff.cents)
        assertEquals("$124.50", diff.format())

        val jpy = Money(1000L, "JPY")
        assertEquals("¥1,000", jpy.format())

        val bdt = Money(150000L, "BDT") // ৳1,500.00
        assertEquals("৳1,500.00", bdt.format())
        assertEquals("৳0.00", Money.zero("BDT").format())
    }

    @Test
    fun testUserInputParsingToCents() {
        val inputString = "200"
        val parsedCents = Math.round((inputString.toDoubleOrNull() ?: 0.0) * 100)
        assertEquals(20000L, parsedCents)
        val money = Money(parsedCents, "BDT")
        assertEquals("৳200.00", money.format())
    }

    @Test
    fun testCashAccountBalanceCalculations() {
        val cashAccount = BalanceCalculator.AccountRecord(
            id = "acc_cash",
            name = "Cash",
            type = AccountType.CASH,
            currency = "USD",
            openingBalanceCents = 10000L // $100.00
        )

        val transactions = listOf(
            BalanceCalculator.TransactionRecord(
                id = "tx1",
                type = TransactionType.INCOME,
                amountCents = 5000L, // +$50.00
                accountId = "acc_cash"
            ),
            BalanceCalculator.TransactionRecord(
                id = "tx2",
                type = TransactionType.EXPENSE,
                amountCents = 3000L, // -$30.00
                accountId = "acc_cash"
            )
        )

        val balance = BalanceCalculator.calculateAccountBalance(cashAccount, transactions)
        // 100 + 50 - 30 = 120
        assertEquals(12000L, balance)
    }

    @Test
    fun testCreditCardLiabilityCalculations() {
        val creditCard = BalanceCalculator.AccountRecord(
            id = "acc_visa",
            name = "Visa",
            type = AccountType.CREDIT_CARD,
            currency = "USD",
            openingBalanceCents = 2000L // $20.00 owed initially
        )

        val transactions = listOf(
            // Purchase of $50.00 increases debt
            BalanceCalculator.TransactionRecord(
                id = "tx1",
                type = TransactionType.EXPENSE,
                amountCents = 5000L,
                accountId = "acc_visa"
            ),
            // Payment transfer from checking of $40.00 reduces debt
            BalanceCalculator.TransactionRecord(
                id = "tx2",
                type = TransactionType.TRANSFER,
                amountCents = 4000L,
                accountId = "acc_checking",
                destinationAccountId = "acc_visa"
            )
        )

        val owed = BalanceCalculator.calculateAccountBalance(creditCard, transactions)
        // 20 + 50 - 40 = 30
        assertEquals(3000L, owed)
    }

    @Test
    fun testInternalTransferNeutralityOnNetWorth() {
        val checking = BalanceCalculator.AccountRecord(
            id = "acc_checking",
            name = "Checking",
            type = AccountType.BANK,
            currency = "USD",
            openingBalanceCents = 50000L // $500.00
        )
        val savings = BalanceCalculator.AccountRecord(
            id = "acc_savings",
            name = "Savings",
            type = AccountType.SAVINGS,
            currency = "USD",
            openingBalanceCents = 100000L // $1,000.00
        )

        // Internal transfer of $200 from checking to savings
        val transferTx = listOf(
            BalanceCalculator.TransactionRecord(
                id = "tx_transfer",
                type = TransactionType.TRANSFER,
                amountCents = 20000L,
                accountId = "acc_checking",
                destinationAccountId = "acc_savings"
            )
        )

        val checkingBal = BalanceCalculator.calculateAccountBalance(checking, transferTx)
        val savingsBal = BalanceCalculator.calculateAccountBalance(savings, transferTx)

        assertEquals(30000L, checkingBal) // $300.00
        assertEquals(120000L, savingsBal) // $1,200.00

        val netWorth = BalanceCalculator.calculateNetWorth(listOf(checking, savings), transferTx)
        // Total should remain unchanged: 500 + 1000 = 1500
        assertEquals(150000L, netWorth.netWorthCents)
    }

    @Test
    fun testBudgetUsageAndCarryOver() {
        val now = 1000000L
        val txs = listOf(
            BudgetCalculator.BudgetTransaction(
                categoryId = "cat_groceries",
                type = TransactionType.EXPENSE,
                amountCents = 6000L, // $60.00
                dateMillis = now
            ),
            BudgetCalculator.BudgetTransaction(
                categoryId = "cat_dining",
                type = TransactionType.EXPENSE,
                amountCents = 2500L, // $25.00
                dateMillis = now
            )
        )

        val status = BudgetCalculator.calculateBudgetStatus(
            budgetId = "b1",
            name = "Groceries",
            limitCents = 10000L, // $100.00 limit
            categoryIds = setOf("cat_groceries"),
            transactions = txs,
            periodStartMillis = 0L,
            periodEndMillis = 2000000L,
            carryOverPolicy = CarryOverPolicy.NONE
        )

        assertEquals(6000L, status.spentCents)
        assertEquals(4000L, status.remainingCents)
        assertEquals(60.0f, status.usagePercentage, 0.01f)
        assertFalse(status.isExceeded)
    }

    @Test
    fun testRecurrenceMonthEndSafety() {
        val timeZone = TimeZone.getTimeZone("UTC")
        val cal = Calendar.getInstance(timeZone).apply {
            set(2026, Calendar.JANUARY, 31, 12, 0, 0)
        }

        // Monthly recurrence from Jan 31 -> Feb 28
        val nextDue = RecurrenceEngine.calculateNextDueDate(cal.timeInMillis, RecurrenceFrequency.MONTHLY, timeZone)
        val nextCal = Calendar.getInstance(timeZone).apply { timeInMillis = nextDue }

        assertEquals(Calendar.FEBRUARY, nextCal.get(Calendar.MONTH))
        assertEquals(28, nextCal.get(Calendar.DAY_OF_MONTH))
    }
}
