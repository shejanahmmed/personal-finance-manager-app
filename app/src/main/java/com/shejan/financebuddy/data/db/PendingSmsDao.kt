package com.shejan.financebuddy.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSmsDao {

    @Insert
    suspend fun insertPending(entity: PendingSmsTransactionEntity): Long

    /** Observed by the Pending Transactions screen — emits on every change. */
    @Query("SELECT * FROM pending_sms_transactions ORDER BY receivedAt DESC")
    fun getAllPending(): Flow<List<PendingSmsTransactionEntity>>

    /** Returns current count for the notification badge. */
    @Query("SELECT COUNT(*) FROM pending_sms_transactions")
    fun getPendingCount(): Flow<Int>

    /** Called when user confirms or dismisses a pending entry. */
    @Delete
    suspend fun deletePending(entity: PendingSmsTransactionEntity)

    /** Called when user edits a pending entry before confirming. */
    @Update
    suspend fun updatePending(entity: PendingSmsTransactionEntity)

    /** Clears all pending entries (e.g. dismiss-all action). */
    @Query("DELETE FROM pending_sms_transactions")
    suspend fun clearAll()

    @Query("SELECT EXISTS(SELECT 1 FROM pending_sms_transactions WHERE rawSmsBody = :rawBody LIMIT 1)")
    suspend fun isSmsExists(rawBody: String): Boolean
}
