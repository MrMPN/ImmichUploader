package com.marcportabella.immichuploader.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.marcportabella.immichuploader.platform.initializePlatformLogging

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initializePlatformLogging()
    ComposeViewport {
        App()
    }
}
