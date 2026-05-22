package com.buypilot.core.network

interface BaseUrlProvider {
    val baseUrl: String
}

class BuildConfigBaseUrlProvider : BaseUrlProvider {
    override val baseUrl: String = BuildConfig.BUY_PILOT_BASE_URL
}
