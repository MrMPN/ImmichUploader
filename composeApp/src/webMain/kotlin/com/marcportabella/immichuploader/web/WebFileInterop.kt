package com.marcportabella.immichuploader.web

import com.marcportabella.immichuploader.domain.LocalIntakeFile
import org.w3c.files.File

fun File.toLocalIntakeFile(): LocalIntakeFile {
    val previewUrl = if (type.startsWith("image/") || type.startsWith("video/")) {
        createObjectUrl(this)
    } else {
        null
    }

    return LocalIntakeFile(
        name = name,
        type = type,
        size = size.toString().substringBefore('.').toLongOrNull() ?: 0L,
        lastModifiedEpochMillis = lastModified.toString().substringBefore('.').toLongOrNull() ?: 0L,
        previewUrl = previewUrl
    )
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
