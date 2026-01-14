package dev.onexeor.kdownloader

/**
 * Immutable download request configuration.
 */
class DownloadRequest internal constructor(
    val url: String,
    val fileName: String?,
    val directory: String?,
    val headers: Map<String, String>,
    val auth: Auth?,
    val networkType: NetworkType?,
    val overwrite: Boolean,
    internal val listeners: DownloadListeners
)

/**
 * DSL builder for creating download requests.
 *
 * Example:
 * ```kotlin
 * downloader.download("https://example.com/file.zip") {
 *     fileName = "archive.zip"
 *     directory = "/custom/path"
 *
 *     headers {
 *         "Authorization" to "Bearer token"
 *         "Accept" to "application/octet-stream"
 *     }
 *
 *     auth {
 *         bearer("token")
 *     }
 *
 *     wifiOnly()
 *     overwriteIfExists()
 *
 *     onProgress { progress ->
 *         println("${progress.percentage}%")
 *     }
 *
 *     onComplete { filePath ->
 *         println("Downloaded to: $filePath")
 *     }
 *
 *     onError { error ->
 *         println("Failed: ${error.message}")
 *     }
 * }
 * ```
 */
class DownloadRequestBuilder(val url: String) {
    /**
     * File name for the downloaded file.
     * If null, will be derived from URL or use a generated name.
     */
    var fileName: String? = null

    /**
     * Directory to save the file.
     * If null, uses the downloader's default directory.
     */
    var directory: String? = null

    /**
     * Whether to overwrite existing file with same name.
     */
    var overwrite: Boolean = false

    /**
     * Network type restriction for this download.
     * If null, uses the downloader's default.
     */
    var networkType: NetworkType? = null

    private val headersMap = mutableMapOf<String, String>()
    private var authValue: Auth? = null
    private val listenersBuilder = DownloadListeners()

    /**
     * Configure request headers.
     */
    fun headers(block: HeadersBuilder.() -> Unit) {
        HeadersBuilder(headersMap).apply(block)
    }

    /**
     * Configure authentication.
     */
    fun auth(block: AuthBuilder.() -> Unit) {
        authValue = AuthBuilder().apply(block).build()
    }

    /**
     * Restrict download to WiFi only.
     */
    fun wifiOnly() {
        networkType = NetworkType.WIFI_ONLY
    }

    /**
     * Allow overwriting existing files.
     */
    fun overwriteIfExists() {
        overwrite = true
    }

    /**
     * Called when download progress updates.
     */
    fun onProgress(listener: (DownloadProgress) -> Unit) {
        listenersBuilder.onProgress = listener
    }

    /**
     * Called when download state changes.
     */
    fun onStateChange(listener: (DownloadState) -> Unit) {
        listenersBuilder.onStateChange = listener
    }

    /**
     * Called when download completes successfully.
     * @param listener receives the file path
     */
    fun onComplete(listener: (String) -> Unit) {
        listenersBuilder.onComplete = listener
    }

    /**
     * Called when download fails.
     */
    fun onError(listener: (DownloadError) -> Unit) {
        listenersBuilder.onError = listener
    }

    internal fun build(): DownloadRequest = DownloadRequest(
        url = url,
        fileName = fileName,
        directory = directory,
        headers = headersMap.toMap(),
        auth = authValue,
        networkType = networkType,
        overwrite = overwrite,
        listeners = listenersBuilder
    )
}

/**
 * DSL builder for HTTP headers.
 */
class HeadersBuilder(private val headers: MutableMap<String, String>) {
    infix fun String.to(value: String) {
        headers[this] = value
    }
}

/**
 * DSL builder for authentication.
 */
class AuthBuilder {
    private var auth: Auth? = null

    fun bearer(token: String) {
        auth = Auth.Bearer(token)
    }

    fun basic(username: String, password: String) {
        auth = Auth.Basic(username, password)
    }

    internal fun build(): Auth? = auth
}

/**
 * Internal holder for download listeners.
 */
internal class DownloadListeners {
    var onProgress: ((DownloadProgress) -> Unit)? = null
    var onStateChange: ((DownloadState) -> Unit)? = null
    var onComplete: ((String) -> Unit)? = null
    var onError: ((DownloadError) -> Unit)? = null
}
