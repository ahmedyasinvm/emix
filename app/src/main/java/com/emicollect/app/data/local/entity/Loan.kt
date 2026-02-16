package com.emicollect.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EmiFrequency {
    WEEKLY, MONTHLY
}

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class Loan(
    @PrimaryKey(autoGenerate = true)
    val loanId: Long = 0,
    val customerId: Long,
    val itemName: String,
    val totalPrincipal: Double,
    val downPayment: Double,
    val currentBalance: Double,
    val emiFrequency: EmiFrequency,
    val nextDueDate: Long,
    val isClosed: Boolean = false
)
