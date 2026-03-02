package com.marcportabella.immichuploader.platform

import com.marcportabella.immichuploader.domain.LocalIntakeFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.mimeType
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.size

suspend fun PlatformFile.toLocalIntakeFile(): LocalIntakeFile {
    return toLocalIntakeFileMetadataOnly()
}

suspend fun PlatformFile.toLocalIntakeFileMetadataOnly(): LocalIntakeFile {
    val mimeType = mimeType()?.toString()?.ifBlank { null } ?: inferMimeTypeFromName(name)
    return LocalIntakeFile(
        name = name,
        type = mimeType,
        size = runCatching { size() }.getOrDefault(0L),
        lastModifiedEpochMillis = 0L,
        sourceFile = this,
        previewUrl = null,
        captureDateTime = null,
        timeZone = null,
        cameraMake = null,
        cameraModel = null,
        checksum = null,
        exifMetadata = emptyMap(),
        exifSummary = null
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
