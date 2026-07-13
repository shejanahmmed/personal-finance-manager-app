package com.shejan.financebuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Holds SMS-detected transactions that are waiting for user confirmation.
 * Data is stored in the same SQLCipher-encrypted database as all other app data.
 * Records are deleted once the user confirms (→ saved to transactions) or dismisses.
 */
@Entity(tableName = "pending_sms_transactions")
data class PendingSmsTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    /** The original SMS body — kept for audit purposes until user acts, never synced externally. */
    val rawSmsBody: String,

    /** Sender address as reported by Android (e.g. "bKash", "BRACBank", "8801XXXXXXX"). */
    val senderAddress: String,

    /** Parsed amount in BDT. */
    val amount: Double,

    /** "INCOME", "EXPENSE", or "TRANSFER". */
    val type: String,

    /** Suggested expense/income category (e.g. "Mobile Banking", "Transfer"). */
    val category: String,

    /** Note extracted from SMS (merchant, reference, recipient name, etc.). */
    val note: String,

    /**
     * The account name matched from the sender (e.g. "bKash", "BRAC Bank PLC").
     * Used to pre-fill the account picker in the edit sheet.
     */
    val detectedAccountName: String,

    /** Pre-filled account ID matching detectedAccountName. -1 if not matched. */
    val fromAccountId: Int,

    /** Only set for TRANSFER type. */
    val toAccountId: Int?,

    /** Timestamp parsed from SMS body, or System.currentTimeMillis() if unparseable. */
    val timestamp: Long,

    /** When this SMS was received by the BroadcastReceiver. */
    val receivedAt: Long
)
