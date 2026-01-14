package dev.onexeor.kdownloader.internal

import android.app.DownloadManager
import android.database.Cursor
import android.os.Handler
import dev.onexeor.kdownloader.DownloadError
import dev.onexeor.kdownloader.DownloadListeners
import dev.onexeor.kdownloader.DownloadProgress
import dev.onexeor.kdownloader.DownloadRequest
import dev.onexeor.kdownloader.DownloadState
import dev.onexeor.kdownloader.DownloadTask

/**
 * Android implementation of DownloadTask using DownloadManager.
 */
internal class AndroidDownloadTask(
    override val id: String,
    val downloadId: Long,
    override val request: DownloadRequest,
    val filePath: String,
    private val downloadManager: DownloadManager,
    private val workerHandler: Handler,
    private val mainHandler: Handler,
    private val initialListeners: DownloadListeners
) : DownloadTask {

    @Volatile
    private var _currentState: DownloadState = DownloadState.Pending
    override val currentState: DownloadState get() = _currentState

    @Volatile
    private var _currentProgress: DownloadProgress = DownloadProgress.ZERO
    override val currentProgress: DownloadProgress get() = _currentProgress

    @Volatile
    private var isMonitoring = false

    @Volatile
    private var isCancelled = false

    private val progressListeners = mutableListOf<(DownloadProgress) -> Unit>()
    private val stateListeners = mutableListOf<(DownloadState) -> Unit>()
    private val completeListeners = mutableListOf<(String) -> Unit>()
    private val errorListeners = mutableListOf<(DownloadError) -> Unit>()

    init {
        // Add initial listeners from request
        initialListeners.onProgress?.let { progressListeners.add(it) }
        initialListeners.onStateChange?.let { stateListeners.add(it) }
        initialListeners.onComplete?.let { completeListeners.add(it) }
        initialListeners.onError?.let { errorListeners.add(it) }
    }

    override fun cancel() {
        isCancelled = true
        isMonitoring = false
        downloadManager.remove(downloadId)
        updateState(DownloadState.Cancelled)
        notifyError(DownloadError.Cancelled)
    }

    override fun onProgress(listener: (DownloadProgress) -> Unit): DownloadTask {
        progressListeners.add(listener)
        return this
    }

    override fun onStateChange(listener: (DownloadState) -> Unit): DownloadTask {
        stateListeners.add(listener)
        return this
    }

    override fun onComplete(listener: (String) -> Unit): DownloadTask {
        completeListeners.add(listener)
        return this
    }

    override fun onError(listener: (DownloadError) -> Unit): DownloadTask {
        errorListeners.add(listener)
        return this
    }

    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        workerHandler.post { pollProgress() }
    }

    private fun pollProgress() {
        if (!isMonitoring || isCancelled) return

        val query = DownloadManager.Query().setFilterById(downloadId)
        var cursor: Cursor? = null

        try {
            cursor = downloadManager.query(query)

            if (cursor == null || !cursor.moveToFirst()) {
                // Download not found, might have been removed
                if (!isCancelled) {
                    notifyError(DownloadError.Unknown("Download not found"))
                }
                return
            }

            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalBytesIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

            val status = cursor.getInt(statusIndex)
            val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
            val totalBytes = cursor.getLong(totalBytesIndex)

            // Update progress
            val percentage = if (totalBytes > 0) {
                ((bytesDownloaded * 100) / totalBytes).toInt()
            } else {
                -1
            }

            val progress = DownloadProgress(
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                percentage = percentage
            )

            if (progress != _currentProgress) {
                _currentProgress = progress
                notifyProgress(progress)
            }

            when (status) {
                DownloadManager.STATUS_PENDING -> {
                    updateState(DownloadState.Pending)
                }

                DownloadManager.STATUS_RUNNING -> {
                    updateState(DownloadState.Downloading(progress))
                }

                DownloadManager.STATUS_PAUSED -> {
                    val reason = cursor.getInt(reasonIndex)
                    val message = getPausedReason(reason)
                    updateState(DownloadState.Paused(message))
                }

                DownloadManager.STATUS_SUCCESSFUL -> {
                    isMonitoring = false
                    updateState(DownloadState.Completed(filePath))
                    notifyComplete(filePath)
                    return
                }

                DownloadManager.STATUS_FAILED -> {
                    isMonitoring = false
                    val reason = cursor.getInt(reasonIndex)
                    val error = getDownloadError(reason)
                    updateState(DownloadState.Failed(error))
                    notifyError(error)
                    return
                }
            }

            // Continue polling
            if (isMonitoring && !isCancelled) {
                workerHandler.postDelayed({ pollProgress() }, POLL_INTERVAL_MS)
            }

        } catch (e: Exception) {
            if (!isCancelled) {
                val error = DownloadError.Unknown("Error monitoring download: ${e.message}", e)
                updateState(DownloadState.Failed(error))
                notifyError(error)
            }
        } finally {
            cursor?.close()
        }
    }

    private fun updateState(newState: DownloadState) {
        if (_currentState != newState) {
            _currentState = newState
            notifyState(newState)
        }
    }

    private fun notifyProgress(progress: DownloadProgress) {
        mainHandler.post {
            progressListeners.forEach { it(progress) }
        }
    }

    private fun notifyState(state: DownloadState) {
        mainHandler.post {
            stateListeners.forEach { it(state) }
        }
    }

    private fun notifyComplete(path: String) {
        mainHandler.post {
            completeListeners.forEach { it(path) }
        }
    }

    private fun notifyError(error: DownloadError) {
        mainHandler.post {
            errorListeners.forEach { it(error) }
        }
    }

    private fun getPausedReason(reason: Int): String {
        return when (reason) {
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for WiFi"
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network"
            DownloadManager.PAUSED_WAITING_TO_RETRY -> "Waiting to retry"
            DownloadManager.PAUSED_UNKNOWN -> "Paused"
            else -> "Paused"
        }
    }

    private fun getDownloadError(reason: Int): DownloadError {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> DownloadError.Network("Cannot resume download")
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> DownloadError.Storage("Storage device not found")
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> DownloadError.Storage("File already exists")
            DownloadManager.ERROR_FILE_ERROR -> DownloadError.Storage("File error")
            DownloadManager.ERROR_HTTP_DATA_ERROR -> DownloadError.Network("HTTP data error")
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> DownloadError.Storage("Insufficient storage space")
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> DownloadError.Network("Too many redirects")
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> DownloadError.Http(0, "Unhandled HTTP error")
            DownloadManager.ERROR_UNKNOWN -> DownloadError.Unknown("Unknown download error")
            else -> {
                // HTTP error codes are returned directly for 4xx and 5xx
                if (reason in 400..599) {
                    DownloadError.Http(reason, "HTTP error $reason")
                } else {
                    DownloadError.Unknown("Unknown error: $reason")
                }
            }
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 500L
    }
}
