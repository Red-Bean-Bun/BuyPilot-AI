package com.buypilot.core.data

import com.buypilot.core.database.dao.MessageDao
import com.buypilot.core.database.dao.SessionDao
import com.buypilot.core.database.entity.MessageEntity
import com.buypilot.core.database.entity.SessionEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
) {
    fun observeSessions(): Flow<List<SessionSummary>> =
        sessionDao.observeSessions().map { sessions -> sessions.map(SessionEntity::toSummary) }

    fun observeMessages(sessionId: String): Flow<List<MessageEntity>> =
        messageDao.observeMessages(sessionId)

    suspend fun restoreMessages(sessionId: String): List<MessageEntity> =
        messageDao.getMessages(sessionId)

    suspend fun deleteSession(sessionId: String) {
        messageDao.deleteForSession(sessionId)
        sessionDao.delete(sessionId)
    }
}

private fun SessionEntity.toSummary(): SessionSummary =
    SessionSummary(
        sessionId = sessionId,
        title = title,
        lastMessage = lastMessage,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
    )
