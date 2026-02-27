package com.emicollect.app.utils

import com.emicollect.app.data.local.CollectionSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Date

object DueCalculator {

    fun isOverdue(lastPaymentDate: Long?, customer: com.emicollect.app.data.local.entity.Customer): Boolean {
        val today = LocalDate.now()
        val dueDate = try {
            when (customer.frequency) {
                "Weekly" -> calculateWeeklyDueDate(today, customer.collectionDay)
                "Monthly" -> calculateMonthlyDueDate(today, customer.collectionWeek, customer.collectionDay)
                else -> return false
            }
        } catch (e: Exception) {
            return false
        }

        if (lastPaymentDate == null) return true
        
        val lastPaymentLocalDate = LocalDate.ofInstant(
            Date(lastPaymentDate).toInstant(), 
            ZoneId.systemDefault()
        )
        
        return lastPaymentLocalDate.isBefore(dueDate)
    }

    private fun calculateWeeklyDueDate(today: LocalDate, dayIdx: Int): LocalDate {
        // User: 1=Sun, 2=Mon ... 7=Sat
        // DayOfWeek: 1=Mon ... 7=Sun
        val targetDay = when (dayIdx) {
            1 -> DayOfWeek.SUNDAY
            2 -> DayOfWeek.MONDAY
            3 -> DayOfWeek.TUESDAY
            4 -> DayOfWeek.WEDNESDAY
            5 -> DayOfWeek.THURSDAY
            6 -> DayOfWeek.FRIDAY
            7 -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }
        
        // Find most recent targetDay (including today)
        return if (today.dayOfWeek == targetDay) {
            today
        } else {
            today.with(TemporalAdjusters.previous(targetDay))
        }
    }

    private fun calculateMonthlyDueDate(today: LocalDate, weekNum: Int, dayIdx: Int): LocalDate {
        val targetDay = when (dayIdx) {
            1 -> DayOfWeek.SUNDAY
            2 -> DayOfWeek.MONDAY
            3 -> DayOfWeek.TUESDAY
            4 -> DayOfWeek.WEDNESDAY
            5 -> DayOfWeek.THURSDAY
            6 -> DayOfWeek.FRIDAY
            7 -> DayOfWeek.SATURDAY
            else -> DayOfWeek.SUNDAY
        }

        // 1st, 2nd, 3rd, 4th occurrence
        val currentMonthInstance = today.with(TemporalAdjusters.dayOfWeekInMonth(weekNum, targetDay))
        
        return if (today.isBefore(currentMonthInstance)) {
            // If today is before this month's due date, look at last month
            today.minusMonths(1).with(TemporalAdjusters.dayOfWeekInMonth(weekNum, targetDay))
        } else {
            currentMonthInstance
        }
    }
}
