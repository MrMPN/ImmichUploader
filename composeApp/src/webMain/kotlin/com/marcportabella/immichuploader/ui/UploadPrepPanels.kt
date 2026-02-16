package com.marcportabella.immichuploader.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.UploadExecutionStatus
import com.marcportabella.immichuploader.domain.UploadPrepState

@Composable
fun SummaryHeaderCard(
    assetCount: Int,
    selectedCount: Int,
    stagedCount: Int,
    gateStatus: String,
    executionPath: String,
    catalogGateStatus: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Immich Upload Prep",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Prepare metadata, validate the request payload, then execute upload safely.",
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryPill("Assets", assetCount.toString())
                SummaryPill("Selected", selectedCount.toString())
                SummaryPill("Staged", stagedCount.toString())
                SummaryPill("Transport", gateStatus)
                SummaryPill("Execution", executionPath)
                SummaryPill("Catalog", catalogGateStatus)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QueueSelectionCard(
    state: UploadPrepState,
    onOpenFilePicker: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Queue selection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Load local media, review in the explorer pane, then use the sidebar for edit details.",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenFilePicker) {
                    Text("Select local media")
                }
                Button(
                    onClick = onSelectAll,
                    enabled = state.assets.isNotEmpty()
                ) {
                    Text("Select all")
                }
                Button(
                    onClick = onClearSelection,
                    enabled = state.selectedAssetIds.isNotEmpty()
                ) {
                    Text("Clear selection")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequestPlanExecutionCard(
    state: UploadPrepState,
    onGeneratePlan: () -> Unit,
    onClearPlan: () -> Unit,
    onExecute: () -> Unit,
    onClearExecutionStatus: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Request plan and execution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Generate a request plan preview before execution to verify payload details and request count.",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onGeneratePlan,
                    enabled = state.selectedAssetIds.isNotEmpty()
                ) {
                    Text("Generate request plan")
                }
                Button(
                    onClick = onClearPlan,
                    enabled = state.dryRunPlan != null
                ) {
                    Text("Clear plan")
                }
                Button(
                    onClick = onExecute,
                    enabled = state.dryRunPlan != null && state.executionStatus != UploadExecutionStatus.Executing
                ) {
                    Text("Execute API upload")
                }
                Button(
                    onClick = onClearExecutionStatus,
                    enabled = state.executionMessage != null || state.executionStatus != UploadExecutionStatus.Idle
                ) {
                    Text("Clear execution status")
                }
            }

            HorizontalDivider()
            Text("Execution status: ${state.executionStatus}")
            if (state.executionMessage != null) {
                Text("Execution message: ${state.executionMessage}")
            }
            if (state.executionRequestCount != null) {
                Text("Submitted requests: ${state.executionRequestCount}")
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "$label:", style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
