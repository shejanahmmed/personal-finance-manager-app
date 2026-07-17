package com.shejan.financebuddy.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payee_accounts",
    foreignKeys = [
        ForeignKey(
            entity = PayeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["payeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["payeeId"])]
)
data class PayeeAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val payeeId: Int,
    val bankName: String,
    val accountNumber: String,
    val recipientName: String,
    val type: String, // "BANK" or "MFS"
    val nickname: String = ""
)
