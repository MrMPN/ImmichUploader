package com.marcportabella.immichuploader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File

@Composable
fun App() {
    MaterialTheme {
        val store = remember { UploadPrepStore() }
        val state = store.state

        DisposableEffect(store) {
            val input = document.getElementById("local-file-input") as? HTMLInputElement
            if (input == null) {
                onDispose { }
            } else {
                val listener: (dynamic) -> Unit = {
                    val fileList = input.files
                    if (fileList != null) {
                        val nextFiles = mutableListOf<LocalIntakeFile>()
                        for (index in 0 until fileList.length) {
                            val file = fileList.item(index) ?: continue
                            nextFiles += file.toLocalIntakeFile()
                        }

                        store.state.assets.values.mapNotNull { it.previewUrl }.forEach {
                            revokeObjectUrl(it)
                        }

                        val assets = mapLocalIntakeFilesToAssets(nextFiles)
                        store.dispatch(UploadPrepAction.ReplaceAssets(assets))
                        store.dispatch(UploadPrepAction.ClearSelection)
                        input.value = ""
                    }
                }

                input.addEventListener("change", listener)
                onDispose {
                    input.removeEventListener("change", listener)
                    store.state.assets.values.mapNotNull { it.previewUrl }.forEach {
                        revokeObjectUrl(it)
                    }
                }
            }
        }

        val stagedBulkRequest = remember(state.selectedAssetIds, state.stagedEditsByAssetId) {
            val selection = state.selectedAssetIds.map { "remote-${it.value}" }.toSet()
            val combinedPatch = state.selectedAssetIds
                .mapNotNull { state.stagedEditsByAssetId[it] }
                .fold<AssetEditPatch, AssetEditPatch?>(null) { acc, patch -> if (acc == null) patch else acc.merge(patch) }

            if (combinedPatch == null) {
                null
            } else {
                ImmichRequestBuilder.buildBulkMetadataRequest(selection, combinedPatch)
            }
        }

        val transport = remember { ApiKeyGatedImmichTransport(DryRunImmichTransport()) }
        val gateStatus = transport.gateStatus(apiKey = null)

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Immich Upload Prep")
            Text("Assets loaded: ${state.assets.size}")
            Text("Selected: ${state.selectedAssetIds.size}")
            Text("Staged edits: ${state.stagedEditsByAssetId.size}")
            Text("Bulk metadata request ready: ${stagedBulkRequest != null}")
            Text("Transport gate: $gateStatus")

            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = {
                    val input = document.getElementById("local-file-input") as? HTMLInputElement
                    input?.click()
                }
            ) {
                Text("Select local media")
            }

            if (state.assets.isEmpty()) {
                Text("No files selected yet.")
            } else {
                Text("Queue")
                state.assets.values.sortedBy { it.fileName }.forEach { asset ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(asset.fileName)
                            Text("${asset.mimeType} · ${asset.fileSizeBytes} bytes")
                        }
                        Text(if (asset.previewUrl == null) "No preview" else "Preview ready")
                    }
                }
            }
        }
    }
}

private fun File.toLocalIntakeFile(): LocalIntakeFile {
    val previewUrl = if (type.startsWith("image/") || type.startsWith("video/")) {
        createObjectUrl(this)
    } else {
        null
    }

    return LocalIntakeFile(
        name = name,
        type = type,
        size = size.toLong(),
        lastModifiedEpochMillis = lastModified.toLong(),
        previewUrl = previewUrl
    )
}

private fun createObjectUrl(file: File): String? =
    runCatching { js("URL.createObjectURL(file)") as String }.getOrNull()

private fun revokeObjectUrl(url: String) {
    runCatching { js("URL.revokeObjectURL(url)") }
}
