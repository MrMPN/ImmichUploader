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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadPrepState

@Composable
internal fun UploadPrepScreenContent(
    state: UploadPrepState,
    gateStatus: String,
    executionPath: String,
    catalogGateStatus: String,
    sortedAssets: List<LocalAsset>,
    selectedAssets: List<LocalAsset>,
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
    onClearExecutionStatus: () -> Unit,
    canApplyBulkEdit: (UploadPrepState) -> Boolean
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
                    item { BatchFeedbackBanner(feedback = state.batchFeedback, onDismiss = onDismissBatchFeedback) }
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
                item { DryRunInspectorSection(plan = state.dryRunPlan, requests = state.dryRunApiRequests, message = state.dryRunMessage) }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1.6f).fillMaxHeight(),
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
                    modifier = Modifier.weight(1f).fillMaxHeight(),
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
                        item { BatchFeedbackBanner(feedback = state.batchFeedback, onDismiss = onDismissBatchFeedback) }
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
                    item { DryRunInspectorSection(plan = state.dryRunPlan, requests = state.dryRunApiRequests, message = state.dryRunMessage) }
                }
            }
        }
    }
}
