package com.marcportabella.immichuploader.web

import com.marcportabella.immichuploader.domain.LocalIntakeFile
import com.marcportabella.immichuploader.domain.parseJpegExifMetadata
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.events.Event
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.math.roundToInt

suspend fun File.toLocalIntakeFile(): LocalIntakeFile {
    val previewUrl = if (type.startsWith("image/") || type.startsWith("video/")) {
        createObjectUrl(this)
    } else {
        null
    }

    val previewBytes = if (type.startsWith("image/")) {
        createDownscaledPreviewBytes(maxDimension = 256)
    } else {
        null
    }

    val imageBytesForMetadata = if (type.startsWith("image/")) readBytes() else null
    val exifMetadata = if (type.equals("image/jpeg", ignoreCase = true) || type.equals("image/jpg", ignoreCase = true)) {
        imageBytesForMetadata?.let(::parseJpegExifMetadata)
    } else {
        null
    }

    return LocalIntakeFile(
        name = name,
        type = type,
        size = size.toString().substringBefore('.').toLongOrNull() ?: 0L,
        lastModifiedEpochMillis = lastModified.toString().substringBefore('.').toLongOrNull() ?: 0L,
        previewUrl = previewUrl,
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

private suspend fun File.createDownscaledPreviewBytes(
    maxDimension: Int
): ByteArray? = suspendCancellableCoroutine { continuation ->
    val objectUrl = createObjectUrl(this)
    if (objectUrl == null) {
        continuation.resume(null)
        return@suspendCancellableCoroutine
    }

    val image = document.createElement("img") as? HTMLImageElement
    if (image == null) {
        revokeObjectUrl(objectUrl)
        continuation.resume(null)
        return@suspendCancellableCoroutine
    }

    fun finish(value: ByteArray?) {
        revokeObjectUrl(objectUrl)
        if (continuation.isActive) {
            continuation.resume(value)
        }
    }

    image.onload = {
        val width = image.naturalWidth.toInt().takeIf { it > 0 } ?: image.width
        val height = image.naturalHeight.toInt().takeIf { it > 0 } ?: image.height
        if (width <= 0 || height <= 0) {
            finish(null)
        } else {
            val scale = minOf(1.0, maxDimension.toDouble() / maxOf(width, height).toDouble())
            val targetWidth = maxOf(1, (width * scale).roundToInt())
            val targetHeight = maxOf(1, (height * scale).roundToInt())

            val canvas = document.createElement("canvas") as? HTMLCanvasElement
            val context = canvas?.getContext("2d") as? CanvasRenderingContext2D
            if (canvas == null || context == null) {
                finish(null)
            } else {
                canvas.width = targetWidth
                canvas.height = targetHeight
                context.drawImage(image, 0.0, 0.0, targetWidth.toDouble(), targetHeight.toDouble())

                val dataUrl = runCatching { canvas.toDataURL("image/jpeg") }.getOrNull()
                finish(dataUrl?.let(::decodeDataUrlToBytes))
            }
        }
    }

    val failListener: (Event) -> Unit = { finish(null) }
    image.addEventListener("error", failListener)
    image.addEventListener("abort", failListener)
    image.src = objectUrl
}

private fun decodeDataUrlToBytes(dataUrl: String): ByteArray? {
    val commaIndex = dataUrl.indexOf(',')
    if (commaIndex <= 0 || commaIndex >= dataUrl.length - 1) return null

    val base64Part = dataUrl.substring(commaIndex + 1)
    val binary = runCatching { atob(base64Part) }.getOrNull() ?: return null
    return ByteArray(binary.length) { index -> binary[index].code.toByte() }
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

private external fun atob(value: String): String
