package com.marcportabella.immichuploader.ui.uploadprep

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
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadPrepState
import com.marcportabella.immichuploader.domain.canApplyBulkEdit

@Composable
internal fun UploadPrepScreenContent(
    state: UploadPrepState,
    gateStatus: String,
    executionPath: String,
    catalogGateStatus: String,
    sortedAssets: List<LocalAsset>,
    selectedAssets: List<LocalAsset>,
    bulkPreflightMessage: String?,
    onOpenFilePicker: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onToggleSelection: (LocalAssetId) -> Unit,
    onSingleAssetPatch: (LocalAssetId, AssetEditPatch) -> Unit,
    onSingleAssetTagSelectionReplace: (LocalAssetId, Set<String>, Set<String>) -> Unit,
    onClearSingleSelectionStaged: () -> Unit,
    onBulkDraftChange: (BulkEditDraft) -> Unit,
    onCreateSessionTagForBulk: (String) -> Unit,
    onCreateSessionTagForAsset: (LocalAssetId, String) -> Unit,
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
                        duplicateCount = state.duplicateAssetIds.size,
                        duplicateStatus = state.duplicateCheckStatus.toString(),
                        gateStatus = gateStatus,
                        executionPath = executionPath,
                        catalogGateStatus = catalogGateStatus
                    )
                }
                item {
                    QueueSelectionCard(
                        hasAssets = state.assets.isNotEmpty(),
                        hasSelection = state.selectedAssetIds.isNotEmpty(),
                        duplicateCheckMessage = state.duplicateCheckMessage,
                        onOpenFilePicker = onOpenFilePicker,
                        onSelectAll = onSelectAll,
                        onClearSelection = onClearSelection
                    )
                }
                assetQueueSection(
                    selectedAssetIds = state.selectedAssetIds,
                    duplicateAssetIds = state.duplicateAssetIds,
                    stagedEditsByAssetId = state.stagedEditsByAssetId,
                    sortedAssets = sortedAssets,
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
                        onSingleAssetTagSelectionReplace = onSingleAssetTagSelectionReplace,
                        onClearSingleSelectionStaged = onClearSingleSelectionStaged,
                        onBulkDraftChange = onBulkDraftChange,
                        onCreateSessionTagForBulk = onCreateSessionTagForBulk,
                        onCreateSessionTagForAsset = onCreateSessionTagForAsset,
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
                        hasPlan = state.dryRunPlan != null,
                        planMessage = state.dryRunMessage,
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
                            duplicateCount = state.duplicateAssetIds.size,
                            duplicateStatus = state.duplicateCheckStatus.toString(),
                            gateStatus = gateStatus,
                            executionPath = executionPath,
                            catalogGateStatus = catalogGateStatus
                        )
                    }
                    item {
                        QueueSelectionCard(
                            hasAssets = state.assets.isNotEmpty(),
                            hasSelection = state.selectedAssetIds.isNotEmpty(),
                            duplicateCheckMessage = state.duplicateCheckMessage,
                            onOpenFilePicker = onOpenFilePicker,
                            onSelectAll = onSelectAll,
                            onClearSelection = onClearSelection
                        )
                    }
                    assetQueueSection(
                        selectedAssetIds = state.selectedAssetIds,
                        duplicateAssetIds = state.duplicateAssetIds,
                        stagedEditsByAssetId = state.stagedEditsByAssetId,
                        sortedAssets = sortedAssets,
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
                            onSingleAssetTagSelectionReplace = onSingleAssetTagSelectionReplace,
                            onClearSingleSelectionStaged = onClearSingleSelectionStaged,
                            onBulkDraftChange = onBulkDraftChange,
                            onCreateSessionTagForBulk = onCreateSessionTagForBulk,
                            onCreateSessionTagForAsset = onCreateSessionTagForAsset,
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
                            hasPlan = state.dryRunPlan != null,
                            planMessage = state.dryRunMessage,
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

@Preview(widthDp = 1600, heightDp = 900, showBackground = true)
@Composable
private fun UploadPrepScreenContentPreview(
    @PreviewParameter(UploadPrepScreenPreviewProvider::class) model: UploadPrepScreenPreviewModel
) {
    MaterialTheme {
        UploadPrepScreenContent(
            state = model.state,
            gateStatus = PREVIEW_GATE_STATUS,
            executionPath = PREVIEW_EXECUTION_PATH,
            catalogGateStatus = PREVIEW_CATALOG_STATUS,
            sortedAssets = model.sortedAssets,
            selectedAssets = model.selectedAssets,
            bulkPreflightMessage = model.bulkPreflightMessage,
            onOpenFilePicker = {},
            onSelectAll = {},
            onClearSelection = {},
            onToggleSelection = {},
            onSingleAssetPatch = { _, _ -> },
            onSingleAssetTagSelectionReplace = { _, _, _ -> },
            onClearSingleSelectionStaged = {},
            onBulkDraftChange = {},
            onCreateSessionTagForBulk = {},
            onCreateSessionTagForAsset = { _, _ -> },
            onApplyBulk = {},
            onClearBulkDraft = {},
            onClearSelectedStaged = {},
            onClearCatalogMessage = {},
            onDismissBatchFeedback = {},
            onGeneratePlan = {},
            onClearPlan = {},
            onExecute = {},
            onClearExecutionStatus = {},
            canApplyBulkEdit = { canApplyBulkEdit(it) }
        )
    }
}
