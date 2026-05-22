package com.buypilot.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.buypilot.core.database.dao.MessageDao
import com.buypilot.core.database.dao.SessionDao
import com.buypilot.core.database.entity.MessageEntity
import com.buypilot.core.database.entity.SessionEntity

@Database(
    entities = [
        SessionEntity::class,
        MessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
}
