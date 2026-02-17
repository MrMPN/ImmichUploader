package com.marcportabella.immichuploader.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import com.marcportabella.immichuploader.data.ApiImmichOnlineCatalogTransport
import com.marcportabella.immichuploader.data.ApiImmichOnlineTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichCatalogTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichTransport
import com.marcportabella.immichuploader.data.ImmichCatalogResult
import com.marcportabella.immichuploader.data.ImmichTransportResult
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadPrepAction
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.domain.canApplyBulkEdit
import com.marcportabella.immichuploader.domain.mapLocalIntakeFilesToAssets
import com.marcportabella.immichuploader.domain.preflightBulkEditDraft
import com.marcportabella.immichuploader.platform.BindPlatformFileInput
import com.marcportabella.immichuploader.platform.openPlatformFilePicker
import com.marcportabella.immichuploader.platform.revokePlatformPreviewUrl
import kotlinx.coroutines.launch

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
                is ImmichCatalogResult.BlockedMissingApiKey ->
                    store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(albumResult.message))
                is ImmichCatalogResult.Success ->
                    store.dispatch(UploadPrepAction.CatalogAlbumsLoaded(albumResult.entries, albumResult.message))
            }
            when (val tagResult = catalogTransport.lookupTags(apiKey)) {
                is ImmichCatalogResult.BlockedMissingApiKey ->
                    store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(tagResult.message))
                is ImmichCatalogResult.Success ->
                    store.dispatch(UploadPrepAction.CatalogTagsLoaded(tagResult.entries, tagResult.message))
            }
        }
    }

    val runExecution: () -> Unit = runExecution@{
        val plan = state.dryRunPlan ?: return@runExecution
        scope.launch {
            store.dispatch(UploadPrepAction.UploadExecutionStarted("Executing ${state.dryRunApiRequests.size} API requests."))
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
        onOpenFilePicker = { openPlatformFilePicker() },
        onSelectAll = { store.dispatch(UploadPrepAction.SelectAll) },
        onClearSelection = { store.dispatch(UploadPrepAction.ClearSelection) },
        onToggleSelection = { store.dispatch(UploadPrepAction.ToggleSelection(it)) },
        onSingleAssetPatch = { assetId: LocalAssetId, patch: AssetEditPatch ->
            store.dispatch(UploadPrepAction.StageEditForAsset(assetId, patch))
        },
        onClearSingleSelectionStaged = { store.dispatch(UploadPrepAction.ClearStagedForSelected) },
        onBulkDraftChange = { draft: BulkEditDraft -> store.dispatch(UploadPrepAction.SetBulkEditDraft(draft)) },
        onApplyBulk = { store.dispatch(UploadPrepAction.ApplyBulkEditDraftToSelected) },
        onClearBulkDraft = { store.dispatch(UploadPrepAction.ClearBulkEditDraft) },
        onClearSelectedStaged = { store.dispatch(UploadPrepAction.ClearStagedForSelected) },
        onClearCatalogMessage = { store.dispatch(UploadPrepAction.ClearCatalogMessage) },
        onDismissBatchFeedback = { store.dispatch(UploadPrepAction.ClearBatchFeedback) },
        onGeneratePlan = { store.dispatch(UploadPrepAction.GenerateDryRunPreview) },
        onClearPlan = { store.dispatch(UploadPrepAction.ClearDryRunPreview) },
        onExecute = runExecution,
        onClearExecutionStatus = { store.dispatch(UploadPrepAction.ClearUploadExecutionStatus) },
        canApplyBulkEdit = { canApplyBulkEdit(it) }
    )
}
