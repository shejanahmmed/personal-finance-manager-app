package com.shejan.financebuddy.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromAccountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["fromAccountId"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "INCOME", "EXPENSE", "TRANSFER"
    val category: String, // e.g., "Salary", "Food", "Utilities", "Transfer"
    val timestamp: Long,
    val fromAccountId: Int, // The account where money goes in (Income), out of (Expense), or moves from (Transfer)
    val toAccountId: Int? = null, // Only used for "TRANSFER" to identify target account
    val note: String = ""
)
