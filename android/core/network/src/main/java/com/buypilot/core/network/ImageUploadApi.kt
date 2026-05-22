package com.buypilot.core.network

import com.buypilot.core.model.responses.ImageUploadResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

interface ImageUploadApi {
    suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        sessionId: String? = null,
    ): ImageUploadResponse
}

class OkHttpImageUploadApi(
    private val restClient: RestClient,
) : ImageUploadApi {
    override suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        sessionId: String?,
    ): ImageUploadResponse {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                fileName,
                bytes.toRequestBody(mimeType.toMediaType()),
            )
            .apply {
                if (sessionId != null) {
                    addFormDataPart("session_id", sessionId)
                }
                addFormDataPart("purpose", "chat_input")
            }
            .build()

        return restClient.postMultipart(
            path = "/upload/image",
            body = body,
            responseDeserializer = ImageUploadResponse.serializer(),
        )
    }
}
