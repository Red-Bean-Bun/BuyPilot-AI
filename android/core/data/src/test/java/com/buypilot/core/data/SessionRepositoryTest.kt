package com.buypilot.core.data

import com.buypilot.core.database.dao.MessageDao
import com.buypilot.core.database.dao.SessionDao
import com.buypilot.core.database.entity.MessageEntity
import com.buypilot.core.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionRepositoryTest {
    @Test
    fun deleteSessionRemovesSessionAndMessagesOnlyForThatSession() = runTest {
        val sessionDao = FakeSessionDao()
        val messageDao = FakeMessageDao()
        val repository = SessionRepository(sessionDao, messageDao)

        sessionDao.upsert(session("sess_delete"))
        sessionDao.upsert(session("sess_keep"))
        messageDao.upsert(message("msg_delete", "sess_delete"))
        messageDao.upsert(message("msg_keep", "sess_keep"))

        repository.deleteSession("sess_delete")

        assertNull(sessionDao.getSession("sess_delete"))
        assertEquals(emptyList<MessageEntity>(), messageDao.getMessages("sess_delete"))
        assertEquals("sess_keep", sessionDao.getSession("sess_keep")?.sessionId)
        assertEquals(listOf("msg_keep"), messageDao.getMessages("sess_keep").map { it.messageId })
    }
}

private fun session(sessionId: String): SessionEntity =
    SessionEntity(
        sessionId = sessionId,
        title = sessionId,
        lastMessage = sessionId,
        createdAtMs = 1,
        updatedAtMs = 1,
    )

private fun message(messageId: String, sessionId: String): MessageEntity =
    MessageEntity(
        messageId = messageId,
        sessionId = sessionId,
        role = "user",
        content = messageId,
        createdAtMs = 1,
    )

private class FakeSessionDao : SessionDao {
    private val sessions = mutableMapOf<String, SessionEntity>()

    override suspend fun upsert(session: SessionEntity) {
        sessions[session.sessionId] = session
    }

    override fun observeSessions(): Flow<List<SessionEntity>> = flowOf(sessions.values.toList())

    override suspend fun getSession(sessionId: String): SessionEntity? = sessions[sessionId]

    override suspend fun delete(sessionId: String) {
        sessions.remove(sessionId)
    }
}

private class FakeMessageDao : MessageDao {
    private val messagesBySessionId = mutableMapOf<String, MutableList<MessageEntity>>()

    override suspend fun upsert(message: MessageEntity) {
        messagesBySessionId.getOrPut(message.sessionId) { mutableListOf() }.add(message)
    }

    override fun observeMessages(sessionId: String): Flow<List<MessageEntity>> =
        flowOf(messagesBySessionId[sessionId].orEmpty())

    override suspend fun getMessages(sessionId: String): List<MessageEntity> =
        messagesBySessionId[sessionId].orEmpty()

    override suspend fun deleteForSession(sessionId: String) {
        messagesBySessionId.remove(sessionId)
    }
}
