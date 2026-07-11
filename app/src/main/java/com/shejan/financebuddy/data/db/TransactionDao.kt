package com.shejan.financebuddy.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TransactionDao {
    @Insert
    abstract suspend fun insertRawTransaction(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    abstract fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    abstract fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND timestamp >= :start")
    abstract fun getMonthlyExpenses(start: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND timestamp >= :start")
    abstract fun getMonthlyIncome(start: Long): Flow<Double?>

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :id")
    abstract suspend fun adjustAccountBalance(id: Int, amount: Double)

    @Delete
    abstract suspend fun deleteRawTransaction(transaction: TransactionEntity)

    @Transaction
    open suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = insertRawTransaction(transaction)
        when (transaction.type) {
            "INCOME" -> {
                adjustAccountBalance(transaction.fromAccountId, transaction.amount)
            }
            "EXPENSE" -> {
                adjustAccountBalance(transaction.fromAccountId, -transaction.amount)
            }
            "TRANSFER" -> {
                adjustAccountBalance(transaction.fromAccountId, -transaction.amount)
                transaction.toAccountId?.let { toId ->
                    adjustAccountBalance(toId, transaction.amount)
                }
            }
        }
        return id
    }

    @Transaction
    open suspend fun deleteTransaction(transaction: TransactionEntity) {
        deleteRawTransaction(transaction)
        // Reverse the balance adjustment
        when (transaction.type) {
            "INCOME" -> {
                adjustAccountBalance(transaction.fromAccountId, -transaction.amount)
            }
            "EXPENSE" -> {
                adjustAccountBalance(transaction.fromAccountId, transaction.amount)
            }
            "TRANSFER" -> {
                adjustAccountBalance(transaction.fromAccountId, transaction.amount)
                transaction.toAccountId?.let { toId ->
                    adjustAccountBalance(toId, -transaction.amount)
                }
            }
        }
    }
}
