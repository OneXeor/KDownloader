package dev.onexeor.kdownloader.extension

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import java.io.File

fun Environment.isExternalStorageWritable(): Boolean {
    return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
}

@Deprecated(
    "Cause in android 10 there is scoped storage",
    replaceWith = ReplaceWith(
        "getFilePath29Api()",
        imports = ["dev.onexeor.download_manager.extension.getFilePath29Api"]
    )
)
fun getFilePath(fileName: String): Uri? {
    val folderName = "Documents"

    var file = File(
        Environment.getExternalStorageDirectory().toString() + "/" + folderName + "/" + fileName
    )
    if (!file.exists()) {
        val rootFolder = File(
            Environment.getExternalStorageDirectory().toString() + "/" + folderName
        )
        rootFolder.mkdirs()
        file = File(
            Environment.getExternalStorageDirectory().toString() + "/" + folderName + "/" + fileName
        )
    }
    return Uri.fromFile(file)
}

fun getFilePath29Api(context: Context, fileName: String): Uri? {
    val folderName = "Documents"

    val externalStorageVolumes: Array<out File> =
        ContextCompat.getExternalFilesDirs(context, null)
    val primaryExternalStorage = externalStorageVolumes[0]

    var file = File("$primaryExternalStorage/$folderName/$fileName")
    if (!file.exists()) {
        val rootFolder = File("$primaryExternalStorage/$folderName")
        rootFolder.mkdirs()
        file = File("$primaryExternalStorage/$folderName/$fileName")
    }
    return Uri.fromFile(file)
}
