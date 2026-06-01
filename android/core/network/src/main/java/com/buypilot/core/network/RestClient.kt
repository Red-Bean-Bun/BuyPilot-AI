package com.buypilot.core.network

import com.buypilot.core.common.coroutine.DispatcherProvider
import javax.inject.Inject
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class RestClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val baseUrlProvider: BaseUrlProvider,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun <R : Any> postJson(
        path: String,
        bodyJson: String,
        responseDeserializer: DeserializationStrategy<R>,
    ): R =
        executeJson(
            Request.Builder()
                .url(url(path))
                .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json")
                .build(),
            responseDeserializer,
        )

    suspend fun <R : Any> patchJson(
        path: String,
        bodyJson: String,
        responseDeserializer: DeserializationStrategy<R>,
    ): R =
        executeJson(
            Request.Builder()
                .url(url(path))
                .patch(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
                .header("Content-Type", "application/json")
                .build(),
            responseDeserializer,
        )

    suspend fun <R : Any> postMultipart(
        path: String,
        body: MultipartBody,
        responseDeserializer: DeserializationStrategy<R>,
    ): R =
        executeJson(
            Request.Builder()
                .url(url(path))
                .post(body)
                .build(),
            responseDeserializer,
        )

    suspend fun <R : Any> getJson(path: String, responseDeserializer: DeserializationStrategy<R>): R =
        executeJson(
            Request.Builder()
                .url(url(path))
                .get()
                .build(),
            responseDeserializer,
        )

    suspend fun delete(path: String) {
        executeStatus(
            Request.Builder()
                .url(url(path))
                .delete()
                .build(),
        )
    }

    suspend fun <R : Any> executeJson(
        request: Request,
        responseDeserializer: DeserializationStrategy<R>,
    ): R =
        withContext(dispatchers.io) {
            okHttpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}: $responseBody")
                }
                json.decodeFromString(responseDeserializer, responseBody)
            }
        }

    private suspend fun executeStatus(request: Request) {
        withContext(dispatchers.io) {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}: ${response.body?.string().orEmpty()}")
                }
            }
        }
    }

    fun url(path: String): String = baseUrlProvider.baseUrl.trimEnd('/') + path

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
