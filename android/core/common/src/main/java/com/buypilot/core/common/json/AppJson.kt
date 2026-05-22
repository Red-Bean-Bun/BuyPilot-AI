package com.buypilot.core.common.json

import kotlinx.serialization.json.Json

object AppJson {
    val instance: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }
}
