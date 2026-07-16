package com.shejan.financebuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String,           // "BANK" or "MFS"
    val balance: Double,
    val colorHex: String,
    val accountSubtype: String = "",  // e.g. "Savings", "Current", "Salary", "Student"
    val isManaged: Boolean = false,   // true if user manages this for someone else
    val holderName: String = "",      // account holder name (for managed accounts)
    val accountNumber: String = "",   // optional account number for reference
    val showAs: String = ""           // custom display name alias
)

