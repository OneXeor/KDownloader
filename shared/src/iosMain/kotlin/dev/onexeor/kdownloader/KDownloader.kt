@file:OptIn(ExperimentalForeignApi::class)

package dev.onexeor.kdownloader

import dev.onexeor.kdownloader.internal.IosDownloadTask
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSUUID

/**
 * iOS implementation of KDownloader using NSURLSession.
 */
actual class KDownloader actual constructor(
    private val config: KDownloaderConfig
) {
    private val tasks = mutableMapOf<String, IosDownloadTask>()

    actual fun download(url: String, builder: DownloadRequestBuilder.() -> Unit): DownloadTask {
        val request = DownloadRequestBuilder(url).apply(builder).build()
        return download(request)
    }

    actual fun download(request: DownloadRequest): DownloadTask {
        val taskId = NSUUID().UUIDString

        // Determine file name
        val fileName = request.fileName ?: extractFileNameFromUrl(request.url)

        // Determine directory
        val directory = request.directory ?: config.defaultDirectory

        // Create task
        val task = IosDownloadTask(
            id = taskId,
            request = request,
            fileName = fileName,
            directory = directory,
            initialListeners = request.listeners
        )

        tasks[taskId] = task

        // Start download
        task.start()

        return task
    }

    actual fun getTask(id: String): DownloadTask? = tasks[id]

    actual fun cancelAll() {
        tasks.values.forEach { it.cancel() }
        tasks.clear()
    }

    private fun extractFileNameFromUrl(url: String): String {
        return try {
            val lastSegment = url.substringAfterLast("/")
            if (lastSegment.isNotBlank() && lastSegment.contains(".")) {
                lastSegment.substringBefore("?")
            } else {
                "${NSUUID().UUIDString}.bin"
            }
        } catch (e: Exception) {
            "${NSUUID().UUIDString}.bin"
        }
    }
}
