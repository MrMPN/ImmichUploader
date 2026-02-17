package com.marcportabella.immichuploader.platform

import androidx.compose.ui.graphics.ImageBitmap

expect fun decodePreviewBitmap(bytes: ByteArray): ImageBitmap?
