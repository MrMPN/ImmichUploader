package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.UploadExecutionStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequestPlanExecutionCard(
    hasPlan: Boolean,
    planMessage: String?,
    executionStatus: UploadExecutionStatus,
    executionMessage: String?,
    executionRequestCount: Int?,
    onGeneratePlan: () -> Unit,
    onClearPlan: () -> Unit,
    onExecute: () -> Unit,
    onClearExecutionStatus: () -> Unit
) {
    var showTechnicalStatus by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Step 3 · Plan and Upload",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Generate the request plan, review it, then execute the upload.",
                style = MaterialTheme.typography.bodySmall
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onGeneratePlan) { Text("Generate plan") }
                Button(
                    onClick = onExecute,
                    enabled = hasPlan && executionStatus != UploadExecutionStatus.Executing
                ) { Text("Execute upload") }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onClearPlan, enabled = hasPlan) { Text("Clear plan") }
                Button(
                    onClick = onClearExecutionStatus,
                    enabled = executionMessage != null || executionStatus != UploadExecutionStatus.Idle
                ) { Text("Clear status") }
            }
            TextButton(onClick = { showTechnicalStatus = !showTechnicalStatus }) {
                Text(if (showTechnicalStatus) "Hide technical status" else "Show technical status")
            }

            if (showTechnicalStatus) {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (planMessage != null) Text("Plan message: $planMessage")
                    Text("Execution status: $executionStatus")
                    if (executionMessage != null) Text("Execution message: $executionMessage")
                    if (executionRequestCount != null) Text("Submitted requests: $executionRequestCount")
                }
            }
        }
    }
}

@Preview
@Composable
private fun RequestPlanExecutionCardPreview() {
    MaterialTheme {
        RequestPlanExecutionCard(
            hasPlan = true,
            planMessage = "Dry-run generated 2 operations.",
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
