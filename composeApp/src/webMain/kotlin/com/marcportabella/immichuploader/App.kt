package com.marcportabella.immichuploader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun App() {
    MaterialTheme {
        val store = remember { UploadPrepStore() }
        UploadPrepScreen(store)
    }
}
