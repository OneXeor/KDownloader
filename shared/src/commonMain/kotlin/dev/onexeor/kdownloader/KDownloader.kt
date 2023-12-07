package dev.onexeor.kdownloader

expect class KDownloader {
    /**
     *
     */
    fun cancelDownloadById(downloadId: Long)

    /**
     *
     */
    fun getMimeTypeById(downloadId: Long): String?

    /**
     *
     */
    fun getUrlById(downloadId: Long): String?

    /**
     *
     * @param url http or https url
     * @param fileName if null will be [System.currentTimeMillis].txt
     * @param progressListener can be nullable, consist of [Uri] to file and downloading status, see [android.app.DownloadManager] constants
     *
     * @return download id
     */
    fun downloadFile(
        url: String,
        fileName: String?,
        progressListener: ((String, Int) -> Unit)?,
        errorListener: ((DownloadError) -> Unit)?
    ): Long
}
