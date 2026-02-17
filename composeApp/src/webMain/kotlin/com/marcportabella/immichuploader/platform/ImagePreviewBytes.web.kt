package com.marcportabella.immichuploader.platform

import io.github.vinceglb.filekit.utils.toJsArray
import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL
import org.w3c.files.File
import kotlin.coroutines.resume
import kotlin.math.roundToInt

actual suspend fun createPreviewBytes(
    originalBytes: ByteArray,
    mimeType: String,
    maxDimension: Int
): ByteArray? {
    if (!mimeType.startsWith("image/")) return null
    if (originalBytes.isEmpty()) return null

    return downscaleImageBytesWithCanvas(
        originalBytes = originalBytes,
        outputMimeType = if (mimeType.equals("image/png", ignoreCase = true)) "image/png" else "image/jpeg",
        maxDimension = maxDimension
    ) ?: originalBytes
}

private suspend fun downscaleImageBytesWithCanvas(
    originalBytes: ByteArray,
    outputMimeType: String,
    maxDimension: Int
): ByteArray? = suspendCancellableCoroutine { continuation ->
    val image = document.createElement("img") as? HTMLImageElement
    if (image == null) {
        continuation.resume(null)
        return@suspendCancellableCoroutine
    }

    val objectUrl = createObjectUrlForBytes(originalBytes)
    if (objectUrl == null) {
        continuation.resume(null)
        return@suspendCancellableCoroutine
    }

    fun finish(value: ByteArray?) {
        runCatching { URL.revokeObjectURL(objectUrl) }
        if (continuation.isActive) continuation.resume(value)
    }

    image.onload = {
        val width = image.naturalWidth.takeIf { it > 0 } ?: image.width
        val height = image.naturalHeight.takeIf { it > 0 } ?: image.height

        val result = if (width <= 0 || height <= 0) {
            null
        } else {
            val scale = minOf(1.0, maxDimension.toDouble() / maxOf(width, height).toDouble())
            val targetWidth = maxOf(1, (width * scale).roundToInt())
            val targetHeight = maxOf(1, (height * scale).roundToInt())

            val canvas = document.createElement("canvas") as? HTMLCanvasElement
            val context = canvas?.getContext("2d") as? CanvasRenderingContext2D
            if (canvas == null || context == null) {
                null
            } else {
                canvas.width = targetWidth
                canvas.height = targetHeight
                context.drawImage(image, 0.0, 0.0, targetWidth.toDouble(), targetHeight.toDouble())
                val dataUrl = runCatching { canvas.toDataURL(outputMimeType) }.getOrNull()
                dataUrl?.let(::decodeDataUrlToBytes)
            }
        }

        finish(result)
    }

    val failListener: (Event) -> Unit = { finish(null) }
    image.addEventListener("error", failListener)
    image.addEventListener("abort", failListener)
    image.src = objectUrl
}

private fun createObjectUrlForBytes(bytes: ByteArray): String? {
    val file = File(
        fileBits = bytes.toJsArray(),
        fileName = "preview"
    )
    return runCatching { URL.createObjectURL(file) }.getOrNull()
}

private fun decodeDataUrlToBytes(dataUrl: String): ByteArray? {
    val commaIndex = dataUrl.indexOf(',')
    if (commaIndex <= 0 || commaIndex >= dataUrl.length - 1) return null

    val base64Part = dataUrl.substring(commaIndex + 1)
    val binary = runCatching { atob(base64Part) }.getOrNull() ?: return null
    return ByteArray(binary.length) { index -> binary[index].code.toByte() }
}

private external fun atob(value: String): String
