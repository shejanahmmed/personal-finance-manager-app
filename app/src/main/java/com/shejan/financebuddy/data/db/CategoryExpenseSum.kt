package com.shejan.financebuddy.data.db

import androidx.room.ColumnInfo

/** Projection used by [TransactionDao.getExpensesByCategoryFromDate] */
data class CategoryExpenseSum(
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "total")    val total: Double
)
