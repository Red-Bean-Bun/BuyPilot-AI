package com.buypilot.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE messages ADD COLUMN node_type TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN payload_json TEXT")
            db.execSQL("ALTER TABLE messages ADD COLUMN deck_id TEXT")
        }
    }
}
