package dev.onexeor.kdownloader

data class DownloadError(
    val url: String,
    val status: Int,
    val description: String,
    val statusCode: Int
)
