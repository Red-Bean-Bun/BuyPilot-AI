package com.buypilot.core.network.di

import com.buypilot.core.common.json.AppJson
import com.buypilot.core.network.AdminAuthInterceptor
import com.buypilot.core.network.BaseUrlProvider
import com.buypilot.core.network.BuildConfigBaseUrlProvider
import com.buypilot.core.network.CartApi
import com.buypilot.core.network.ChatApi
import com.buypilot.core.network.ChatCancelApi
import com.buypilot.core.network.FeedbackApi
import com.buypilot.core.network.ImageUploadApi
import com.buypilot.core.network.OkHttpCartApi
import com.buypilot.core.network.OkHttpChatCancelApi
import com.buypilot.core.network.OkHttpFeedbackApi
import com.buypilot.core.network.OkHttpImageUploadApi
import com.buypilot.core.network.OkHttpProductDetailApi
import com.buypilot.core.network.OkHttpSseClient
import com.buypilot.core.network.ProductDetailApi
import com.buypilot.core.network.RestClient
import com.buypilot.core.network.SseChatApi
import com.buypilot.core.network.SseClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = AppJson.instance

    @Provides
    @Singleton
    fun provideBaseUrlProvider(): BaseUrlProvider = BuildConfigBaseUrlProvider()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .addInterceptor(AdminAuthInterceptor(com.buypilot.core.network.BuildConfig.ADMIN_API_KEY))
            .build()

    @Provides
    @Singleton
    fun provideSseClient(client: OkHttpSseClient): SseClient = client

    @Provides
    @Singleton
    fun provideChatApi(sseClient: SseClient): ChatApi = SseChatApi(sseClient)

    @Provides
    @Singleton
    fun provideChatCancelApi(restClient: RestClient): ChatCancelApi = OkHttpChatCancelApi(restClient)

    @Provides
    @Singleton
    fun provideImageUploadApi(restClient: RestClient): ImageUploadApi = OkHttpImageUploadApi(restClient)

    @Provides
    @Singleton
    fun provideFeedbackApi(restClient: RestClient): FeedbackApi = OkHttpFeedbackApi(restClient)

    @Provides
    @Singleton
    fun provideCartApi(restClient: RestClient): CartApi = OkHttpCartApi(restClient)

    @Provides
    @Singleton
    fun provideProductDetailApi(restClient: RestClient): ProductDetailApi = OkHttpProductDetailApi(restClient)
}
