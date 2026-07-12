package com.shejan.financebuddy.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,           // e.g. "Emergency Fund"
    val targetAmount: Double,    // savings target in BDT
    val savedAmount: Double = 0.0, // accumulated deposits
    val colorHex: String,        // accent color for the card
    val emoji: String,           // e.g. "🎯", "🏠", "✈️"
    val deadline: Long? = null,  // optional epoch millis
    val createdAt: Long          // epoch millis
)
