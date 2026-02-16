package com.emicollect.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emicollect.app.data.local.entity.Customer
import com.emicollect.app.data.model.CustomerWithDebtStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("""
        SELECT c.*, 
               SUM(l.currentBalance) as totalRemainingDebt, 
               MIN(l.nextDueDate) as earliestNextDueDate
        FROM customers c
        LEFT JOIN loans l ON c.id = l.customerId AND l.isClosed = 0
        GROUP BY c.id
        ORDER BY earliestNextDueDate ASC, totalRemainingDebt DESC
    """)
    fun getCustomersSortedByUrgency(): Flow<List<CustomerWithDebtStatus>>

    @Query("""
        SELECT c.*, 
               COALESCE(SUM(l.currentBalance), 0.0) as totalRemainingDebt, 
               MIN(l.nextDueDate) as earliestNextDueDate
        FROM customers c
        LEFT JOIN loans l ON c.id = l.customerId AND l.isClosed = 0
        GROUP BY c.id
        ORDER BY totalRemainingDebt DESC
    """)
    fun getCustomersSortedByDebt(): Flow<List<CustomerWithDebtStatus>>

    @Query("""
        SELECT c.*, 
               SUM(l.currentBalance) as totalRemainingDebt, 
               MIN(l.nextDueDate) as earliestNextDueDate
        FROM customers c
        LEFT JOIN loans l ON c.id = l.customerId AND l.isClosed = 0
        GROUP BY c.id
        ORDER BY c.name ASC
    """)
    fun getCustomersSortedByName(): Flow<List<CustomerWithDebtStatus>>

    @Query("""
        SELECT c.*, 
               COALESCE(SUM(l.currentBalance), 0.0) as totalRemainingDebt, 
               MIN(l.nextDueDate) as earliestNextDueDate
        FROM customers c
        LEFT JOIN loans l ON c.id = l.customerId AND l.isClosed = 0
        WHERE c.name LIKE '%' || :query || '%' OR c.phone LIKE '%' || :query || '%'
        GROUP BY c.id
        ORDER BY c.name ASC
    """)
    fun searchCustomers(query: String): Flow<List<CustomerWithDebtStatus>>

    @androidx.room.Transaction
    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerWithLoans(id: Long): Flow<com.emicollect.app.data.model.CustomerWithLoans>
}
