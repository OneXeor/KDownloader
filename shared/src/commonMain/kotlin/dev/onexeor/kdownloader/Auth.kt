package dev.onexeor.kdownloader

/**
 * Authentication configuration for downloads.
 */
sealed class Auth {
    /**
     * Bearer token authentication.
     * Adds `Authorization: Bearer <token>` header.
     */
    data class Bearer(val token: String) : Auth()

    /**
     * Basic authentication with username and password.
     * Adds `Authorization: Basic <base64>` header.
     */
    data class Basic(val username: String, val password: String) : Auth()
}
