package com.emicollect.app.data.model

import androidx.room.Embedded
import com.emicollect.app.data.local.entity.Customer

data class CustomerWithDebtStatus(
    @Embedded val customer: Customer,
    val totalRemainingDebt: Double?,
    val earliestNextDueDate: Long?
)
