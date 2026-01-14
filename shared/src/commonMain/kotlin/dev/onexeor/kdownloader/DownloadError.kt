package dev.onexeor.kdownloader

/**
 * Represents errors that can occur during download.
 *
 * @property message Human-readable error description
 * @property cause Underlying exception, if any
 */
sealed class DownloadError(
    open val message: String,
    open val cause: Throwable? = null
) {
    /**
     * Network-related error (connection failed, timeout, etc.)
     */
    data class Network(
        override val message: String,
        override val cause: Throwable? = null
    ) : DownloadError(message, cause)

    /**
     * HTTP error response from server.
     *
     * @property statusCode HTTP status code (4xx, 5xx)
     */
    data class Http(
        val statusCode: Int,
        override val message: String
    ) : DownloadError(message)

    /**
     * Storage-related error (disk full, permission denied, etc.)
     */
    data class Storage(
        override val message: String,
        override val cause: Throwable? = null
    ) : DownloadError(message, cause)

    /**
     * Invalid or malformed URL.
     */
    data class InvalidUrl(
        override val message: String
    ) : DownloadError(message)

    /**
     * Download was cancelled.
     */
    data object Cancelled : DownloadError("Download cancelled")

    /**
     * Unknown or unexpected error.
     */
    data class Unknown(
        override val message: String,
        override val cause: Throwable? = null
    ) : DownloadError(message, cause)
}
