package dev.onexeor.kdownloader

/**
 * Represents the current state of a download.
 */
sealed class DownloadState {
    /**
     * Download is queued but not yet started.
     */
    data object Pending : DownloadState()

    /**
     * Download is actively in progress.
     */
    data class Downloading(val progress: DownloadProgress) : DownloadState()

    /**
     * Download is paused.
     */
    data class Paused(val reason: String) : DownloadState()

    /**
     * Download completed successfully.
     * @property filePath Path to the downloaded file
     */
    data class Completed(val filePath: String) : DownloadState()

    /**
     * Download failed with an error.
     */
    data class Failed(val error: DownloadError) : DownloadState()

    /**
     * Download was cancelled by the user.
     */
    data object Cancelled : DownloadState()
}
