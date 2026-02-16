package com.marcportabella.immichuploader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.data.ApiImmichOnlineCatalogTransport
import com.marcportabella.immichuploader.data.ApiImmichOnlineTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichCatalogTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichTransport
import com.marcportabella.immichuploader.data.ImmichCatalogResult
import com.marcportabella.immichuploader.data.ImmichTransportResult
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadPrepAction
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.domain.mapLocalIntakeFilesToAssets
import com.marcportabella.immichuploader.domain.preflightBulkEditDraft
import com.marcportabella.immichuploader.web.revokeObjectUrl
import com.marcportabella.immichuploader.web.toLocalIntakeFile
import kotlinx.browser.document
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import androidx.compose.material3.MaterialTheme

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

                        store.state.assets.values.mapNotNull { it.previewUrl }.forEach { revokeObjectUrl(it) }

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
                store.state.assets.values.mapNotNull { it.previewUrl }.forEach { revokeObjectUrl(it) }
            }
        }
    }

    val transport = remember { ApiKeyGatedImmichTransport(ApiImmichOnlineTransport()) }
    val gateStatus = transport.gateStatus(apiKey = state.apiKey.ifBlank { null })
    val executionPath = transport.selectExecutionPath(apiKey = state.apiKey.ifBlank { null })
    val catalogTransport = remember { ApiKeyGatedImmichCatalogTransport(ApiImmichOnlineCatalogTransport()) }
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
                    store.dispatch(UploadPrepAction.UploadExecutionBlocked("API key required. Upload execution remained blocked."))

                is ImmichTransportResult.Submitted ->
                    store.dispatch(
                        UploadPrepAction.UploadExecutionSubmitted(
                            requestCount = result.requestCount,
                            message = "Submitted ${result.requestCount} API requests."
                        )
                    )

                is ImmichTransportResult.Failed ->
                    store.dispatch(UploadPrepAction.UploadExecutionFailed(result.message))
            }
        }
        Unit
    }

    val lookupAlbums: () -> Unit = {
        scope.launch {
            store.dispatch(UploadPrepAction.CatalogRequestStarted)
            when (val result = catalogTransport.lookupAlbums(state.apiKey.ifBlank { null })) {
                is ImmichCatalogResult.BlockedMissingApiKey ->
                    store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                is ImmichCatalogResult.Success ->
                    store.dispatch(UploadPrepAction.CatalogAlbumsLoaded(result.entries, result.message))
            }
        }
    }

    val lookupTags: () -> Unit = {
        scope.launch {
            store.dispatch(UploadPrepAction.CatalogRequestStarted)
            when (val result = catalogTransport.lookupTags(state.apiKey.ifBlank { null })) {
                is ImmichCatalogResult.BlockedMissingApiKey ->
                    store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                is ImmichCatalogResult.Success ->
                    store.dispatch(UploadPrepAction.CatalogTagsLoaded(result.entries, result.message))
            }
        }
    }

    val createAlbum: () -> Unit = {
        scope.launch {
            store.dispatch(UploadPrepAction.CatalogRequestStarted)
            when (val result = catalogTransport.createAlbumIfMissing(state.apiKey.ifBlank { null }, state.albumCreateDraft)) {
                is ImmichCatalogResult.BlockedMissingApiKey ->
                    store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                is ImmichCatalogResult.Success -> {
                    store.dispatch(UploadPrepAction.CatalogAlbumsLoaded(result.entries, result.message))
                    store.dispatch(UploadPrepAction.SetAlbumCreateDraft(""))
                }
            }
        }
    }

    val createTag: () -> Unit = {
        scope.launch {
            store.dispatch(UploadPrepAction.CatalogRequestStarted)
            when (val result = catalogTransport.createTagIfMissing(state.apiKey.ifBlank { null }, state.tagCreateDraft)) {
                is ImmichCatalogResult.BlockedMissingApiKey ->
                    store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                is ImmichCatalogResult.Success -> {
                    store.dispatch(UploadPrepAction.CatalogTagsLoaded(result.entries, result.message))
                    store.dispatch(UploadPrepAction.SetTagCreateDraft(""))
                }
            }
        }
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
                        onSingleAssetPatch = { assetId, patch -> store.dispatch(UploadPrepAction.StageEditForAsset(assetId, patch)) },
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
                        onLookupAlbums = lookupAlbums,
                        onLookupTags = lookupTags,
                        onAlbumDraftChange = { store.dispatch(UploadPrepAction.SetAlbumCreateDraft(it)) },
                        onTagDraftChange = { store.dispatch(UploadPrepAction.SetTagCreateDraft(it)) },
                        onCreateAlbum = createAlbum,
                        onCreateTag = createTag,
                        onClearMessage = { store.dispatch(UploadPrepAction.ClearCatalogMessage) }
                    )
                }

                item {
                    RequestPlanExecutionCard(
                        state = state,
                        onGeneratePlan = { store.dispatch(UploadPrepAction.GenerateDryRunPreview) },
                        onClearPlan = { store.dispatch(UploadPrepAction.ClearDryRunPreview) },
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
                        SelectionSidebarPane(
                            state = state,
                            selectedAssets = selectedAssets,
                            preflightMessage = bulkPreflightFeedback?.message,
                            onSingleAssetPatch = { assetId, patch -> store.dispatch(UploadPrepAction.StageEditForAsset(assetId, patch)) },
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
                            onLookupAlbums = lookupAlbums,
                            onLookupTags = lookupTags,
                            onAlbumDraftChange = { store.dispatch(UploadPrepAction.SetAlbumCreateDraft(it)) },
                            onTagDraftChange = { store.dispatch(UploadPrepAction.SetTagCreateDraft(it)) },
                            onCreateAlbum = createAlbum,
                            onCreateTag = createTag,
                            onClearMessage = { store.dispatch(UploadPrepAction.ClearCatalogMessage) }
                        )
                    }
                    item {
                        RequestPlanExecutionCard(
                            state = state,
                            onGeneratePlan = { store.dispatch(UploadPrepAction.GenerateDryRunPreview) },
                            onClearPlan = { store.dispatch(UploadPrepAction.ClearDryRunPreview) },
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
