package dev.onexeor.kdownloader

/**
 * Configuration for KDownloader instance.
 *
 * Example:
 * ```kotlin
 * val downloader = KDownloader(
 *     KDownloaderConfig(
 *         defaultDirectory = "/downloads",
 *         maxConcurrentDownloads = 2
 *     )
 * )
 * ```
 */
data class KDownloaderConfig(
    /**
     * Default directory for downloads.
     * If null, uses platform default (Documents folder).
     */
    val defaultDirectory: String? = null,

    /**
     * Maximum number of concurrent downloads.
     */
    val maxConcurrentDownloads: Int = 3,

    /**
     * Default network type restriction.
     */
    val defaultNetworkType: NetworkType = NetworkType.ANY
)
