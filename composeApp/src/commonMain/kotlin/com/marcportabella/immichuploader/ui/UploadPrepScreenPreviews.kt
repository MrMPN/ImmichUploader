package com.marcportabella.immichuploader.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.Preview
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.domain.canApplyBulkEdit

@Preview
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
            onClearExecutionStatus = {},
            canApplyBulkEdit = { canApplyBulkEdit(it) }
        )
    }
}

@Preview
@Composable
private fun UploadPrepScreenRoutePreview(
    @PreviewParameter(UploadPrepScreenPreviewProvider::class) model: UploadPrepScreenPreviewModel
) {
    val previewStore = UploadPrepStore(model.state)
    MaterialTheme {
        UploadPrepScreen(
            store = previewStore,
            enableWebEffects = false
        )
    }
}
