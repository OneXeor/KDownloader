package dev.onexeor.kdownloader

/**
 * Handle for a download operation.
 *
 * Provides methods to control and observe the download.
 *
 * Example:
 * ```kotlin
 * val task = downloader.download("https://example.com/file.zip") {
 *     fileName = "file.zip"
 * }
 *
 * // Check state
 * println("State: ${task.currentState}")
 * println("Progress: ${task.currentProgress.percentage}%")
 *
 * // Add listeners after creation
 * task.onProgress { println("${it.percentage}%") }
 *     .onComplete { println("Done: $it") }
 *     .onError { println("Error: ${it.message}") }
 *
 * // Control
 * task.cancel()
 * ```
 */
interface DownloadTask {
    /**
     * Unique identifier for this download.
     */
    val id: String

    /**
     * The original request configuration.
     */
    val request: DownloadRequest

    /**
     * Current download state.
     */
    val currentState: DownloadState

    /**
     * Current download progress.
     */
    val currentProgress: DownloadProgress

    /**
     * Cancel this download.
     */
    fun cancel()

    /**
     * Add a progress listener.
     * @return this task for chaining
     */
    fun onProgress(listener: (DownloadProgress) -> Unit): DownloadTask

    /**
     * Add a state change listener.
     * @return this task for chaining
     */
    fun onStateChange(listener: (DownloadState) -> Unit): DownloadTask

    /**
     * Add a completion listener.
     * @param listener receives the file path
     * @return this task for chaining
     */
    fun onComplete(listener: (String) -> Unit): DownloadTask

    /**
     * Add an error listener.
     * @return this task for chaining
     */
    fun onError(listener: (DownloadError) -> Unit): DownloadTask
}
