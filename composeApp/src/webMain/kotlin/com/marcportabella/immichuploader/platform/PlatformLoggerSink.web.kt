package com.marcportabella.immichuploader.platform

import de.halfbit.logger.LoggerBuilder
import de.halfbit.logger.sink.wasmjs.registerConsoleLogSink

internal actual fun LoggerBuilder.registerPlatformLoggerSink() {
    registerConsoleLogSink()
}
