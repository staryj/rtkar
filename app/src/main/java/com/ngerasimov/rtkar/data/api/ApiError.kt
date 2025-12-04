package com.ngerasimov.rtkar.data.api

/**
 * Sealed class representing different types of API errors
 * Extends Throwable to work with Kotlin's Result type
 */
sealed class ApiError(message: String? = null, cause: Throwable? = null) : Throwable(message, cause) {
    /**
     * Network connectivity issue (no internet, timeout, etc.)
     */
    object NetworkError : ApiError("Network error occurred")
    
    /**
     * Authentication/authorization failed (401, 403)
     */
    object Unauthorized : ApiError("Unauthorized access")
    
    /**
     * Server error (4xx, 5xx)
     */
    data class ServerError(
        val code: Int,
        val errorMessage: String
    ) : ApiError("Server error $code: $errorMessage") {
        override val message: String
            get() = errorMessage
    }
    
    /**
     * Unknown or unexpected error
     */
    data class UnknownError(
        val throwable: Throwable
    ) : ApiError(throwable.message, throwable)
    
    /**
     * Get user-friendly error message
     */
    fun getUserMessage(context: android.content.Context): String {
        return when (this) {
            is NetworkError -> context.getString(com.ngerasimov.rtkar.R.string.error_network)
            is Unauthorized -> context.getString(com.ngerasimov.rtkar.R.string.error_unauthorized)
            is ServerError -> context.getString(com.ngerasimov.rtkar.R.string.error_server, code, errorMessage)
            is UnknownError -> context.getString(com.ngerasimov.rtkar.R.string.error_unknown, throwable.message ?: "")
        }
    }
}
