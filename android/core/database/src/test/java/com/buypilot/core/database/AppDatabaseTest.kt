package com.buypilot.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.buypilot.core.database.entity.MessageEntity
import com.buypilot.core.database.entity.SessionEntity
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
}
