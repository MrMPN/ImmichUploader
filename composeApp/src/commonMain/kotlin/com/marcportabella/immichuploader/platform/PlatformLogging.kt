package com.marcportabella.immichuploader.platform

import de.halfbit.logger.LoggableLevel
import de.halfbit.logger.LoggerBuilder
import de.halfbit.logger.i
import de.halfbit.logger.e
import de.halfbit.logger.initializeLogger

private const val PLATFORM_LOG_TAG = "ImmichUploader"
private var loggingInitialized: Boolean = false

internal expect fun LoggerBuilder.registerPlatformLoggerSink()

fun initializePlatformLogging(level: LoggableLevel = LoggableLevel.Everything) {
    if (loggingInitialized) return
    initializeLogger {
        registerPlatformLoggerSink()
        loggableLevel = level
    }
    loggingInitialized = true
}

fun platformLogInfo(message: String) {
    i(PLATFORM_LOG_TAG) { message }
}

fun platformLogError(message: String) {
    e(PLATFORM_LOG_TAG) { message }
}

fun Throwable.diagnosticMessage(): String {
    val parts = mutableListOf<String>()
    parts += this::class.simpleName ?: "Throwable"
    if (!message.isNullOrBlank()) {
        parts += message!!
    }

    var current: Throwable? = cause
    while (current != null) {
        val type = current::class.simpleName ?: "Throwable"
        val msg = current.message
        parts += if (msg.isNullOrBlank()) "caused by $type" else "caused by $type: $msg"
        current = current.cause
    }

    return parts.joinToString(" | ")
}
