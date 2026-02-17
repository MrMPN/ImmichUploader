package com.marcportabella.immichuploader.platform

import androidx.compose.runtime.Composable
import com.marcportabella.immichuploader.domain.LocalIntakeFile

@Composable
actual fun BindPlatformFileInput(
    onFilesSelected: suspend (List<LocalIntakeFile>) -> Unit
) {
    // Android target is used for previews in this project.
}

actual fun openPlatformFilePicker() {
    // Android target is used for previews in this project.
}

actual fun revokePlatformPreviewUrl(url: String) {
    // Android target is used for previews in this project.
}
