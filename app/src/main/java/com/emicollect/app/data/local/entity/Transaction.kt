package com.emicollect.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType {
    PAYMENT, DOWN_PAYMENT, LOAN_CREATED
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Loan::class,
            parentColumns = ["loanId"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["loanId"])]
)
data class Transaction(
    @PrimaryKey
    val transactionId: String = UUID.randomUUID().toString(),
    val loanId: Long,
    val amountPaid: Double,
    val paymentMode: String = "Cash",
    val datePaid: Long = System.currentTimeMillis(),
    val type: TransactionType = TransactionType.PAYMENT
)
