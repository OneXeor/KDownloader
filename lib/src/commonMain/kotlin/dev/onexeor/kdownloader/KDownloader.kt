package dev.onexeor.kdownloader

/**
 * Main entry point for downloading files.
 *
 * Platform-specific implementations use native download managers:
 * - Android: DownloadManager (system-managed, survives app kill)
 * - iOS: NSURLSession (background download support)
 *
 * Example:
 * ```kotlin
 * val downloader = KDownloader()
 *
 * downloader.download("https://example.com/file.pdf") {
 *     fileName = "document.pdf"
 *     onComplete { println("Downloaded: $it") }
 * }
 * ```
 */
expect class KDownloader(config: KDownloaderConfig = KDownloaderConfig()) {

    /**
     * Start a download with DSL configuration.
     *
     * @param url The URL to download from
     * @param builder DSL block to configure the download request
     * @return DownloadTask handle for tracking and controlling the download
     */
    fun download(url: String, builder: DownloadRequestBuilder.() -> Unit = {}): DownloadTask

    /**
     * Start a download with a pre-built request.
     *
     * @param request The download request configuration
     * @return DownloadTask handle for tracking and controlling the download
     */
    fun download(request: DownloadRequest): DownloadTask

    /**
     * Get an existing download task by ID.
     *
     * @param id The task ID
     * @return The task if found, null otherwise
     */
    fun getTask(id: String): DownloadTask?

    /**
     * Cancel all active downloads.
     */
    fun cancelAll()
}
