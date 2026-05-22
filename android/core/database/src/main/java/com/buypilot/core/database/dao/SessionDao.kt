package com.buypilot.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.buypilot.core.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY updated_at_ms DESC")
    fun observeSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE session_id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): SessionEntity?

    @Query("DELETE FROM sessions WHERE session_id = :sessionId")
    suspend fun delete(sessionId: String)
}
