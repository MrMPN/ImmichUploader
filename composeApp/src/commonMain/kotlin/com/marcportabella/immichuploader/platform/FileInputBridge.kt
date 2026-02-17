package com.marcportabella.immichuploader.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.marcportabella.immichuploader.domain.LocalIntakeFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch

private var launchPlatformPicker: (() -> Unit)? = null

@Composable
fun BindPlatformFileInput(
    onFilesSelected: suspend (List<LocalIntakeFile>) -> Unit
) {
    val scope = rememberCoroutineScope()
    val launcher = rememberFilePickerLauncher(
        mode = FileKitMode.Multiple(),
        type = FileKitType.ImageAndVideo
    ) { files ->
        scope.launch {
            val intakeFiles = files
                .orEmpty()
                .mapNotNull { file ->
                    runCatching { file.toLocalIntakeFile() }
                        .onFailure { throwable ->
                            platformLogError(
                                "[immichuploader][files] failed to process file: ${throwable.diagnosticMessage()}"
                            )
                        }
                        .getOrNull()
                }
            onFilesSelected(intakeFiles)
        }
    }

    DisposableEffect(launcher) {
        launchPlatformPicker = { launcher.launch() }
        onDispose {
            launchPlatformPicker = null
        }
    }
}

fun openPlatformFilePicker() {
    launchPlatformPicker?.invoke()
}

fun revokePlatformPreviewUrl(url: String) {
    // FileKit-backed intake does not allocate browser object URLs.
}
