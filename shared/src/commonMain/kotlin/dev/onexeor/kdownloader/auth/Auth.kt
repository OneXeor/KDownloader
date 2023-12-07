package dev.onexeor.kdownloader.auth

/**
 *
 */
sealed class Auth {
    /**
     *
     */
    data class BasicAuth(val login: String, val password: String) : Auth()

    /**
     *
     */
    data class TokenAuth(val token: String) : Auth()
}
