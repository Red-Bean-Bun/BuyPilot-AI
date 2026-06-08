package com.buypilot.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.buypilot.core.database.entity.MessageEntity
import com.buypilot.core.database.entity.SessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertsSessionAndMessages() = runTest {
        database.sessionDao().upsert(
            SessionEntity(
                sessionId = "sess_1",
                title = "title",
                lastMessage = "hello",
                createdAtMs = 1,
                updatedAtMs = 2,
            ),
        )
        database.messageDao().upsert(
            MessageEntity(
                messageId = "msg_1",
                sessionId = "sess_1",
                turnId = "turn_1",
                role = "user",
                content = "hello",
                createdAtMs = 2,
            ),
        )

        assertEquals("title", database.sessionDao().getSession("sess_1")!!.title)
        assertEquals(1, database.messageDao().getMessages("sess_1").size)
    }

    @Test
    fun observesSessionsByUpdatedAtDescending() = runTest {
        database.sessionDao().upsert(
            SessionEntity(
                sessionId = "sess_old",
                title = "old",
                lastMessage = "old",
                createdAtMs = 1,
                updatedAtMs = 10,
            ),
        )
        database.sessionDao().upsert(
            SessionEntity(
                sessionId = "sess_new",
                title = "new",
                lastMessage = "new",
                createdAtMs = 2,
                updatedAtMs = 30,
            ),
        )
        database.sessionDao().upsert(
            SessionEntity(
                sessionId = "sess_mid",
                title = "mid",
                lastMessage = "mid",
                createdAtMs = 3,
                updatedAtMs = 20,
            ),
        )

        val sessions = database.sessionDao().observeSessions().first()

        assertEquals(listOf("sess_new", "sess_mid", "sess_old"), sessions.map { it.sessionId })
    }

    @Test
    fun restoresMessagesByCreatedAtAscending() = runTest {
        database.messageDao().upsert(
            MessageEntity(
                messageId = "msg_late",
                sessionId = "sess_messages",
                role = "user",
                content = "later",
                createdAtMs = 30,
            ),
        )
        database.messageDao().upsert(
            MessageEntity(
                messageId = "msg_early",
                sessionId = "sess_messages",
                role = "user",
                content = "earlier",
                createdAtMs = 10,
            ),
        )

        val messages = database.messageDao().getMessages("sess_messages")

        assertEquals(listOf("msg_early", "msg_late"), messages.map { it.messageId })
    }
}
