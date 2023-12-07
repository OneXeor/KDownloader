package dev.onexeor.kdownloader

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.database.getIntOrNull
import androidx.core.database.getStringOrNull
import dev.onexeor.kdownloader.auth.Auth
import dev.onexeor.kdownloader.extension.getFilePath
import dev.onexeor.kdownloader.extension.getFilePath29Api
import dev.onexeor.kdownloader.extension.isExternalStorageWritable
import kotlin.properties.Delegates

actual class KDownloader {

    private var context: Context by Delegates.notNull()
    private val downloadService by lazy { context.getSystemService(DownloadManager::class.java) }
    private var handler: Handler
    private var statusHandler: Handler = Handler(Looper.getMainLooper())

    init {
        HandlerThread(this::class.java.canonicalName).apply {
            start()
            handler = Handler(looper)
        }
    }

    /**
     * Needs to be invoked at the Android side, preferably in the `onCreate` method of the Application class
     *
     * @param context
     */
    fun setContext(context: Context) {
        this.context = context
    }

    actual fun cancelDownloadById(downloadId: Long) {
        downloadService.remove(downloadId)
    }

    actual fun getMimeTypeById(downloadId: Long): String? {
        return downloadService.getMimeTypeForDownloadedFile(downloadId)
    }

    actual fun getUrlById(downloadId: Long): String? {
        return downloadService.getUriForDownloadedFile(downloadId)?.toString()
    }

    fun openDownloadById(downloadId: Long): ParcelFileDescriptor? {
        return downloadService.openDownloadedFile(downloadId)
    }

    actual fun downloadFile(
        url: String,
        fileName: String?,
        progressListener: ((String, Int) -> Unit)?,
        errorListener: ((DownloadError) -> Unit)?
    ): Long {
        val defaultAuth = Auth.BasicAuth("", "")

        val extOut = when {
            fileName != null -> fileName.substringAfterLast(".", "")
            else -> ".txt"
        }
        val mimeTypeOut = when {
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extOut) ?: DEFAULT_MIME_TYPE
        }

        return startDownloadManager(
            url = url,
            fileName = fileName ?: "${System.currentTimeMillis()}.txt",
            auth = defaultAuth,
            mimeType = mimeTypeOut,
            progressListener = progressListener,
            errorListener = errorListener
        )
    }

    private fun startDownloadManager(
        url: String,
        downloadDialogTitle: String? = null,
        downloadDialogDescription: String? = null,
        fileName: String,
        normalizedCookies: String? = null,
        mimeType: String,
        auth: Auth = Auth.BasicAuth("", ""),
        downloadDeclineListener: (() -> Unit)? = null,
        progressListener: ((String, Int) -> Unit)? = null,
        errorListener: ((DownloadError) -> Unit)? = null
    ): Long {
        val headerCredentials = when (auth) {
            is Auth.BasicAuth -> "Basic " + Base64.encodeToString(
                "${auth.login}:${auth.password}".toByteArray(),
                Base64.NO_WRAP
            )

            is Auth.TokenAuth -> auth.token
        }

        if (!Environment().isExternalStorageWritable()) {
            downloadDeclineListener?.invoke()
            return -1
        }

        val newFileName = fileName.replace(
            Regex("[^a-zA-Z0-9À-ÿ.\\s]+"),
            " "
        )
        val downloadUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getFilePath29Api(context, newFileName)
        } else {
            getFilePath(newFileName)
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(downloadDialogTitle ?: context.getString(R.string.app_name))
            .setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE
            )
            .setMimeType(mimeType)
            .addRequestHeader("Authorization", headerCredentials)
            .addRequestHeader("Cookie", normalizedCookies)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDescription(
                downloadDialogDescription ?: String.format(
                    context.getString(R.string.download_dialog_description),
                    fileName
                )
            )
            .setDestinationUri(downloadUri)

        val dwnID = downloadService.enqueue(request)

        downloadUri?.let {
            this.getDownloadStatus(dwnID, it, progressListener, errorListener)
        }
        return dwnID
    }

    private fun getDownloadStatus(
        dwnID: Long,
        downloadUri: Uri,
        progressListener: ((String, Int) -> Unit)?,
        errorListener: ((DownloadError) -> Unit)? = null
    ) {
        handler.removeCallbacksAndMessages(null)
        handler.post {
            try {
                var downloading = true
                while (downloading) {
                    val query = DownloadManager.Query().setFilterById(dwnID)
                    val cursor: Cursor = downloadService.query(query)
                    if (cursor.moveToFirst()) {
                        val columnDescriptionIdx = cursor.getColumnIndex(
                            DownloadManager.COLUMN_DESCRIPTION
                        )
                        val columnReasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val columnStatusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val dwnStatus = cursor.getIntOrNull(columnStatusIdx)
                        val dwnDescription = cursor.getStringOrNull(columnDescriptionIdx)
                        val dwnHttpStatusCode = cursor.getIntOrNull(columnReasonIdx)

                        statusHandler.post {
                            when (dwnStatus) {
                                DownloadManager.STATUS_FAILED -> {
                                    errorListener?.invoke(
                                        DownloadError(
                                            url = downloadUri.toString(),
                                            status = dwnStatus,
                                            description = dwnDescription.orEmpty(),
                                            statusCode = dwnHttpStatusCode ?: -1
                                        )
                                    )
                                    progressListener?.invoke(downloadUri.toString(), dwnStatus)
                                }

                                DownloadManager.STATUS_PAUSED,
                                DownloadManager.STATUS_SUCCESSFUL,
                                DownloadManager.STATUS_PENDING
                                -> progressListener?.invoke(downloadUri.toString(), dwnStatus)
                            }
                        }
                        if (dwnStatus == DownloadManager.STATUS_SUCCESSFUL ||
                            dwnStatus == DownloadManager.STATUS_FAILED
                        ) {
                            downloading = false
                        }
                    }

                    Log.d(
                        KDownloader::class.simpleName,
                        "Download status: " + statusMessage(cursor)
                    )
                    cursor.close()
                }
            } catch (e: Exception) {
                statusHandler.post {
                    progressListener?.invoke(
                        downloadUri.toString(),
                        DownloadManager.STATUS_FAILED
                    )
                }
                e.printStackTrace()
                Log.e(
                    KDownloader::class.simpleName,
                    "Error while downloading file: " + e.message
                )
            }
        }
    }

    companion object {
        private const val DEFAULT_MIME_TYPE = "application/*"
    }

    private fun statusMessage(c: Cursor): String {
        val columnStatusIdx = c.getColumnIndex(DownloadManager.COLUMN_STATUS)
        return when (c.getInt(columnStatusIdx)) {
            DownloadManager.STATUS_FAILED -> "Download failed!"
            DownloadManager.STATUS_PAUSED -> "Download paused!"
            DownloadManager.STATUS_PENDING -> "Download pending!"
            DownloadManager.STATUS_RUNNING -> "Download in progress!"
            DownloadManager.STATUS_SUCCESSFUL -> "Download complete!"
            else -> "Download is nowhere in sight"
        }
    }
}
