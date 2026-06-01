package com.buypilot.core.network

import com.buypilot.core.common.json.AppJson
import com.buypilot.core.model.CartItemPayload
import com.buypilot.core.model.requests.CartMutationRequest
import com.buypilot.core.model.responses.CartResponse
import kotlinx.serialization.encodeToString

interface CartApi {
    suspend fun getCart(sessionId: String): CartResponse
    suspend fun updateQuantity(sessionId: String, productId: String, quantity: Int): CartItemPayload
    suspend fun removeItem(sessionId: String, productId: String)
}

class OkHttpCartApi(
    private val restClient: RestClient,
) : CartApi {
    override suspend fun getCart(sessionId: String): CartResponse =
        restClient.getJson(
            path = "/cart/$sessionId",
            responseDeserializer = CartResponse.serializer(),
        )

    override suspend fun updateQuantity(sessionId: String, productId: String, quantity: Int): CartItemPayload =
        restClient.patchJson(
            path = "/cart/$sessionId/items/$productId",
            bodyJson = AppJson.instance.encodeToString(CartMutationRequest(quantity = quantity)),
            responseDeserializer = CartItemPayload.serializer(),
        )

    override suspend fun removeItem(sessionId: String, productId: String) {
        restClient.delete(path = "/cart/$sessionId/items/$productId")
    }
}
