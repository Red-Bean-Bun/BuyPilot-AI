package com.buypilot.core.common.id

import java.util.UUID

object Ids {
    fun clientTurnId(): String = "android-turn-${UUID.randomUUID()}"
    fun clientTraceId(): String = "android-trace-${UUID.randomUUID()}"
    fun userMessageId(nowMs: Long): String = "user_$nowMs"
}
