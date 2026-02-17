package com.marcportabella.immichuploader.platform

import de.halfbit.logger.LoggerBuilder
import de.halfbit.logger.sink.android.registerAndroidLogSink

internal actual fun LoggerBuilder.registerPlatformLoggerSink() {
    registerAndroidLogSink()
}
