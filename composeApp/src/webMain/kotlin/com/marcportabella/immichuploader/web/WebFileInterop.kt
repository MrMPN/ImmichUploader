package com.marcportabella.immichuploader.web

import com.marcportabella.immichuploader.domain.LocalIntakeFile
import com.marcportabella.immichuploader.domain.parseJpegExifMetadata
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume

suspend fun File.toLocalIntakeFile(): LocalIntakeFile {
    val previewUrl = if (type.startsWith("image/") || type.startsWith("video/")) {
        createObjectUrl(this)
    } else {
        null
    }
    val imageBytes = if (type.startsWith("image/")) readBytes() else null
    val exifMetadata = if (type.equals("image/jpeg", ignoreCase = true) || type.equals("image/jpg", ignoreCase = true)) {
        imageBytes?.let(::parseJpegExifMetadata)
    } else {
        null
    }

    return LocalIntakeFile(
        name = name,
        type = type,
        size = size.toString().substringBefore('.').toLongOrNull() ?: 0L,
        lastModifiedEpochMillis = lastModified.toString().substringBefore('.').toLongOrNull() ?: 0L,
        previewUrl = previewUrl,
        previewBytes = imageBytes,
        captureDateTime = exifMetadata?.captureDateTime,
        timeZone = exifMetadata?.timeZone,
        cameraMake = exifMetadata?.cameraMake,
        cameraModel = exifMetadata?.cameraModel,
        exifMetadata = exifMetadata?.metadata ?: emptyMap(),
        exifSummary = exifMetadata
            ?.metadata
            ?.takeIf { it.isNotEmpty() }
            ?.entries
            ?.joinToString(" · ") { "${it.key}=${it.value}" }
    )
}

private suspend fun File.readBytes(): ByteArray? {
    return suspendCancellableCoroutine { continuation ->
        val reader = FileReader()
        val resumeWithNull = {
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }

        reader.onload = {
            if (continuation.isActive) {
                val result = reader.result as? ArrayBuffer
                if (result == null) {
                    continuation.resume(null)
                } else {
                    val view = DataView(result)
                    val bytes = ByteArray(result.byteLength) { index -> view.getInt8(index) }
                    continuation.resume(bytes)
                }
            }
        }
        reader.onerror = { resumeWithNull() }
        reader.onabort = { resumeWithNull() }

        runCatching { reader.readAsArrayBuffer(this) }.onFailure { resumeWithNull() }
    }
}

fun createObjectUrl(file: File): String? =
    runCatching { URL.createObjectURL(file) }.getOrNull()

fun revokeObjectUrl(url: String) {
    runCatching { URL.revokeObjectURL(url) }
}

private external object URL {
    fun createObjectURL(file: File): String
    fun revokeObjectURL(url: String)
}
