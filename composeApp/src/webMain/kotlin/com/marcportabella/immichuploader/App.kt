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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = {
                        val input = document.getElementById("local-file-input") as? HTMLInputElement
                        input?.click()
                    }
                ) {
                    Text("Select local media")
                }

                Button(
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = { store.dispatch(UploadPrepAction.SelectAll) },
                    enabled = state.assets.isNotEmpty()
                ) {
                    Text("Select all")
                }

                Button(
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = { store.dispatch(UploadPrepAction.ClearSelection) },
                    enabled = state.selectedAssetIds.isNotEmpty()
                ) {
                    Text("Clear selection")
                }
            }

            BulkEditSection(
                draft = state.bulkEditDraft,
                selectedCount = state.selectedAssetIds.size,
                onDraftChange = { store.dispatch(UploadPrepAction.SetBulkEditDraft(it)) },
                onApply = { store.dispatch(UploadPrepAction.ApplyBulkEditDraftToSelected) },
                onClearDraft = { store.dispatch(UploadPrepAction.ClearBulkEditDraft) },
                onClearSelectedStaged = { store.dispatch(UploadPrepAction.ClearStagedForSelected) }
            )

            if (state.assets.isEmpty()) {
                Text("No files selected yet.")
            } else {
                Text("Queue")
                state.assets.values.sortedBy { it.fileName }.forEach { asset ->
                    val patch = state.stagedEditsByAssetId[asset.id]
                    val metadata = asset.toDisplayMetadata(patch)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = asset.id in state.selectedAssetIds,
                            onCheckedChange = { store.dispatch(UploadPrepAction.ToggleSelection(asset.id)) }
                        )
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(asset.fileName)
                            Text("${asset.mimeType} · ${asset.fileSizeBytes} bytes")
                            Text("Date/time: ${metadata.dateTimeOriginal ?: "Unknown"}")
                            Text("Timezone: ${metadata.timeZone ?: "Unknown"} (read-only)")
                            Text("Description: ${metadata.description ?: "None"}")
                            Text("Favorite: ${metadata.isFavorite?.toString() ?: "None"}")
                            Text("Album: ${metadata.albumId ?: "None"}")
                            Text("Tags: ${if (metadata.tagIds.isEmpty()) "None" else metadata.tagIds.joinToString(", ")}")
                            Text(if (asset.previewUrl == null) "No preview" else "Preview ready")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BulkEditSection(
    draft: BulkEditDraft,
    selectedCount: Int,
    onDraftChange: (BulkEditDraft) -> Unit,
    onApply: () -> Unit,
    onClearDraft: () -> Unit,
    onClearSelectedStaged: () -> Unit
) {
    Text("Bulk edit selected assets")
    Text("Selected subset: $selectedCount")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = draft.includeDescription,
            onCheckedChange = { onDraftChange(draft.copy(includeDescription = it)) }
        )
        OutlinedTextField(
            value = draft.description,
            onValueChange = { onDraftChange(draft.copy(description = it)) },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = draft.includeDateTimeOriginal,
            onCheckedChange = { onDraftChange(draft.copy(includeDateTimeOriginal = it)) }
        )
        OutlinedTextField(
            value = draft.dateTimeOriginal,
            onValueChange = { onDraftChange(draft.copy(dateTimeOriginal = it)) },
            label = { Text("Date/time original (ISO 8601)") },
            modifier = Modifier.fillMaxWidth()
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = draft.includeAlbumId,
            onCheckedChange = { onDraftChange(draft.copy(includeAlbumId = it)) }
        )
        OutlinedTextField(
            value = draft.albumId,
            onValueChange = { onDraftChange(draft.copy(albumId = it)) },
            label = { Text("Album ID") },
            modifier = Modifier.fillMaxWidth()
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = draft.includeFavorite,
            onCheckedChange = { onDraftChange(draft.copy(includeFavorite = it)) }
        )
        Button(onClick = { onDraftChange(draft.copy(isFavorite = !draft.isFavorite)) }) {
            Text("Favorite = ${draft.isFavorite}")
        }
    }

    OutlinedTextField(
        value = draft.addTagIds,
        onValueChange = { onDraftChange(draft.copy(addTagIds = it)) },
        label = { Text("Add tag IDs (comma separated)") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = draft.removeTagIds,
        onValueChange = { onDraftChange(draft.copy(removeTagIds = it)) },
        label = { Text("Remove tag IDs (comma separated)") },
        modifier = Modifier.fillMaxWidth()
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onApply, enabled = selectedCount > 0) {
            Text("Apply to selected")
        }
        Button(onClick = onClearDraft) {
            Text("Reset bulk draft")
        }
        Button(onClick = onClearSelectedStaged, enabled = selectedCount > 0) {
            Text("Clear selected staged")
        }
    }

    Text("Timezone is shown per asset as read-only metadata.")
}

data class DisplayMetadata(
    val dateTimeOriginal: String?,
    val timeZone: String?,
    val description: String?,
    val isFavorite: Boolean?,
    val albumId: String?,
    val tagIds: Set<String>
)

private fun LocalAsset.toDisplayMetadata(patch: AssetEditPatch?): DisplayMetadata {
    val description = (patch?.description as? FieldPatch.Set<String?>)?.value ?: description
    val isFavorite = (patch?.isFavorite as? FieldPatch.Set<Boolean>)?.value ?: isFavorite
    val dateTimeOriginal = (patch?.dateTimeOriginal as? FieldPatch.Set<String>)?.value ?: captureDateTime
    val albumId = (patch?.albumId as? FieldPatch.Set<String?>)?.value ?: albumId

    val addTags = patch?.addTagIds ?: emptySet()
    val removeTags = patch?.removeTagIds ?: emptySet()

    return DisplayMetadata(
        dateTimeOriginal = dateTimeOriginal,
        timeZone = timeZone,
        description = description,
        isFavorite = isFavorite,
        albumId = albumId,
        tagIds = (tagIds + addTags) - removeTags
    )
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
