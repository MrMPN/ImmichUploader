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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLInputElement
import org.w3c.files.File

@Composable
fun App() {
    MaterialTheme {
        val store = remember { UploadPrepStore() }
        val state = store.state
        val scope = rememberCoroutineScope()

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

        val transport = remember { ApiKeyGatedImmichTransport(DryRunImmichTransport()) }
        val gateStatus = transport.gateStatus(apiKey = state.apiKey.ifBlank { null })
        val catalogTransport = remember { ApiKeyGatedImmichCatalogTransport(DryRunImmichCatalogTransport()) }
        val catalogGateStatus = catalogTransport.gateStatus(state.apiKey.ifBlank { null })
        val bulkPreflightFeedback = preflightBulkEditDraft(state)

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
            Text("Transport gate: $gateStatus")
            Text("Catalog gate: $catalogGateStatus")

            OutlinedTextField(
                value = state.apiKey,
                onValueChange = { store.dispatch(UploadPrepAction.SetApiKey(it)) },
                label = { Text("Immich API key (required for lookup/create)") },
                modifier = Modifier.fillMaxWidth()
            )

            CatalogSection(
                state = state,
                onLookupAlbums = {
                    scope.launch {
                        store.dispatch(UploadPrepAction.CatalogRequestStarted)
                        when (val result = catalogTransport.lookupAlbums(state.apiKey.ifBlank { null })) {
                            is ImmichCatalogResult.BlockedMissingApiKey ->
                                store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                            is ImmichCatalogResult.DryRunSuccess ->
                                store.dispatch(UploadPrepAction.CatalogAlbumsLoaded(result.entries, result.message))
                        }
                    }
                },
                onLookupTags = {
                    scope.launch {
                        store.dispatch(UploadPrepAction.CatalogRequestStarted)
                        when (val result = catalogTransport.lookupTags(state.apiKey.ifBlank { null })) {
                            is ImmichCatalogResult.BlockedMissingApiKey ->
                                store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                            is ImmichCatalogResult.DryRunSuccess ->
                                store.dispatch(UploadPrepAction.CatalogTagsLoaded(result.entries, result.message))
                        }
                    }
                },
                onAlbumDraftChange = { store.dispatch(UploadPrepAction.SetAlbumCreateDraft(it)) },
                onTagDraftChange = { store.dispatch(UploadPrepAction.SetTagCreateDraft(it)) },
                onCreateAlbum = {
                    scope.launch {
                        store.dispatch(UploadPrepAction.CatalogRequestStarted)
                        when (val result = catalogTransport.createAlbumIfMissing(state.apiKey.ifBlank { null }, state.albumCreateDraft)) {
                            is ImmichCatalogResult.BlockedMissingApiKey ->
                                store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                            is ImmichCatalogResult.DryRunSuccess -> {
                                store.dispatch(UploadPrepAction.CatalogAlbumsLoaded(result.entries, result.message))
                                store.dispatch(UploadPrepAction.SetAlbumCreateDraft(""))
                            }
                        }
                    }
                },
                onCreateTag = {
                    scope.launch {
                        store.dispatch(UploadPrepAction.CatalogRequestStarted)
                        when (val result = catalogTransport.createTagIfMissing(state.apiKey.ifBlank { null }, state.tagCreateDraft)) {
                            is ImmichCatalogResult.BlockedMissingApiKey ->
                                store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                            is ImmichCatalogResult.DryRunSuccess -> {
                                store.dispatch(UploadPrepAction.CatalogTagsLoaded(result.entries, result.message))
                                store.dispatch(UploadPrepAction.SetTagCreateDraft(""))
                            }
                        }
                    }
                },
                onClearMessage = { store.dispatch(UploadPrepAction.ClearCatalogMessage) }
            )

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
                applyEnabled = canApplyBulkEdit(state),
                preflightMessage = bulkPreflightFeedback?.message,
                onDraftChange = { store.dispatch(UploadPrepAction.SetBulkEditDraft(it)) },
                onApply = { store.dispatch(UploadPrepAction.ApplyBulkEditDraftToSelected) },
                onClearDraft = { store.dispatch(UploadPrepAction.ClearBulkEditDraft) },
                onClearSelectedStaged = { store.dispatch(UploadPrepAction.ClearStagedForSelected) }
            )

            if (state.batchFeedback != null) {
                BatchFeedbackBanner(
                    feedback = state.batchFeedback,
                    onDismiss = { store.dispatch(UploadPrepAction.ClearBatchFeedback) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { store.dispatch(UploadPrepAction.GenerateDryRunPreview) },
                    enabled = state.selectedAssetIds.isNotEmpty()
                ) {
                    Text("Generate dry-run plan")
                }
                Button(
                    onClick = { store.dispatch(UploadPrepAction.ClearDryRunPreview) },
                    enabled = state.dryRunPlan != null
                ) {
                    Text("Clear dry-run")
                }
            }

            DryRunInspectorSection(state)

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
private fun DryRunInspectorSection(state: UploadPrepState) {
    Text("Dry-run payload inspector")
    val plan = state.dryRunPlan
    val requests = state.dryRunApiRequests
    Text("Planned operations: ${requests.size}")

    if (plan != null) {
        Text("Upload ops: ${plan.uploadRequests.size}")
        Text("Metadata ops: ${plan.bulkMetadataRequests.size}")
        Text("Tag ops: ${plan.tagAssignRequests.size}")
        Text("Album ops: ${plan.albumAddRequests.size}")
        Text("Lookup hooks: ${plan.lookupHooks.size}")
    }

    if (state.dryRunMessage != null) {
        Text(state.dryRunMessage)
    }

    if (requests.isEmpty()) {
        Text("No payload preview available yet.")
    } else {
        requests.forEachIndexed { index, request ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("${index + 1}. ${request.method} ${request.url}")
                Text("Payload: ${request.body ?: "<none>"}")
            }
        }
    }
}

@Composable
private fun CatalogSection(
    state: UploadPrepState,
    onLookupAlbums: () -> Unit,
    onLookupTags: () -> Unit,
    onAlbumDraftChange: (String) -> Unit,
    onTagDraftChange: (String) -> Unit,
    onCreateAlbum: () -> Unit,
    onCreateTag: () -> Unit,
    onClearMessage: () -> Unit
) {
    Text("Immich tags/albums")
    Text("Catalog status: ${state.catalogStatus}")

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onLookupAlbums) {
            Text("Load albums")
        }
        Button(onClick = onLookupTags) {
            Text("Load tags")
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = state.albumCreateDraft,
            onValueChange = onAlbumDraftChange,
            label = { Text("Create album if missing") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onCreateAlbum) {
            Text("Create album")
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = state.tagCreateDraft,
            onValueChange = onTagDraftChange,
            label = { Text("Create tag if missing") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onCreateTag) {
            Text("Create tag")
        }
    }

    Text("Albums loaded: ${state.availableAlbums.size}")
    Text("Tags loaded: ${state.availableTags.size}")
    Text(
        "Album names: ${
            if (state.availableAlbums.isEmpty()) "None"
            else state.availableAlbums.joinToString(", ") { it.name }
        }"
    )
    Text(
        "Tag names: ${
            if (state.availableTags.isEmpty()) "None"
            else state.availableTags.joinToString(", ") { it.name }
        }"
    )

    if (state.catalogMessage != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.catalogMessage)
            Button(onClick = onClearMessage) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun BulkEditSection(
    draft: BulkEditDraft,
    selectedCount: Int,
    applyEnabled: Boolean,
    preflightMessage: String?,
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

    if (preflightMessage != null) {
        Text(preflightMessage)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onApply, enabled = applyEnabled) {
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

@Composable
private fun BatchFeedbackBanner(
    feedback: BatchFeedback,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = when (feedback.level) {
        BatchFeedbackLevel.Error -> colorScheme.errorContainer
        BatchFeedbackLevel.Warning -> colorScheme.secondaryContainer
        BatchFeedbackLevel.Success -> colorScheme.tertiaryContainer
    }
    val contentColor = when (feedback.level) {
        BatchFeedbackLevel.Error -> colorScheme.onErrorContainer
        BatchFeedbackLevel.Warning -> colorScheme.onSecondaryContainer
        BatchFeedbackLevel.Success -> colorScheme.onTertiaryContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = feedback.message,
            color = contentColor
        )
        Button(onClick = onDismiss) {
            Text("Dismiss")
        }
    }
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
