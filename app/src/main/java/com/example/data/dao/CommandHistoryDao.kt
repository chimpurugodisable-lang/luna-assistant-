package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.model.CommandHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<CommandHistoryEntity>>

    @Insert
    suspend fun insert(entry: CommandHistoryEntity): Long

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()
}
