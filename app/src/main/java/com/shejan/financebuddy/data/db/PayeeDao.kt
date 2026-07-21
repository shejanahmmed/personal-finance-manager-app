package com.shejan.financebuddy.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PayeeDao {
    @Query("SELECT * FROM payees ORDER BY name ASC")
    fun getAllPayees(): Flow<List<PayeeEntity>>

    @Query("SELECT * FROM payees ORDER BY name ASC")
    suspend fun getAllPayeesOnce(): List<PayeeEntity>

    @Query("SELECT * FROM payees WHERE id = :id")
    suspend fun getPayeeById(id: Int): PayeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayee(payee: PayeeEntity): Long

    @Update
    suspend fun updatePayee(payee: PayeeEntity)

    @Delete
    suspend fun deletePayee(payee: PayeeEntity)

    @Query("SELECT * FROM payee_accounts WHERE payeeId = :payeeId")
    fun getAccountsForPayee(payeeId: Int): Flow<List<PayeeAccountEntity>>

    @Query("SELECT * FROM payee_accounts")
    fun getAllPayeeAccounts(): Flow<List<PayeeAccountEntity>>

    @Query("SELECT * FROM payee_accounts WHERE payeeId = :payeeId")
    suspend fun getAccountsForPayeeOnce(payeeId: Int): List<PayeeAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayeeAccount(account: PayeeAccountEntity): Long

    @Update
    suspend fun updatePayeeAccount(account: PayeeAccountEntity)

    @Delete
    suspend fun deletePayeeAccount(account: PayeeAccountEntity)

    @Query("DELETE FROM payee_accounts")
    suspend fun deleteAllPayeeAccounts()

    @Query("DELETE FROM payees")
    suspend fun deleteAllPayees()
}
