package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val spokenQuery: String,
    val identifiedIntent: String,
    val responseText: String,
    val executionSuccess: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
