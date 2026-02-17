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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.data.ApiImmichOnlineCatalogTransport
import com.marcportabella.immichuploader.data.ApiImmichOnlineTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichCatalogTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichTransport
import com.marcportabella.immichuploader.data.ImmichTransportResult
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadPrepState
import com.marcportabella.immichuploader.domain.UploadPrepAction
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.domain.canApplyBulkEdit
import com.marcportabella.immichuploader.domain.mapLocalIntakeFilesToAssets
import com.marcportabella.immichuploader.domain.preflightBulkEditDraft
import com.marcportabella.immichuploader.platform.BindPlatformFileInput
import com.marcportabella.immichuploader.platform.openPlatformFilePicker
import com.marcportabella.immichuploader.platform.revokePlatformPreviewUrl
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme

@Composable
fun UploadPrepScreen(
    store: UploadPrepStore,
    enableWebEffects: Boolean = true
) {
    val state = store.state
    val scope = rememberCoroutineScope()

    if (enableWebEffects) {
        BindPlatformFileInput { nextFiles ->
            scope.launch {
                store.state.assets.values.mapNotNull { it.previewUrl }.forEach { revokePlatformPreviewUrl(it) }
                val assets = mapLocalIntakeFilesToAssets(nextFiles)
                store.dispatch(UploadPrepAction.ReplaceAssets(assets))
                store.dispatch(UploadPrepAction.ClearSelection)
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
    val catalogLoadedAtInit = remember { mutableStateOf(false) }

    if (enableWebEffects) {
        LaunchedEffect(state.apiKey) {
            if (catalogLoadedAtInit.value) return@LaunchedEffect
            catalogLoadedAtInit.value = true

            store.dispatch(UploadPrepAction.CatalogRequestStarted)
            val apiKey = state.apiKey.ifBlank { null }

            when (val albumResult = catalogTransport.lookupAlbums(apiKey)) {
                is com.marcportabella.immichuploader.data.ImmichCatalogResult.BlockedMissingApiKey ->
                    store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(albumResult.message))

                is com.marcportabella.immichuploader.data.ImmichCatalogResult.Success ->
                    store.dispatch(UploadPrepAction.CatalogAlbumsLoaded(albumResult.entries, albumResult.message))
            }

            when (val tagResult = catalogTransport.lookupTags(apiKey)) {
                is com.marcportabella.immichuploader.data.ImmichCatalogResult.BlockedMissingApiKey ->
                    store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(tagResult.message))

                is com.marcportabella.immichuploader.data.ImmichCatalogResult.Success ->
                    store.dispatch(UploadPrepAction.CatalogTagsLoaded(tagResult.entries, tagResult.message))
            }
        }
    }

    val openFilePicker: () -> Unit = {
        openPlatformFilePicker()
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

    UploadPrepScreenContent(
        state = state,
        gateStatus = gateStatus.toString(),
        executionPath = executionPath.toString(),
        catalogGateStatus = catalogGateStatus.toString(),
        sortedAssets = sortedAssets,
        selectedAssets = selectedAssets,
        bulkPreflightMessage = bulkPreflightFeedback?.message,
        thumbnailCache = thumbnailCache,
        onOpenFilePicker = openFilePicker,
        onSelectAll = { store.dispatch(UploadPrepAction.SelectAll) },
        onClearSelection = { store.dispatch(UploadPrepAction.ClearSelection) },
        onToggleSelection = { store.dispatch(UploadPrepAction.ToggleSelection(it)) },
        onSingleAssetPatch = { assetId, patch -> store.dispatch(UploadPrepAction.StageEditForAsset(assetId, patch)) },
        onClearSingleSelectionStaged = { store.dispatch(UploadPrepAction.ClearStagedForSelected) },
        onBulkDraftChange = { store.dispatch(UploadPrepAction.SetBulkEditDraft(it)) },
        onApplyBulk = { store.dispatch(UploadPrepAction.ApplyBulkEditDraftToSelected) },
        onClearBulkDraft = { store.dispatch(UploadPrepAction.ClearBulkEditDraft) },
        onClearSelectedStaged = { store.dispatch(UploadPrepAction.ClearStagedForSelected) },
        onClearCatalogMessage = { store.dispatch(UploadPrepAction.ClearCatalogMessage) },
        onDismissBatchFeedback = { store.dispatch(UploadPrepAction.ClearBatchFeedback) },
        onGeneratePlan = { store.dispatch(UploadPrepAction.GenerateDryRunPreview) },
        onClearPlan = { store.dispatch(UploadPrepAction.ClearDryRunPreview) },
        onExecute = runExecution,
        onClearExecutionStatus = { store.dispatch(UploadPrepAction.ClearUploadExecutionStatus) }
    )
}

@Composable
private fun UploadPrepScreenContent(
    state: UploadPrepState,
    gateStatus: String,
    executionPath: String,
    catalogGateStatus: String,
    sortedAssets: List<com.marcportabella.immichuploader.domain.LocalAsset>,
    selectedAssets: List<com.marcportabella.immichuploader.domain.LocalAsset>,
    bulkPreflightMessage: String?,
    thumbnailCache: MutableMap<LocalAssetId, ImageBitmap?>,
    onOpenFilePicker: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onToggleSelection: (LocalAssetId) -> Unit,
    onSingleAssetPatch: (LocalAssetId, AssetEditPatch) -> Unit,
    onClearSingleSelectionStaged: () -> Unit,
    onBulkDraftChange: (BulkEditDraft) -> Unit,
    onApplyBulk: () -> Unit,
    onClearBulkDraft: () -> Unit,
    onClearSelectedStaged: () -> Unit,
    onClearCatalogMessage: () -> Unit,
    onDismissBatchFeedback: () -> Unit,
    onGeneratePlan: () -> Unit,
    onClearPlan: () -> Unit,
    onExecute: () -> Unit,
    onClearExecutionStatus: () -> Unit
) {
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
                        gateStatus = gateStatus,
                        executionPath = executionPath,
                        catalogGateStatus = catalogGateStatus
                    )
                }
                item {
                    QueueSelectionCard(
                        hasAssets = state.assets.isNotEmpty(),
                        hasSelection = state.selectedAssetIds.isNotEmpty(),
                        onOpenFilePicker = onOpenFilePicker,
                        onSelectAll = onSelectAll,
                        onClearSelection = onClearSelection
                    )
                }

                assetQueueSection(
                    selectedAssetIds = state.selectedAssetIds,
                    stagedEditsByAssetId = state.stagedEditsByAssetId,
                    sortedAssets = sortedAssets,
                    thumbnailCache = thumbnailCache,
                    columns = 4,
                    onToggleSelection = onToggleSelection
                )

                item {
                    SelectionSidebarPane(
                        selectedAssets = selectedAssets,
                        stagedEditsByAssetId = state.stagedEditsByAssetId,
                        bulkDraft = state.bulkEditDraft,
                        selectedCount = state.selectedAssetIds.size,
                        applyEnabled = canApplyBulkEdit(state),
                        availableAlbums = state.availableAlbums,
                        availableTags = state.availableTags,
                        catalogMessage = state.catalogMessage,
                        preflightMessage = bulkPreflightMessage,
                        onSingleAssetPatch = onSingleAssetPatch,
                        onClearSingleSelectionStaged = onClearSingleSelectionStaged,
                        onBulkDraftChange = onBulkDraftChange,
                        onApplyBulk = onApplyBulk,
                        onClearBulkDraft = onClearBulkDraft,
                        onClearSelectedStaged = onClearSelectedStaged,
                        onClearCatalogMessage = onClearCatalogMessage
                    )
                }

                if (state.batchFeedback != null) {
                    item {
                        BatchFeedbackBanner(
                            feedback = state.batchFeedback,
                            onDismiss = onDismissBatchFeedback
                        )
                    }
                }

                item {
                    RequestPlanExecutionCard(
                        hasSelection = state.selectedAssetIds.isNotEmpty(),
                        hasPlan = state.dryRunPlan != null,
                        executionStatus = state.executionStatus,
                        executionMessage = state.executionMessage,
                        executionRequestCount = state.executionRequestCount,
                        onGeneratePlan = onGeneratePlan,
                        onClearPlan = onClearPlan,
                        onExecute = onExecute,
                        onClearExecutionStatus = onClearExecutionStatus
                    )
                }

                item {
                    DryRunInspectorSection(
                        plan = state.dryRunPlan,
                        requests = state.dryRunApiRequests,
                        message = state.dryRunMessage
                    )
                }
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
                            gateStatus = gateStatus,
                            executionPath = executionPath,
                            catalogGateStatus = catalogGateStatus
                        )
                    }
                    item {
                        QueueSelectionCard(
                            hasAssets = state.assets.isNotEmpty(),
                            hasSelection = state.selectedAssetIds.isNotEmpty(),
                            onOpenFilePicker = onOpenFilePicker,
                            onSelectAll = onSelectAll,
                            onClearSelection = onClearSelection
                        )
                    }
                    assetQueueSection(
                        selectedAssetIds = state.selectedAssetIds,
                        stagedEditsByAssetId = state.stagedEditsByAssetId,
                        sortedAssets = sortedAssets,
                        thumbnailCache = thumbnailCache,
                        columns = 5,
                        onToggleSelection = onToggleSelection
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
                            selectedAssets = selectedAssets,
                            stagedEditsByAssetId = state.stagedEditsByAssetId,
                            bulkDraft = state.bulkEditDraft,
                            selectedCount = state.selectedAssetIds.size,
                            applyEnabled = canApplyBulkEdit(state),
                            availableAlbums = state.availableAlbums,
                            availableTags = state.availableTags,
                            catalogMessage = state.catalogMessage,
                            preflightMessage = bulkPreflightMessage,
                            onSingleAssetPatch = onSingleAssetPatch,
                            onClearSingleSelectionStaged = onClearSingleSelectionStaged,
                            onBulkDraftChange = onBulkDraftChange,
                            onApplyBulk = onApplyBulk,
                            onClearBulkDraft = onClearBulkDraft,
                            onClearSelectedStaged = onClearSelectedStaged,
                            onClearCatalogMessage = onClearCatalogMessage
                        )
                    }
                    if (state.batchFeedback != null) {
                        item {
                            BatchFeedbackBanner(
                                feedback = state.batchFeedback,
                                onDismiss = onDismissBatchFeedback
                            )
                        }
                    }
                    item {
                        RequestPlanExecutionCard(
                            hasSelection = state.selectedAssetIds.isNotEmpty(),
                            hasPlan = state.dryRunPlan != null,
                            executionStatus = state.executionStatus,
                            executionMessage = state.executionMessage,
                            executionRequestCount = state.executionRequestCount,
                            onGeneratePlan = onGeneratePlan,
                            onClearPlan = onClearPlan,
                            onExecute = onExecute,
                            onClearExecutionStatus = onClearExecutionStatus
                        )
                    }
                    item {
                        DryRunInspectorSection(
                            plan = state.dryRunPlan,
                            requests = state.dryRunApiRequests,
                            message = state.dryRunMessage
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun UploadPrepScreenContentPreview() {
    val a1 = previewAsset("a1", "2016-11-08_02-43-27.jpg")
    val a2 = previewAsset("a2", "2016-11-08_04-12-10.jpg")
    val previewState = UploadPrepState(
        assets = listOf(a1, a2).associateBy { it.id },
        selectedAssetIds = setOf(a1.id, a2.id),
        stagedEditsByAssetId = mapOf(a1.id to previewSinglePatch()),
        bulkEditDraft = previewBulkDraft(),
        availableAlbums = previewCatalogAlbums(),
        availableTags = previewCatalogTags(),
        catalogMessage = PREVIEW_CATALOG_MESSAGE,
        dryRunPlan = previewPlan(),
        dryRunApiRequests = previewRequests(),
        dryRunMessage = "Dry-run generated 2 operations.",
        executionStatus = PREVIEW_EXECUTION_STATUS,
        executionMessage = PREVIEW_EXECUTION_MESSAGE,
        executionRequestCount = 2,
        batchFeedback = previewFeedback()
    )
    MaterialTheme {
        UploadPrepScreenContent(
            state = previewState,
            gateStatus = PREVIEW_GATE_STATUS,
            executionPath = PREVIEW_EXECUTION_PATH,
            catalogGateStatus = PREVIEW_CATALOG_STATUS,
            sortedAssets = listOf(a1, a2),
            selectedAssets = listOf(a1, a2),
            bulkPreflightMessage = PREVIEW_PREFLIGHT_MESSAGE,
            thumbnailCache = mutableMapOf(),
            onOpenFilePicker = {},
            onSelectAll = {},
            onClearSelection = {},
            onToggleSelection = {},
            onSingleAssetPatch = { _, _ -> },
            onClearSingleSelectionStaged = {},
            onBulkDraftChange = {},
            onApplyBulk = {},
            onClearBulkDraft = {},
            onClearSelectedStaged = {},
            onClearCatalogMessage = {},
            onDismissBatchFeedback = {},
            onGeneratePlan = {},
            onClearPlan = {},
            onExecute = {},
            onClearExecutionStatus = {}
        )
    }
}

@Preview
@Composable
private fun UploadPrepScreenRoutePreview() {
    val a1 = previewAsset("a1", "2016-11-08_02-43-27.jpg")
    val a2 = previewAsset("a2", "2016-11-08_04-12-10.jpg")
    val previewStore = UploadPrepStore(
        UploadPrepState(
            assets = listOf(a1, a2).associateBy { it.id },
            selectedAssetIds = setOf(a1.id),
            stagedEditsByAssetId = mapOf(a1.id to previewSinglePatch()),
            bulkEditDraft = previewBulkDraft(),
            availableAlbums = previewCatalogAlbums(),
            availableTags = previewCatalogTags(),
            dryRunApiRequests = previewRequests(),
            dryRunMessage = "Dry-run generated 2 operations."
        )
    )
    MaterialTheme {
        UploadPrepScreen(
            store = previewStore,
            enableWebEffects = false
        )
    }
}
