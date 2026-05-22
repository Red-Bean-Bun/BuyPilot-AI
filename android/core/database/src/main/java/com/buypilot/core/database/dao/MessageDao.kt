package com.buypilot.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.buypilot.core.database.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE session_id = :sessionId ORDER BY created_at_ms ASC")
    fun observeMessages(sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE session_id = :sessionId ORDER BY created_at_ms ASC")
    suspend fun getMessages(sessionId: String): List<MessageEntity>

    @Query("DELETE FROM messages WHERE session_id = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}
