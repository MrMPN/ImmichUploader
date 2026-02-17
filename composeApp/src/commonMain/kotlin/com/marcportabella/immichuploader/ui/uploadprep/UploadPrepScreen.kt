package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.platform.BindPlatformFileInput
import com.marcportabella.immichuploader.platform.openPlatformFilePicker
import kotlinx.coroutines.launch

@Composable
fun UploadPrepScreen(
    store: UploadPrepStore,
    enableWebEffects: Boolean = true
) {
    val stateHolder = rememberUploadPrepStateHolder(store)
    val state = stateHolder.state
    val scope = rememberCoroutineScope()

    if (enableWebEffects) {
        BindPlatformFileInput { nextFiles ->
            scope.launch { stateHolder.onFilesSelected(nextFiles) }
        }
    }

    if (enableWebEffects) {
        LaunchedEffect(state.apiKey) {
            stateHolder.loadCatalogAtInit()
        }
    }

    UploadPrepScreenContent(
        state = state,
        gateStatus = stateHolder.gateStatus,
        executionPath = stateHolder.executionPath,
        catalogGateStatus = stateHolder.catalogGateStatus,
        sortedAssets = stateHolder.sortedAssets,
        selectedAssets = stateHolder.selectedAssets,
        bulkPreflightMessage = stateHolder.bulkPreflightMessage,
        onOpenFilePicker = { openPlatformFilePicker() },
        onSelectAll = { stateHolder.selectAll() },
        onClearSelection = { stateHolder.clearSelection() },
        onToggleSelection = { stateHolder.toggleSelection(it) },
        onSingleAssetPatch = { assetId, patch -> stateHolder.patchSingleAsset(assetId, patch) },
        onClearSingleSelectionStaged = { stateHolder.clearSingleSelectionStaged() },
        onBulkDraftChange = { draft -> stateHolder.updateBulkDraft(draft) },
        onApplyBulk = { stateHolder.applyBulk() },
        onClearBulkDraft = { stateHolder.clearBulkDraft() },
        onClearSelectedStaged = { stateHolder.clearSelectedStaged() },
        onClearCatalogMessage = { stateHolder.clearCatalogMessage() },
        onDismissBatchFeedback = { stateHolder.dismissBatchFeedback() },
        onGeneratePlan = { stateHolder.generatePlan() },
        onClearPlan = { stateHolder.clearPlan() },
        onExecute = { scope.launch { stateHolder.executePlan() } },
        onClearExecutionStatus = { stateHolder.clearExecutionStatus() },
        canApplyBulkEdit = { stateHolder.canApplyBulkEdit() }
    )
}

@Preview(widthDp = 1600, heightDp = 900, showBackground = true)
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
