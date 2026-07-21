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

    /** Used by BackupManager to backup all pending, confirmed, and dismissed detections. */
    @Query("SELECT * FROM pending_sms_transactions ORDER BY receivedAt DESC")
    fun getAllForBackup(): Flow<List<PendingSmsTransactionEntity>>

    /** Observed by the Pending Transactions screen — emits pending detections. */
    @Query("SELECT * FROM pending_sms_transactions WHERE status = 'PENDING' ORDER BY receivedAt DESC")
    fun getAllPending(): Flow<List<PendingSmsTransactionEntity>>

    /** Observed by the Inbox screen for confirmed detections. */
    @Query("SELECT * FROM pending_sms_transactions WHERE status = 'CONFIRMED' ORDER BY receivedAt DESC")
    fun getConfirmedList(): Flow<List<PendingSmsTransactionEntity>>

    /** Observed by the Inbox screen for dismissed detections. */
    @Query("SELECT * FROM pending_sms_transactions WHERE status = 'DISMISSED' ORDER BY receivedAt DESC")
    fun getDismissedList(): Flow<List<PendingSmsTransactionEntity>>

    /** Returns current count for the notification badge (PENDING only). */
    @Query("SELECT COUNT(*) FROM pending_sms_transactions WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    /** Returns count of confirmed items. */
    @Query("SELECT COUNT(*) FROM pending_sms_transactions WHERE status = 'CONFIRMED'")
    fun getConfirmedCount(): Flow<Int>

    /** Returns count of dismissed items. */
    @Query("SELECT COUNT(*) FROM pending_sms_transactions WHERE status = 'DISMISSED'")
    fun getDismissedCount(): Flow<Int>

    /** Update status of a pending entry (PENDING, CONFIRMED, DISMISSED). */
    @Query("UPDATE pending_sms_transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    /** Marks all currently PENDING entries as DISMISSED. */
    @Query("UPDATE pending_sms_transactions SET status = 'DISMISSED' WHERE status = 'PENDING'")
    suspend fun dismissAllPending()

    /** Called when user confirms or deletes a pending entry. */
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
