package com.buypilot.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["turn_id"]),
    ],
)
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "turn_id")
    val turnId: String? = null,
    val role: String,
    val content: String,
    @ColumnInfo(name = "created_at_ms")
    val createdAtMs: Long,
)
