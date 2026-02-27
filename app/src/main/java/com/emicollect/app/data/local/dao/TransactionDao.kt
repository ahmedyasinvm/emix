package com.emicollect.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emicollect.app.data.local.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @androidx.room.Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("SELECT * FROM transactions WHERE loanId = :loanId ORDER BY datePaid DESC")
    fun getTransactionsForLoan(loanId: Long): Flow<List<Transaction>>

    @Query("SELECT SUM(amountPaid) FROM transactions WHERE datePaid BETWEEN :start AND :end")
    fun getTotalCollectionBetween(start: Long, end: Long): Flow<Double?>

    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT SUM(amountPaid) FROM transactions")
    fun getTotalCollection(): Flow<Double?>

    @Query("SELECT t.* FROM transactions t INNER JOIN loans l ON t.loanId = l.loanId WHERE l.customerId = :customerId ORDER BY t.datePaid DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>>
}
