package com.buypilot.core.network

import com.buypilot.core.model.responses.ProductDetailResponse
import javax.inject.Inject

interface ProductDetailApi {
    suspend fun getProductDetail(productId: String): ProductDetailResponse
}

class OkHttpProductDetailApi @Inject constructor(
    private val restClient: RestClient,
) : ProductDetailApi {
    override suspend fun getProductDetail(productId: String): ProductDetailResponse =
        restClient.getJson(
            path = "/products/$productId",
            responseDeserializer = ProductDetailResponse.serializer(),
        )
}
