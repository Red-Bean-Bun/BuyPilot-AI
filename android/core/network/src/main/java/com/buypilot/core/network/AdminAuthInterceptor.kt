package com.buypilot.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that injects `Authorization: Bearer <adminKey>` when
 * the admin API key is configured. No-op when [adminKey] is blank, matching
 * the backend's "no key = no auth" behaviour.
 */
class AdminAuthInterceptor(private val adminKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (adminKey.isBlank() || original.header("Authorization") != null) {
            return chain.proceed(original)
        }
        val authenticated = original.newBuilder()
            .header("Authorization", "Bearer $adminKey")
            .build()
        return chain.proceed(authenticated)
    }
}
