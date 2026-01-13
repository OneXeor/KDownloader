@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package dev.onexeor.kdownloader.internal

import dev.onexeor.kdownloader.DownloadError
import dev.onexeor.kdownloader.DownloadListeners
import dev.onexeor.kdownloader.DownloadProgress
import dev.onexeor.kdownloader.DownloadRequest
import dev.onexeor.kdownloader.DownloadState
import dev.onexeor.kdownloader.DownloadTask
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSUserDomainMask
import platform.Foundation.downloadTaskWithRequest
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue

/**
 * iOS implementation of DownloadTask using NSURLSession.
 */
internal class IosDownloadTask(
    override val id: String,
    override val request: DownloadRequest,
    private val fileName: String,
    private val directory: String?,
    private val initialListeners: DownloadListeners
) : DownloadTask {

    private var _currentState: DownloadState = DownloadState.Pending
    override val currentState: DownloadState get() = _currentState

    private var _currentProgress: DownloadProgress = DownloadProgress.ZERO
    override val currentProgress: DownloadProgress get() = _currentProgress

    private var downloadTask: NSURLSessionDownloadTask? = null
    private var filePath: String = ""

    private val progressListeners = mutableListOf<(DownloadProgress) -> Unit>()
    private val stateListeners = mutableListOf<(DownloadState) -> Unit>()
    private val completeListeners = mutableListOf<(String) -> Unit>()
    private val errorListeners = mutableListOf<(DownloadError) -> Unit>()

    init {
        initialListeners.onProgress?.let { progressListeners.add(it) }
        initialListeners.onStateChange?.let { stateListeners.add(it) }
        initialListeners.onComplete?.let { completeListeners.add(it) }
        initialListeners.onError?.let { errorListeners.add(it) }
    }

    override fun cancel() {
        downloadTask?.cancel()
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

    fun start() {
        val url = NSURL.URLWithString(request.url)
        if (url == null) {
            val error = DownloadError.InvalidUrl("Invalid URL: ${request.url}")
            updateState(DownloadState.Failed(error))
            notifyError(error)
            return
        }

        val urlRequest = NSMutableURLRequest(uRL = url).apply {
            setHTTPMethod("GET")

            // Add headers
            request.headers.forEach { (key, value) ->
                setValue(value, forHTTPHeaderField = key)
            }

            // Add auth header
            request.auth?.let { auth ->
                val authHeader = when (auth) {
                    is dev.onexeor.kdownloader.Auth.Bearer -> "Bearer ${auth.token}"
                    is dev.onexeor.kdownloader.Auth.Basic -> {
                        val credentials = "${auth.username}:${auth.password}"
                        val encoded = encodeBase64(credentials)
                        "Basic $encoded"
                    }
                }
                setValue(authHeader, forHTTPHeaderField = "Authorization")
            }
        }

        val configuration = NSURLSessionConfiguration.defaultSessionConfiguration
        val session = NSURLSession.sessionWithConfiguration(configuration)

        downloadTask = session.downloadTaskWithRequest(urlRequest) { location, response, error ->
            handleCompletion(location, response, error)
        }

        updateState(DownloadState.Downloading(_currentProgress))
        downloadTask?.resume()
    }

    private fun handleCompletion(location: NSURL?, response: NSURLResponse?, error: NSError?) {
        if (error != null) {
            val downloadError = DownloadError.Network(
                message = error.localizedDescription,
                cause = null
            )
            updateState(DownloadState.Failed(downloadError))
            notifyError(downloadError)
            return
        }

        val httpResponse = response as? NSHTTPURLResponse
        val statusCode = httpResponse?.statusCode?.toInt() ?: 0

        if (statusCode >= 400) {
            val downloadError = DownloadError.Http(
                statusCode = statusCode,
                message = "HTTP error $statusCode"
            )
            updateState(DownloadState.Failed(downloadError))
            notifyError(downloadError)
            return
        }

        if (location == null) {
            val downloadError = DownloadError.Unknown("Download location is null")
            updateState(DownloadState.Failed(downloadError))
            notifyError(downloadError)
            return
        }

        // Move file to documents directory
        val documentsPath = getDocumentsDirectory()
        val targetDir = if (directory != null) "$documentsPath/$directory" else documentsPath
        val targetPath = "$targetDir/$fileName"
        filePath = targetPath

        val fileManager = NSFileManager.defaultManager

        // Create directory if needed
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            fileManager.createDirectoryAtPath(
                path = targetDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = errorPtr.ptr
            )
        }

        // Remove existing file if overwrite is enabled
        if (request.overwrite && fileManager.fileExistsAtPath(targetPath)) {
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                fileManager.removeItemAtPath(targetPath, errorPtr.ptr)
            }
        }

        // Move downloaded file to target location
        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            val destinationUrl = NSURL.fileURLWithPath(targetPath)

            val success = fileManager.moveItemAtURL(
                srcURL = location,
                toURL = destinationUrl,
                error = errorPtr.ptr
            )

            if (success) {
                _currentProgress = DownloadProgress(
                    bytesDownloaded = getFileSize(targetPath),
                    totalBytes = getFileSize(targetPath),
                    percentage = 100
                )
                updateState(DownloadState.Completed(targetPath))
                notifyComplete(targetPath)
            } else {
                val moveError = errorPtr.value
                val downloadError = DownloadError.Storage(
                    message = moveError?.localizedDescription ?: "Failed to save file",
                    cause = null
                )
                updateState(DownloadState.Failed(downloadError))
                notifyError(downloadError)
            }
        }
    }

    private fun getDocumentsDirectory(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )
        return paths.firstOrNull() as? String ?: ""
    }

    private fun getFileSize(path: String): Long {
        return try {
            val fileManager = NSFileManager.defaultManager
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val attributes = fileManager.attributesOfItemAtPath(path, errorPtr.ptr)
                (attributes?.get("NSFileSize") as? Number)?.toLong() ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun encodeBase64(input: String): String {
        // Simple base64 encoding implementation
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val bytes = input.encodeToByteArray()
        val result = StringBuilder()

        var i = 0
        while (i < bytes.size) {
            val b0 = bytes[i].toInt() and 0xFF
            val b1 = if (i + 1 < bytes.size) bytes[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < bytes.size) bytes[i + 2].toInt() and 0xFF else 0

            result.append(base64Chars[b0 shr 2])
            result.append(base64Chars[((b0 and 0x03) shl 4) or (b1 shr 4)])

            if (i + 1 < bytes.size) {
                result.append(base64Chars[((b1 and 0x0F) shl 2) or (b2 shr 6)])
            } else {
                result.append('=')
            }

            if (i + 2 < bytes.size) {
                result.append(base64Chars[b2 and 0x3F])
            } else {
                result.append('=')
            }

            i += 3
        }

        return result.toString()
    }

    private fun updateState(newState: DownloadState) {
        if (_currentState != newState) {
            _currentState = newState
            notifyState(newState)
        }
    }

    private fun notifyProgress(progress: DownloadProgress) {
        progressListeners.forEach { it(progress) }
    }

    private fun notifyState(state: DownloadState) {
        stateListeners.forEach { it(state) }
    }

    private fun notifyComplete(path: String) {
        completeListeners.forEach { it(path) }
    }

    private fun notifyError(error: DownloadError) {
        errorListeners.forEach { it(error) }
    }
}
