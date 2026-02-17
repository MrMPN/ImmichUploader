package com.marcportabella.immichuploader.platform

import com.marcportabella.immichuploader.domain.LocalIntakeFile
import com.marcportabella.immichuploader.domain.parseJpegExifMetadata
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.mimeType
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size

suspend fun PlatformFile.toLocalIntakeFile(): LocalIntakeFile {
    val mimeType = mimeType()?.toString()?.ifBlank { null } ?: inferMimeTypeFromName(name)
    val bytes = runCatching { readBytes() }.getOrDefault(ByteArray(0))

    val previewBytes = if (mimeType.startsWith("image/")) {
        createPreviewBytes(
            originalBytes = bytes,
            mimeType = mimeType,
            maxDimension = 256
        )
    } else {
        null
    }

    val exifMetadata = if (mimeType.equals("image/jpeg", ignoreCase = true) || mimeType.equals("image/jpg", ignoreCase = true)) {
        parseJpegExifMetadata(bytes)
    } else {
        null
    }

    return LocalIntakeFile(
        name = name,
        type = mimeType,
        size = runCatching { size() }.getOrDefault(0L),
        lastModifiedEpochMillis = 0L,
        previewUrl = null,
        previewBytes = previewBytes,
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

private fun inferMimeTypeFromName(fileName: String): String {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return when (extension) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "heic", "heif" -> "image/heic"
        "mp4" -> "video/mp4"
        "mov" -> "video/quicktime"
        "m4v" -> "video/x-m4v"
        "webm" -> "video/webm"
        "avi" -> "video/x-msvideo"
        "mkv" -> "video/x-matroska"
        else -> "application/octet-stream"
    }
}
