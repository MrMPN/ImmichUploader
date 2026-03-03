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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.UploadPrepState
import com.marcportabella.immichuploader.domain.canApplyBulkEdit

@Composable
internal fun UploadPrepScreenContent(
    state: UploadPrepState,
    uiLanguage: UiLanguage,
    onUiLanguageChange: (UiLanguage) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onServerBaseUrlChange: (String) -> Unit,
    keyOwnerName: String?,
    keyOwnerLookupInProgress: Boolean,
    keyOwnerLookupFailed: Boolean,
    gateStatus: String,
    executionPath: String,
    catalogGateStatus: String,
    sortedAssets: List<LocalAsset>,
    bulkPreflightMessage: String?,
    onOpenFilePicker: () -> Unit,
    onBulkDraftChange: (BulkEditDraft) -> Unit,
    onCreateSessionAlbumForBulk: (String) -> Unit,
    onCreateSessionTagForBulk: (String) -> Unit,
    onApplyBulk: () -> Unit,
    onClearBulkDraft: () -> Unit,
    onClearBatchStaged: () -> Unit,
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
        val queueColumns = if (isNarrow) 4 else 5
        val queueRows = remember(sortedAssets, queueColumns) {
            sortedAssets.chunked(queueColumns.coerceAtLeast(1))
        }

        if (isNarrow) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SummaryHeaderCard(
                        uiLanguage = uiLanguage,
                        onUiLanguageChange = onUiLanguageChange,
                        apiKey = state.apiKey,
                        onApiKeyChange = onApiKeyChange,
                        serverBaseUrl = state.serverBaseUrl,
                        onServerBaseUrlChange = onServerBaseUrlChange,
                        keyOwnerName = keyOwnerName,
                        keyOwnerLookupInProgress = keyOwnerLookupInProgress,
                        keyOwnerLookupFailed = keyOwnerLookupFailed,
                        assetCount = state.assets.size,
                        batchCount = state.selectedAssetIds.size,
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
                        duplicateCheckMessage = state.duplicateCheckMessage,
                        onOpenFilePicker = onOpenFilePicker
                    )
                }
                assetQueueSection(
                    duplicateAssetIds = state.duplicateAssetIds,
                    stagedEditsByAssetId = state.stagedEditsByAssetId,
                    rows = queueRows,
                    columnCount = queueColumns
                )
                item {
                    SelectionSidebarPane(
                        hasAssets = state.assets.isNotEmpty(),
                        batchAssetCount = state.selectedAssetIds.size,
                        bulkDraft = state.bulkEditDraft,
                        applyEnabled = canApplyBulkEdit(state),
                        availableAlbums = state.availableAlbums,
                        availableTags = state.availableTags,
                        catalogMessage = state.catalogMessage,
                        preflightMessage = bulkPreflightMessage,
                        onBulkDraftChange = onBulkDraftChange,
                        onCreateSessionAlbumForBulk = onCreateSessionAlbumForBulk,
                        onCreateSessionTagForBulk = onCreateSessionTagForBulk,
                        onApplyBulk = onApplyBulk,
                        onClearBulkDraft = onClearBulkDraft,
                        onClearBatchStaged = onClearBatchStaged,
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
                            uiLanguage = uiLanguage,
                            onUiLanguageChange = onUiLanguageChange,
                            apiKey = state.apiKey,
                            onApiKeyChange = onApiKeyChange,
                            serverBaseUrl = state.serverBaseUrl,
                            onServerBaseUrlChange = onServerBaseUrlChange,
                            keyOwnerName = keyOwnerName,
                            keyOwnerLookupInProgress = keyOwnerLookupInProgress,
                            keyOwnerLookupFailed = keyOwnerLookupFailed,
                            assetCount = state.assets.size,
                            batchCount = state.selectedAssetIds.size,
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
                            duplicateCheckMessage = state.duplicateCheckMessage,
                            onOpenFilePicker = onOpenFilePicker
                        )
                    }
                    assetQueueSection(
                        duplicateAssetIds = state.duplicateAssetIds,
                        stagedEditsByAssetId = state.stagedEditsByAssetId,
                        rows = queueRows,
                        columnCount = queueColumns
                    )
                }
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SelectionSidebarPane(
                            hasAssets = state.assets.isNotEmpty(),
                            batchAssetCount = state.selectedAssetIds.size,
                            bulkDraft = state.bulkEditDraft,
                            applyEnabled = canApplyBulkEdit(state),
                            availableAlbums = state.availableAlbums,
                            availableTags = state.availableTags,
                            catalogMessage = state.catalogMessage,
                            preflightMessage = bulkPreflightMessage,
                            onBulkDraftChange = onBulkDraftChange,
                            onCreateSessionAlbumForBulk = onCreateSessionAlbumForBulk,
                            onCreateSessionTagForBulk = onCreateSessionTagForBulk,
                            onApplyBulk = onApplyBulk,
                            onClearBulkDraft = onClearBulkDraft,
                            onClearBatchStaged = onClearBatchStaged,
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
            uiLanguage = UiLanguage.Catalan,
            onUiLanguageChange = {},
            onApiKeyChange = {},
            onServerBaseUrlChange = {},
            keyOwnerName = null,
            keyOwnerLookupInProgress = false,
            keyOwnerLookupFailed = false,
            gateStatus = PREVIEW_GATE_STATUS,
            executionPath = PREVIEW_EXECUTION_PATH,
            catalogGateStatus = PREVIEW_CATALOG_STATUS,
            sortedAssets = model.sortedAssets,
            bulkPreflightMessage = model.bulkPreflightMessage,
            onOpenFilePicker = {},
            onBulkDraftChange = {},
            onCreateSessionAlbumForBulk = {},
            onCreateSessionTagForBulk = {},
            onApplyBulk = {},
            onClearBulkDraft = {},
            onClearBatchStaged = {},
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
