package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryHeaderCard(
    assetCount: Int,
    batchCount: Int,
    stagedCount: Int,
    duplicateCount: Int,
    duplicateStatus: String,
    gateStatus: String,
    executionPath: String,
    catalogGateStatus: String
) {
    var showSystemStatus by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        SummaryPill("In Batch", batchCount.toString())
                        SummaryPill("Ready", (batchCount - duplicateCount).coerceAtLeast(0).toString())
                        SummaryPill("Staged", stagedCount.toString())
                        SummaryPill("Duplicates", duplicateCount.toString())
                        SummaryPill("Total Picked", assetCount.toString())
                    }
                    TextButton(onClick = { showSystemStatus = !showSystemStatus }) {
                        Text(if (showSystemStatus) "Hide system status" else "Show system status")
                    }
                    if (showSystemStatus) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryPill("Dup Check", duplicateStatus)
                            SummaryPill("Transport", gateStatus)
                            SummaryPill("Execution", executionPath)
                            SummaryPill("Catalog", catalogGateStatus)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RectangleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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

@Preview
@Composable
private fun SummaryHeaderCardPreview() {
    MaterialTheme {
        SummaryHeaderCard(
            assetCount = 12,
            batchCount = 11,
            stagedCount = 2,
            duplicateCount = 1,
            duplicateStatus = "Ready",
            gateStatus = PREVIEW_GATE_STATUS,
            executionPath = PREVIEW_EXECUTION_PATH,
            catalogGateStatus = PREVIEW_CATALOG_STATUS
        )
    }
}
