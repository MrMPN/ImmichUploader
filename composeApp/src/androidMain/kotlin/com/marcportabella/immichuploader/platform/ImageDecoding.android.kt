package com.marcportabella.immichuploader.platform

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodePreviewBitmap(bytes: ByteArray): ImageBitmap? {
    val bitmap = runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }.getOrNull()
    return bitmap?.asImageBitmap()
}
