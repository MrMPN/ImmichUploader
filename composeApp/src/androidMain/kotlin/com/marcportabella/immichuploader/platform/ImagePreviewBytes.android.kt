package com.marcportabella.immichuploader.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

actual suspend fun createPreviewBytes(
    originalBytes: ByteArray,
    mimeType: String,
    maxDimension: Int
): ByteArray? {
    if (!mimeType.startsWith("image/")) return null
    if (originalBytes.isEmpty()) return null

    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, boundsOptions)
    val width = boundsOptions.outWidth
    val height = boundsOptions.outHeight
    if (width <= 0 || height <= 0) return originalBytes

    val sampleSize = calculateInSampleSize(width, height, maxDimension)
    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, decodeOptions)
        ?: return originalBytes

    val scale = minOf(1f, maxDimension.toFloat() / maxOf(decoded.width, decoded.height).toFloat())
    val targetWidth = maxOf(1, (decoded.width * scale).toInt())
    val targetHeight = maxOf(1, (decoded.height * scale).toInt())
    val resized = if (decoded.width == targetWidth && decoded.height == targetHeight) {
        decoded
    } else {
        Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true).also {
            if (it !== decoded) decoded.recycle()
        }
    }

    return ByteArrayOutputStream().use { out ->
        val format = if (mimeType.equals("image/png", ignoreCase = true)) {
            Bitmap.CompressFormat.PNG
        } else {
            Bitmap.CompressFormat.JPEG
        }
        resized.compress(format, 85, out)
        resized.recycle()
        out.toByteArray()
    }
}

private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var inSampleSize = 1
    var currentWidth = width
    var currentHeight = height
    while (currentWidth > maxDimension * 2 || currentHeight > maxDimension * 2) {
        inSampleSize *= 2
        currentWidth /= 2
        currentHeight /= 2
    }
    return inSampleSize
}
