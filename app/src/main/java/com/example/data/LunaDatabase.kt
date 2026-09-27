package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CommandHistoryDao
import com.example.data.dao.ReminderDao
import com.example.data.model.CommandHistoryEntity
import com.example.data.model.ReminderEntity

@Database(
    entities = [ReminderEntity::class, CommandHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LunaDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun commandHistoryDao(): CommandHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: LunaDatabase? = null

        fun getInstance(context: Context): LunaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LunaDatabase::class.java,
                    "luna_database.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
