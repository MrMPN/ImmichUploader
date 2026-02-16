package com.marcportabella.immichuploader.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.data.ApiImmichOnlineTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichCatalogTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichTransport
import com.marcportabella.immichuploader.data.DryRunImmichCatalogTransport
import com.marcportabella.immichuploader.data.ImmichCatalogResult
import com.marcportabella.immichuploader.data.ImmichTransportResult
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadExecutionStatus
import com.marcportabella.immichuploader.domain.UploadPrepAction
import com.marcportabella.immichuploader.domain.UploadPrepState
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.domain.canApplyBulkEdit
import com.marcportabella.immichuploader.domain.mapLocalIntakeFilesToAssets
import com.marcportabella.immichuploader.domain.preflightBulkEditDraft
import com.marcportabella.immichuploader.web.revokeObjectUrl
import com.marcportabella.immichuploader.web.toLocalIntakeFile
import kotlinx.browser.document
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadPrepScreen(store: UploadPrepStore) {
    val state = store.state
    val scope = rememberCoroutineScope()

    DisposableEffect(store) {
        val input = document.getElementById("local-file-input") as? HTMLInputElement
        if (input == null) {
            onDispose { }
        } else {
            val listener: (Event) -> Unit = {
                val fileList = input.files
                if (fileList != null) {
                    scope.launch {
                        val nextFiles = mutableListOf<com.marcportabella.immichuploader.domain.LocalIntakeFile>()
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

    val transport = remember { ApiKeyGatedImmichTransport(ApiImmichOnlineTransport()) }
    val gateStatus = transport.gateStatus(apiKey = state.apiKey.ifBlank { null })
    val executionPath = transport.selectExecutionPath(apiKey = state.apiKey.ifBlank { null })
    val catalogTransport = remember { ApiKeyGatedImmichCatalogTransport(DryRunImmichCatalogTransport()) }
    val catalogGateStatus = catalogTransport.gateStatus(state.apiKey.ifBlank { null })
    val bulkPreflightFeedback = preflightBulkEditDraft(state)
    val sortedAssets = remember(state.assets) { state.assets.values.sortedBy { it.fileName } }
    val selectedAssets = remember(state.selectedAssetIds, state.assets) {
        state.selectedAssetIds.mapNotNull { state.assets[it] }.sortedBy { it.fileName }
    }
    val thumbnailCache = remember { mutableMapOf<LocalAssetId, ImageBitmap?>() }

    val openFilePicker: () -> Unit = {
        val input = document.getElementById("local-file-input") as? HTMLInputElement
        input?.click()
        Unit
    }

    val runExecution: () -> Unit = runExecution@{
        val plan = state.dryRunPlan ?: return@runExecution
        scope.launch {
            store.dispatch(
                UploadPrepAction.UploadExecutionStarted(
                    "Executing ${state.dryRunApiRequests.size} API requests."
                )
            )
            when (val result = transport.submit(plan = plan, apiKey = state.apiKey.ifBlank { null })) {
                is ImmichTransportResult.BlockedMissingApiKey ->
                    store.dispatch(
                        UploadPrepAction.UploadExecutionBlocked(
                            "API key required. Upload execution remained blocked."
                        )
                    )

                is ImmichTransportResult.DryRun ->
                    store.dispatch(
                        UploadPrepAction.UploadExecutionFailed(
                            "Execution stayed in dry-run mode. Configure executable transport first."
                        )
                    )

                is ImmichTransportResult.Submitted ->
                    store.dispatch(
                        UploadPrepAction.UploadExecutionSubmitted(
                            requestCount = result.requestCount,
                            message = "Submitted ${result.requestCount} API requests."
                        )
                    )

                is ImmichTransportResult.Failed ->
                    store.dispatch(
                        UploadPrepAction.UploadExecutionFailed(result.message)
                    )
            }
        }
        Unit
    }

    BoxWithConstraints(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .fillMaxSize()
    ) {
        val isNarrow = maxWidth < 1100.dp

        if (isNarrow) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SummaryHeaderCard(
                        assetCount = state.assets.size,
                        selectedCount = state.selectedAssetIds.size,
                        stagedCount = state.stagedEditsByAssetId.size,
                        gateStatus = gateStatus.toString(),
                        executionPath = executionPath.toString(),
                        catalogGateStatus = catalogGateStatus.toString()
                    )
                }
                item {
                    ConnectionCard(
                        apiKey = state.apiKey,
                        onApiKeyChange = { store.dispatch(UploadPrepAction.SetApiKey(it)) }
                    )
                }
                item {
                    QueueSelectionCard(
                        state = state,
                        onOpenFilePicker = openFilePicker,
                        onSelectAll = { store.dispatch(UploadPrepAction.SelectAll) },
                        onClearSelection = { store.dispatch(UploadPrepAction.ClearSelection) }
                    )
                }

                assetQueueSection(
                    state = state,
                    sortedAssets = sortedAssets,
                    thumbnailCache = thumbnailCache,
                    columns = 4,
                    onToggleSelection = { store.dispatch(UploadPrepAction.ToggleSelection(it)) }
                )

                item {
                    SelectionSidebarPane(
                        state = state,
                        selectedAssets = selectedAssets,
                        preflightMessage = bulkPreflightFeedback?.message,
                        onSingleAssetPatch = { assetId, patch ->
                            store.dispatch(UploadPrepAction.StageEditForAsset(assetId, patch))
                        },
                        onClearSingleSelectionStaged = { store.dispatch(UploadPrepAction.ClearStagedForSelected) },
                        onBulkDraftChange = { store.dispatch(UploadPrepAction.SetBulkEditDraft(it)) },
                        onApplyBulk = { store.dispatch(UploadPrepAction.ApplyBulkEditDraftToSelected) },
                        onClearBulkDraft = { store.dispatch(UploadPrepAction.ClearBulkEditDraft) },
                        onClearSelectedStaged = { store.dispatch(UploadPrepAction.ClearStagedForSelected) }
                    )
                }

                if (state.batchFeedback != null) {
                    item {
                        BatchFeedbackBanner(
                            feedback = state.batchFeedback,
                            onDismiss = { store.dispatch(UploadPrepAction.ClearBatchFeedback) }
                        )
                    }
                }

                item {
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
                                when (
                                    val result = catalogTransport.createAlbumIfMissing(
                                        state.apiKey.ifBlank { null },
                                        state.albumCreateDraft
                                    )
                                ) {
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
                                when (
                                    val result = catalogTransport.createTagIfMissing(
                                        state.apiKey.ifBlank { null },
                                        state.tagCreateDraft
                                    )
                                ) {
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
                }

                item {
                    DryRunExecutionCard(
                        state = state,
                        onGenerateDryRun = { store.dispatch(UploadPrepAction.GenerateDryRunPreview) },
                        onClearDryRun = { store.dispatch(UploadPrepAction.ClearDryRunPreview) },
                        onExecute = runExecution,
                        onClearExecutionStatus = { store.dispatch(UploadPrepAction.ClearUploadExecutionStatus) }
                    )
                }

                item { DryRunInspectorSection(state) }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1.6f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SummaryHeaderCard(
                            assetCount = state.assets.size,
                            selectedCount = state.selectedAssetIds.size,
                            stagedCount = state.stagedEditsByAssetId.size,
                            gateStatus = gateStatus.toString(),
                            executionPath = executionPath.toString(),
                            catalogGateStatus = catalogGateStatus.toString()
                        )
                    }
                    item {
                        QueueSelectionCard(
                            state = state,
                            onOpenFilePicker = openFilePicker,
                            onSelectAll = { store.dispatch(UploadPrepAction.SelectAll) },
                            onClearSelection = { store.dispatch(UploadPrepAction.ClearSelection) }
                        )
                    }
                    assetQueueSection(
                        state = state,
                        sortedAssets = sortedAssets,
                        thumbnailCache = thumbnailCache,
                        columns = 5,
                        onToggleSelection = { store.dispatch(UploadPrepAction.ToggleSelection(it)) }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ConnectionCard(
                            apiKey = state.apiKey,
                            onApiKeyChange = { store.dispatch(UploadPrepAction.SetApiKey(it)) }
                        )
                    }
                    item {
                        SelectionSidebarPane(
                            state = state,
                            selectedAssets = selectedAssets,
                            preflightMessage = bulkPreflightFeedback?.message,
                            onSingleAssetPatch = { assetId, patch ->
                                store.dispatch(UploadPrepAction.StageEditForAsset(assetId, patch))
                            },
                            onClearSingleSelectionStaged = { store.dispatch(UploadPrepAction.ClearStagedForSelected) },
                            onBulkDraftChange = { store.dispatch(UploadPrepAction.SetBulkEditDraft(it)) },
                            onApplyBulk = { store.dispatch(UploadPrepAction.ApplyBulkEditDraftToSelected) },
                            onClearBulkDraft = { store.dispatch(UploadPrepAction.ClearBulkEditDraft) },
                            onClearSelectedStaged = { store.dispatch(UploadPrepAction.ClearStagedForSelected) }
                        )
                    }
                    if (state.batchFeedback != null) {
                        item {
                            BatchFeedbackBanner(
                                feedback = state.batchFeedback,
                                onDismiss = { store.dispatch(UploadPrepAction.ClearBatchFeedback) }
                            )
                        }
                    }
                    item {
                        CatalogSection(
                            state = state,
                            onLookupAlbums = {
                                scope.launch {
                                    store.dispatch(UploadPrepAction.CatalogRequestStarted)
                                    when (val result = catalogTransport.lookupAlbums(state.apiKey.ifBlank { null })) {
                                        is ImmichCatalogResult.BlockedMissingApiKey ->
                                            store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                                        is ImmichCatalogResult.DryRunSuccess ->
                                            store.dispatch(
                                                UploadPrepAction.CatalogAlbumsLoaded(result.entries, result.message)
                                            )
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
                                    when (
                                        val result = catalogTransport.createAlbumIfMissing(
                                            state.apiKey.ifBlank { null },
                                            state.albumCreateDraft
                                        )
                                    ) {
                                        is ImmichCatalogResult.BlockedMissingApiKey ->
                                            store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                                        is ImmichCatalogResult.DryRunSuccess -> {
                                            store.dispatch(
                                                UploadPrepAction.CatalogAlbumsLoaded(result.entries, result.message)
                                            )
                                            store.dispatch(UploadPrepAction.SetAlbumCreateDraft(""))
                                        }
                                    }
                                }
                            },
                            onCreateTag = {
                                scope.launch {
                                    store.dispatch(UploadPrepAction.CatalogRequestStarted)
                                    when (
                                        val result = catalogTransport.createTagIfMissing(
                                            state.apiKey.ifBlank { null },
                                            state.tagCreateDraft
                                        )
                                    ) {
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
                    }
                    item {
                        DryRunExecutionCard(
                            state = state,
                            onGenerateDryRun = { store.dispatch(UploadPrepAction.GenerateDryRunPreview) },
                            onClearDryRun = { store.dispatch(UploadPrepAction.ClearDryRunPreview) },
                            onExecute = runExecution,
                            onClearExecutionStatus = { store.dispatch(UploadPrepAction.ClearUploadExecutionStatus) }
                        )
                    }
                    item { DryRunInspectorSection(state) }
                }
            }
        }
    }
}

@Composable
private fun SummaryHeaderCard(
    assetCount: Int,
    selectedCount: Int,
    stagedCount: Int,
    gateStatus: String,
    executionPath: String,
    catalogGateStatus: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Immich Upload Prep",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Prepare metadata, validate the dry-run payload, then execute upload safely.",
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryPill("Assets", assetCount.toString())
                SummaryPill("Selected", selectedCount.toString())
                SummaryPill("Staged", stagedCount.toString())
                SummaryPill("Transport", gateStatus)
                SummaryPill("Execution", executionPath)
                SummaryPill("Catalog", catalogGateStatus)
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    apiKey: String,
    onApiKeyChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Connection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "API key is required for catalog lookups, catalog create operations, and upload execution.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("Immich API key") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QueueSelectionCard(
    state: UploadPrepState,
    onOpenFilePicker: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Queue selection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Load local media, review in the explorer pane, then use the sidebar for edit details.",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenFilePicker) {
                    Text("Select local media")
                }
                Button(
                    onClick = onSelectAll,
                    enabled = state.assets.isNotEmpty()
                ) {
                    Text("Select all")
                }
                Button(
                    onClick = onClearSelection,
                    enabled = state.selectedAssetIds.isNotEmpty()
                ) {
                    Text("Clear selection")
                }
            }
        }
    }
}

@Composable
private fun SelectionSidebarPane(
    state: UploadPrepState,
    selectedAssets: List<LocalAsset>,
    preflightMessage: String?,
    onSingleAssetPatch: (LocalAssetId, AssetEditPatch) -> Unit,
    onClearSingleSelectionStaged: () -> Unit,
    onBulkDraftChange: (BulkEditDraft) -> Unit,
    onApplyBulk: () -> Unit,
    onClearBulkDraft: () -> Unit,
    onClearSelectedStaged: () -> Unit
) {
    when (selectedAssets.size) {
        0 -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Details sidebar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("No files selected.")
                    Text("Select one asset to inspect and edit metadata, or select multiple for bulk editing.")
                }
            }
        }

        1 -> {
            val asset = selectedAssets.first()
            val patch = state.stagedEditsByAssetId[asset.id]
            SingleSelectionEditorCard(
                asset = asset,
                patch = patch,
                onPatch = { onSingleAssetPatch(asset.id, it) },
                onClearStaged = onClearSingleSelectionStaged
            )
        }

        else -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Bulk edit mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("${selectedAssets.size} assets selected. Changes from this panel apply to the whole selection.")
                }
            }

            BulkEditSection(
                draft = state.bulkEditDraft,
                selectedCount = state.selectedAssetIds.size,
                applyEnabled = canApplyBulkEdit(state),
                preflightMessage = preflightMessage,
                onDraftChange = onBulkDraftChange,
                onApply = onApplyBulk,
                onClearDraft = onClearBulkDraft,
                onClearSelectedStaged = onClearSelectedStaged
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleSelectionEditorCard(
    asset: LocalAsset,
    patch: AssetEditPatch?,
    onPatch: (AssetEditPatch) -> Unit,
    onClearStaged: () -> Unit
) {
    val metadata = asset.toDisplayMetadata(patch)
    val favorite = metadata.isFavorite ?: false

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Asset details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(asset.fileName, fontWeight = FontWeight.Medium)
            Text("${asset.mimeType} - ${asset.fileSizeBytes} bytes")
            metadata.captureDisplay?.let { Text("Capture: $it") }
            Text("Camera: ${metadata.cameraLabel ?: "Unknown"}")
            if (metadata.exifSummary != null) {
                Text("EXIF: ${metadata.exifSummary}")
            }

            HorizontalDivider()
            Text("Edit metadata to stage single-asset changes.")

            OutlinedTextField(
                value = metadata.description.orEmpty(),
                onValueChange = {
                    onPatch(
                        AssetEditPatch(description = FieldPatch.Set(it.ifBlank { null }))
                    )
                },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = metadata.dateTimeOriginal.orEmpty(),
                onValueChange = {
                    onPatch(
                        AssetEditPatch(dateTimeOriginal = FieldPatch.Set(it))
                    )
                },
                label = { Text("Date/time original (ISO 8601)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = metadata.timeZone.orEmpty(),
                onValueChange = {
                    onPatch(
                        AssetEditPatch(timeZone = FieldPatch.Set(it))
                    )
                },
                label = { Text("Timezone (e.g. +02:00 or Z)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = metadata.albumId.orEmpty(),
                onValueChange = {
                    onPatch(
                        AssetEditPatch(albumId = FieldPatch.Set(it.ifBlank { null }))
                    )
                },
                label = { Text("Album ID") },
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onPatch(
                            AssetEditPatch(isFavorite = FieldPatch.Set(!favorite))
                        )
                    }
                ) {
                    Text("Favorite = ${!favorite}")
                }
                Button(onClick = onClearStaged) {
                    Text("Clear staged for selected")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DryRunExecutionCard(
    state: UploadPrepState,
    onGenerateDryRun: () -> Unit,
    onClearDryRun: () -> Unit,
    onExecute: () -> Unit,
    onClearExecutionStatus: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Dry-run and execution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Generate a preview before execution to verify payload details and request count.",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onGenerateDryRun,
                    enabled = state.selectedAssetIds.isNotEmpty()
                ) {
                    Text("Generate dry-run plan")
                }
                Button(
                    onClick = onClearDryRun,
                    enabled = state.dryRunPlan != null
                ) {
                    Text("Clear dry-run")
                }
                Button(
                    onClick = onExecute,
                    enabled = state.dryRunPlan != null && state.executionStatus != UploadExecutionStatus.Executing
                ) {
                    Text("Execute API upload")
                }
                Button(
                    onClick = onClearExecutionStatus,
                    enabled = state.executionMessage != null || state.executionStatus != UploadExecutionStatus.Idle
                ) {
                    Text("Clear execution status")
                }
            }

            HorizontalDivider()
            Text("Execution status: ${state.executionStatus}")
            if (state.executionMessage != null) {
                Text("Execution message: ${state.executionMessage}")
            }
            if (state.executionRequestCount != null) {
                Text("Submitted requests: ${state.executionRequestCount}")
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "$label:", style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
