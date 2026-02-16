package com.emicollect.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.emicollect.app.data.local.dao.CustomerDao
import com.emicollect.app.data.local.dao.LoanDao
import com.emicollect.app.data.local.dao.TransactionDao
import com.emicollect.app.data.local.entity.Customer
import com.emicollect.app.data.local.entity.Loan
import com.emicollect.app.data.local.entity.Transaction

@Database(
    entities = [Customer::class, Loan::class, Transaction::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun loanDao(): LoanDao
    abstract fun transactionDao(): TransactionDao
}
