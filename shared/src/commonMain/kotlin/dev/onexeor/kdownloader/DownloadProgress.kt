package dev.onexeor.kdownloader

/**
 * Represents the progress of a download.
 *
 * @property bytesDownloaded Number of bytes downloaded so far
 * @property totalBytes Total size in bytes, or -1 if unknown
 * @property percentage Download percentage (0-100), or -1 if unknown
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val percentage: Int
) {
    companion object {
        /**
         * Initial progress state before download starts.
         */
        val ZERO = DownloadProgress(
            bytesDownloaded = 0,
            totalBytes = -1,
            percentage = -1
        )
    }

    /**
     * Whether the total size is known.
     */
    val isSizeKnown: Boolean get() = totalBytes > 0

    /**
     * Whether the download has started (bytes received).
     */
    val hasStarted: Boolean get() = bytesDownloaded > 0
}
