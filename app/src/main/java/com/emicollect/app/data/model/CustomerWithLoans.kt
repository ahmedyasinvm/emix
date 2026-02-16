package com.emicollect.app.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.emicollect.app.data.local.entity.Customer
import com.emicollect.app.data.local.entity.Loan

data class CustomerWithLoans(
    @Embedded val customer: Customer,
    @Relation(
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val loans: List<Loan>
)
