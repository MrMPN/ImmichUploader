package com.marcportabella.immichuploader.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.ui.UploadPrepScreen

@Composable
fun App() {
    MaterialTheme {
        val store = remember { UploadPrepStore() }
        UploadPrepScreen(store)
    }
}
