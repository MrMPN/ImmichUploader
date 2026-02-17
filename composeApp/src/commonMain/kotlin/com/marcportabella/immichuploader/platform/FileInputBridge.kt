package com.marcportabella.immichuploader.platform

import androidx.compose.runtime.Composable
import com.marcportabella.immichuploader.domain.LocalIntakeFile

@Composable
expect fun BindPlatformFileInput(
    onFilesSelected: suspend (List<LocalIntakeFile>) -> Unit
)

expect fun openPlatformFilePicker()

expect fun revokePlatformPreviewUrl(url: String)
