package com.example.domain.service

import com.example.domain.model.BillStatus
import com.example.domain.model.RecurrenceFrequency
import java.util.Calendar
import java.util.TimeZone

object RecurrenceEngine {

    fun calculateNextDueDate(
        currentDueDateMillis: Long,
        frequency: RecurrenceFrequency,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = currentDueDateMillis
        }

        when (frequency) {
            RecurrenceFrequency.DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            RecurrenceFrequency.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            RecurrenceFrequency.BI_WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 2)
            }
            RecurrenceFrequency.MONTHLY -> {
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                calendar.add(Calendar.MONTH, 1)
                val maxDayInNewMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                if (dayOfMonth > maxDayInNewMonth) {
                    calendar.set(Calendar.DAY_OF_MONTH, maxDayInNewMonth)
                }
            }
            RecurrenceFrequency.YEARLY -> {
                calendar.add(Calendar.YEAR, 1)
            }
        }

        return calendar.timeInMillis
    }

    fun determineBillStatus(
        dueMillis: Long,
        isPaid: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): BillStatus {
        if (isPaid) return BillStatus.PAID

        val calDue = Calendar.getInstance().apply { timeInMillis = dueMillis }
        val calNow = Calendar.getInstance().apply { timeInMillis = nowMillis }

        val isSameDay = calDue.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calDue.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

        return when {
            isSameDay -> BillStatus.DUE_TODAY
            dueMillis < nowMillis -> BillStatus.OVERDUE
            else -> BillStatus.UPCOMING
        }
    }
}
