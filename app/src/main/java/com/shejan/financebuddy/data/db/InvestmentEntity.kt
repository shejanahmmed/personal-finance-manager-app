package com.shejan.financebuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: String, // e.g. "FDR", "SANCHAYAPATRA", "STOCKS", "MUTUAL_FUND", "GOLD", "REAL_ESTATE", "OTHER"
    val institution: String, // e.g. "BRAC Bank", "National Savings", "DSE"
    val investedAmount: Double,
    val currentValue: Double,
    val expectedReturnRate: Double = 0.0, // Annual % rate
    val startDate: Long = System.currentTimeMillis(),
    val maturityDate: Long? = null,
    val note: String = "",
    val status: String = "ACTIVE" // "ACTIVE", "MATURED", "CLOSED"
)
