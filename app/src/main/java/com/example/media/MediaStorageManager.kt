package com.example.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

data class MediaSaveResult(
    val success: Boolean,
    val contentUri: Uri? = null,
    val filePath: String = "",
    val fileName: String = "",
    val errorMessage: String? = null
)

class MediaStorageManager(private val context: Context) {

    /**
     * Save generated bitmap image to MediaStore (Pictures/MOTO_AI)
     */
    suspend fun saveImageToGallery(
        bitmap: Bitmap,
        baseName: String = "MOTO_AI_IMG"
    ): MediaSaveResult = withContext(Dispatchers.IO) {
        val fileName = "${baseName}_${System.currentTimeMillis()}.jpg"
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MOTO_AI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        try {
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext MediaSaveResult(false, errorMessage = "Failed to allocate MediaStore entry.")

            resolver.openOutputStream(imageUri)?.use { outStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)) {
                    return@withContext MediaSaveResult(false, errorMessage = "Failed to compress and save image.")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            val displayPath = "Pictures/MOTO_AI/$fileName"
            MediaSaveResult(
                success = true,
                contentUri = imageUri,
                filePath = displayPath,
                fileName = fileName
            )
        } catch (e: Exception) {
            MediaSaveResult(false, errorMessage = "Error saving to Gallery: ${e.localizedMessage}")
        }
    }

    /**
     * Save video from base64 string or remote verified Google Generative AI URI to Movies/MOTO_AI
     */
    suspend fun saveVideoToGallery(
        videoUriOrUrl: String?,
        base64Bytes: String?,
        baseName: String = "MOTO_AI_VID"
    ): MediaSaveResult = withContext(Dispatchers.IO) {
        val fileName = "${baseName}_${System.currentTimeMillis()}.mp4"
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/MOTO_AI")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        try {
            val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext MediaSaveResult(false, errorMessage = "Failed to allocate MediaStore entry for video.")

            resolver.openOutputStream(videoUri)?.use { outStream ->
                if (!base64Bytes.isNullOrBlank()) {
                    val decodedBytes = Base64.decode(base64Bytes, Base64.DEFAULT)
                    outStream.write(decodedBytes)
                } else if (!videoUriOrUrl.isNullOrBlank()) {
                    // Validate URL before streaming
                    val url = URL(videoUriOrUrl)
                    val host = url.host.lowercase()
                    if (!host.endsWith("googleapis.com") && !host.endsWith("google.com") && !host.endsWith("googleusercontent.com")) {
                        return@withContext MediaSaveResult(false, errorMessage = "Blocked download from unverified remote host: $host")
                    }
                    val connection = url.openConnection()
                    connection.connectTimeout = 30000
                    connection.readTimeout = 60000
                    connection.getInputStream().use { inStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inStream.read(buffer).also { bytesRead = it } != -1) {
                            outStream.write(buffer, 0, bytesRead)
                        }
                    }
                } else {
                    return@withContext MediaSaveResult(false, errorMessage = "No video content provided.")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(videoUri, contentValues, null, null)
            }

            val displayPath = "Movies/MOTO_AI/$fileName"
            MediaSaveResult(
                success = true,
                contentUri = videoUri,
                filePath = displayPath,
                fileName = fileName
            )
        } catch (e: Exception) {
            MediaSaveResult(false, errorMessage = "Error saving video to Gallery: ${e.localizedMessage}")
        }
    }

    /**
     * Cache bitmap locally for chat messages
     */
    suspend fun cacheBitmap(bitmap: Bitmap, prefix: String = "chat_attach"): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file
    }

    /**
     * Share Media URI using standard Android Intent
     */
    fun shareMedia(uri: Uri, mimeType: String, subject: String = "Shared from MOTO AI") {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share via")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
