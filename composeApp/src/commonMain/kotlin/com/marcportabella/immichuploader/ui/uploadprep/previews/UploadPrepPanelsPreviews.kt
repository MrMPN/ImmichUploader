package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun SummaryHeaderCardPreview() {
    MaterialTheme {
        SummaryHeaderCard(
            assetCount = 12,
            selectedCount = 3,
            stagedCount = 2,
            gateStatus = PREVIEW_GATE_STATUS,
            executionPath = PREVIEW_EXECUTION_PATH,
            catalogGateStatus = PREVIEW_CATALOG_STATUS
        )
    }
}

@Preview
@Composable
private fun QueueSelectionCardPreview() {
    MaterialTheme {
        QueueSelectionCard(
            hasAssets = true,
            hasSelection = true,
            onOpenFilePicker = {},
            onSelectAll = {},
            onClearSelection = {}
        )
    }
}

@Preview
@Composable
private fun RequestPlanExecutionCardPreview() {
    MaterialTheme {
        RequestPlanExecutionCard(
            hasSelection = true,
            hasPlan = true,
            executionStatus = PREVIEW_EXECUTION_STATUS,
            executionMessage = PREVIEW_EXECUTION_MESSAGE,
            executionRequestCount = 2,
            onGeneratePlan = {},
            onClearPlan = {},
            onExecute = {},
            onClearExecutionStatus = {}
        )
    }
}
