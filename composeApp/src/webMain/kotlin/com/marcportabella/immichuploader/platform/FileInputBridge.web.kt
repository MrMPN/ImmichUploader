package com.marcportabella.immichuploader.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.marcportabella.immichuploader.domain.LocalIntakeFile
import com.marcportabella.immichuploader.web.revokeObjectUrl
import com.marcportabella.immichuploader.web.toLocalIntakeFile
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

@Composable
actual fun BindPlatformFileInput(
    onFilesSelected: suspend (List<LocalIntakeFile>) -> Unit
) {
    DisposableEffect(Unit) {
        val input = document.getElementById("local-file-input") as? HTMLInputElement
        if (input == null) {
            onDispose { }
        } else {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val listener: (Event) -> Unit = {
                val fileList = input.files
                if (fileList != null) {
                    scope.launch {
                        val nextFiles = mutableListOf<LocalIntakeFile>()
                        for (index in 0 until fileList.length) {
                            val file = fileList.item(index) ?: continue
                            nextFiles += file.toLocalIntakeFile()
                        }
                        onFilesSelected(nextFiles)
                        input.value = ""
                    }
                }
            }
            input.addEventListener("change", listener)
            onDispose {
                input.removeEventListener("change", listener)
                scope.cancel()
            }
        }
    }
}

actual fun openPlatformFilePicker() {
    val input = document.getElementById("local-file-input") as? HTMLInputElement
    input?.click()
}

actual fun revokePlatformPreviewUrl(url: String) {
    revokeObjectUrl(url)
}
