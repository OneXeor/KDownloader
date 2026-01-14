package dev.onexeor.kdownloader

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Base64
import android.webkit.MimeTypeMap
import dev.onexeor.kdownloader.internal.AndroidDownloadTask
import dev.onexeor.kdownloader.internal.getDownloadDirectory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Android implementation of KDownloader using system DownloadManager.
 *
 * Note: Context must be initialized before use via [KDownloader.init].
 */
actual class KDownloader actual constructor(
    private val config: KDownloaderConfig
) {
    private val downloadManager: DownloadManager by lazy {
        appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    private val tasks = ConcurrentHashMap<String, AndroidDownloadTask>()
    private val workerThread = HandlerThread("KDownloader-Worker").apply { start() }
    private val workerHandler = Handler(workerThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())

    actual fun download(url: String, builder: DownloadRequestBuilder.() -> Unit): DownloadTask {
        val request = DownloadRequestBuilder(url).apply(builder).build()
        return download(request)
    }

    actual fun download(request: DownloadRequest): DownloadTask {
        val taskId = UUID.randomUUID().toString()

        // Determine file name
        val fileName = request.fileName ?: extractFileNameFromUrl(request.url)

        // Determine directory
        val directory = request.directory ?: config.defaultDirectory

        // Get download URI
        val downloadUri = getDownloadDirectory(appContext, directory, fileName)
            ?: throw IllegalStateException("Could not create download directory")

        // Build DownloadManager request
        val dmRequest = DownloadManager.Request(Uri.parse(request.url)).apply {
            setTitle(fileName)
            setDescription("Downloading $fileName")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(downloadUri)

            // Set network type
            val networkType = request.networkType ?: config.defaultNetworkType
            when (networkType) {
                NetworkType.ANY -> setAllowedNetworkTypes(
                    DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
                )
                NetworkType.WIFI_ONLY -> setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
            }

            // Set MIME type
            val mimeType = getMimeType(fileName)
            setMimeType(mimeType)

            // Add headers
            request.headers.forEach { (key, value) ->
                addRequestHeader(key, value)
            }

            // Add auth header
            request.auth?.let { auth ->
                val authHeader = when (auth) {
                    is Auth.Bearer -> "Bearer ${auth.token}"
                    is Auth.Basic -> {
                        val credentials = "${auth.username}:${auth.password}"
                        "Basic ${Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)}"
                    }
                }
                addRequestHeader("Authorization", authHeader)
            }
        }

        // Enqueue download
        val downloadId = downloadManager.enqueue(dmRequest)

        // Create task
        val task = AndroidDownloadTask(
            id = taskId,
            downloadId = downloadId,
            request = request,
            filePath = downloadUri.path ?: "",
            downloadManager = downloadManager,
            workerHandler = workerHandler,
            mainHandler = mainHandler,
            initialListeners = request.listeners
        )

        tasks[taskId] = task

        // Start progress monitoring
        task.startMonitoring()

        return task
    }

    actual fun getTask(id: String): DownloadTask? = tasks[id]

    actual fun cancelAll() {
        tasks.values.forEach { it.cancel() }
        tasks.clear()
    }

    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val path = Uri.parse(url).lastPathSegment
            if (!path.isNullOrBlank() && path.contains(".")) {
                path
            } else {
                "${System.currentTimeMillis()}.bin"
            }
        } catch (e: Exception) {
            "${System.currentTimeMillis()}.bin"
        }
    }

    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast(".", "")
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: DEFAULT_MIME_TYPE
        } else {
            DEFAULT_MIME_TYPE
        }
    }

    companion object {
        private const val DEFAULT_MIME_TYPE = "application/octet-stream"

        @SuppressLint("StaticFieldLeak")
        private lateinit var appContext: Context

        /**
         * Initialize KDownloader with application context.
         * Call this in Application.onCreate().
         *
         * ```kotlin
         * class MyApp : Application() {
         *     override fun onCreate() {
         *         super.onCreate()
         *         KDownloader.init(this)
         *     }
         * }
         * ```
         */
        fun init(context: Context) {
            appContext = context.applicationContext
        }

        /**
         * Check if KDownloader has been initialized.
         */
        val isInitialized: Boolean
            get() = ::appContext.isInitialized
    }
}
