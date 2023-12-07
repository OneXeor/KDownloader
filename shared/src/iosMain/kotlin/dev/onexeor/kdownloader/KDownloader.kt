@file:OptIn(ExperimentalForeignApi::class)

package dev.onexeor.kdownloader

import kotlin.system.getTimeMillis
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionDownloadTask
import platform.Foundation.NSUserDomainMask
import platform.Foundation.downloadTaskWithURL
import platform.Foundation.lastPathComponent

actual class KDownloader {

    private val tasks = mutableMapOf<Long, NSURLSessionDownloadTask>()

    /**
     *
     */
    actual fun cancelDownloadById(downloadId: Long) {
        tasks[downloadId]?.cancel()
    }

    /**
     *
     */
    actual fun getMimeTypeById(downloadId: Long): String? {
        return tasks[downloadId]?.originalRequest?.URL?.lastPathComponent
    }

    /**
     *
     */
    actual fun getUrlById(downloadId: Long): String? {
        return tasks[downloadId]?.originalRequest?.URL.toString()
    }

    /**
     * @param url http or https url
     * @param fileName if null will be [System.currentTimeMillis].txt
     * @param progressListener can be nullable, consist of [Uri] to file and downloading status, see [android.app.DownloadManager] constants
     *
     * @return download id
     */
    actual fun downloadFile(
        url: String,
        fileName: String?,
        progressListener: ((String, Int) -> Unit)?,
        errorListener: ((DownloadError) -> Unit)?
    ): Long {
        fun complete(url: NSURL?, response: NSURLResponse?, error: NSError?) {
            val httpResponse = response as? NSHTTPURLResponse
            val httpStatusCode = httpResponse?.statusCode?.toInt() ?: -1
            if (error != null) {
                errorListener?.invoke(
                    DownloadError(url.toString(), -1, "Error while downloading", httpStatusCode)
                )
                return
            }

            if (url == null) {
                errorListener?.invoke(DownloadError("NULL", -1, "URL is null", httpStatusCode))
                return
            }

            val documentsDirectory = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            ).first()
            val fileManager = NSFileManager.defaultManager
            val writablePath = "$documentsDirectory/$fileName"
            memScoped {
                val pointer = alloc<ObjCObjectVar<NSError?>>()

                try {
                    fileManager.moveItemAtURL(
                        srcURL = url,
                        toURL = NSURL(string = writablePath),
                        error = pointer.ptr
                    )
                    progressListener?.invoke(url.toString(), 1)
                } catch (e: Exception) {
                    e.printStackTrace()
                    errorListener?.invoke(
                        DownloadError(url.toString(), -1, "Error while file saving", httpStatusCode)
                    )
                }
            }
        }

        val downloadTask = NSURLSession.sharedSession.downloadTaskWithURL(
            url = NSURL(string = url),
            completionHandler = ::complete
        )
        val id = getTimeMillis()
        tasks[id] = downloadTask

        downloadTask.resume()

        return id
    }
}
