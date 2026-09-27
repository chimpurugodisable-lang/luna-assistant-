package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val triggerTimeMillis: Long,
    val repeatRule: String? = null, // e.g. "MONDAY", "DAILY", null
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
