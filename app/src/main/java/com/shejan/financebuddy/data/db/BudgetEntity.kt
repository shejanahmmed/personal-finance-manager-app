package com.shejan.financebuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,      // e.g. "Food", "Rent", "Shopping"
    val limitAmount: Double,   // user-set spending cap in BDT
    val colorHex: String       // display accent color for this category
)
