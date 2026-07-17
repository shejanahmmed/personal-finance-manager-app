package com.shejan.financebuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores mapping between a custom or numeric SMS sender address (e.g. "+8801700000000")
 * and a local bank/MFS account.
 */
@Entity(tableName = "sms_sender_mappings")
data class SmsSenderMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderAddress: String, // e.g., "+8801700000000" or a custom code
    val accountId: Int // Mapped account ID from AccountEntity
)
