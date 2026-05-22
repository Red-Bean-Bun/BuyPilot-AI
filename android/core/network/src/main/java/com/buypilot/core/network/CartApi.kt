package com.buypilot.core.network

import com.buypilot.core.model.responses.CartResponse

interface CartApi {
    suspend fun getCart(sessionId: String): CartResponse
}

class OkHttpCartApi(
    private val restClient: RestClient,
) : CartApi {
    override suspend fun getCart(sessionId: String): CartResponse =
        restClient.getJson(
            path = "/cart/$sessionId",
            responseDeserializer = CartResponse.serializer(),
        )
}
