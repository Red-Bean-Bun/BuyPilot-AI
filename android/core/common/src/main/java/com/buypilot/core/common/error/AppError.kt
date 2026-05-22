package com.buypilot.core.common.error

sealed interface AppError {
    val message: String
    val retryable: Boolean

    data class Network(
        override val message: String,
        override val retryable: Boolean = true,
        val cause: Throwable? = null,
    ) : AppError

    data class Server(
        val code: String,
        override val message: String,
        override val retryable: Boolean,
    ) : AppError

    data class Parse(
        override val message: String,
        val raw: String? = null,
        val cause: Throwable? = null,
    ) : AppError {
        override val retryable: Boolean = false
    }

    data class Local(
        override val message: String,
        override val retryable: Boolean = false,
    ) : AppError
}
