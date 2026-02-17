package com.marcportabella.immichuploader.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.marcportabella.immichuploader.platform.initializePlatformLogging
import com.marcportabella.immichuploader.platform.initializeTimeZoneDatabase

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initializePlatformLogging()
    initializeTimeZoneDatabase()
    ComposeViewport {
        App()
    }
}
