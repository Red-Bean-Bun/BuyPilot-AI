package com.buypilot.core.common.time

interface Clock {
    fun nowMs(): Long
}

object SystemClock : Clock {
    override fun nowMs(): Long = System.currentTimeMillis()
}
