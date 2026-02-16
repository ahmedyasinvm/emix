package com.emicollect.app.data.model

import com.emicollect.app.data.local.entity.Customer
import com.emicollect.app.data.local.entity.Loan
import com.emicollect.app.data.local.entity.Transaction
import com.google.gson.annotations.SerializedName

data class BackupData(
    @SerializedName("customers") val customers: List<Customer>,
    @SerializedName("loans") val loans: List<Loan>,
    @SerializedName("transactions") val transactions: List<Transaction>,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)
