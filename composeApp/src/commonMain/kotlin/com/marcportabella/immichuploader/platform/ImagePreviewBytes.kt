package com.marcportabella.immichuploader.platform

expect suspend fun createPreviewBytes(
    originalBytes: ByteArray,
    mimeType: String,
    maxDimension: Int = 256
): ByteArray?
