package com.shejan.financebuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payees")
data class PayeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val uniqueId: String,       // e.g. "PAY-8F3A"
    val createdAt: Long = System.currentTimeMillis()
)
