package com.shejan.financebuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bankName: String,
    val loanAmount: Double,
    val durationMonths: Int,
    val interestRate: Double,
    val repaidAmount: Double = 0.0,
    val accountId: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
