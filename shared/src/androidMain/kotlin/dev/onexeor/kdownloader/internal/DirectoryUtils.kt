package dev.onexeor.kdownloader.internal

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Get download directory URI based on the specified directory and file name.
 * Handles scoped storage for Android 10+.
 */
internal fun getDownloadDirectory(
    context: Context,
    directory: String?,
    fileName: String
): Uri? {
    val targetDir = directory ?: "Downloads"

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Android 10+ with scoped storage
        getDownloadPathScoped(context, targetDir, fileName)
    } else {
        // Legacy storage
        getDownloadPathLegacy(targetDir, fileName)
    }
}

private fun getDownloadPathScoped(
    context: Context,
    directory: String,
    fileName: String
): Uri? {
    val externalStorageVolumes = ContextCompat.getExternalFilesDirs(context, null)
    if (externalStorageVolumes.isEmpty()) return null

    val primaryExternalStorage = externalStorageVolumes[0] ?: return null

    val folder = File(primaryExternalStorage, directory)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    val file = File(folder, fileName)
    return Uri.fromFile(file)
}

@Suppress("DEPRECATION")
private fun getDownloadPathLegacy(
    directory: String,
    fileName: String
): Uri? {
    val externalStorage = Environment.getExternalStorageDirectory()

    val folder = File(externalStorage, directory)
    if (!folder.exists()) {
        folder.mkdirs()
    }

    val file = File(folder, fileName)
    return Uri.fromFile(file)
}
