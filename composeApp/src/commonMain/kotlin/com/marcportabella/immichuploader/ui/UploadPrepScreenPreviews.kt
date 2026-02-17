package com.marcportabella.immichuploader.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.marcportabella.immichuploader.domain.UploadPrepState
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.domain.canApplyBulkEdit

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
            onClearExecutionStatus = {},
            canApplyBulkEdit = { canApplyBulkEdit(it) }
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
