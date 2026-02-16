package com.emicollect.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emicollect.app.data.local.entity.Loan
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)

    @Query("DELETE FROM loans")
    suspend fun deleteAll()

    @Query("SELECT * FROM loans WHERE customerId = :customerId")
    fun getLoansForCustomer(customerId: Long): Flow<List<Loan>>

    @Query("SELECT COUNT(*) FROM loans WHERE isClosed = 0 AND nextDueDate <= :timestamp")
    fun countOverdueLoans(timestamp: Long): Flow<Int>

    @Query("SELECT * FROM loans")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE loanId = :loanId")
    suspend fun getLoanById(loanId: Long): Loan?
}
