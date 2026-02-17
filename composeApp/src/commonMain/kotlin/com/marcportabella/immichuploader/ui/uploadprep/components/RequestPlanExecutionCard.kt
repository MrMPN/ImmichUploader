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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.UploadExecutionStatus

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RequestPlanExecutionCard(
    hasSelection: Boolean,
    hasPlan: Boolean,
    executionStatus: UploadExecutionStatus,
    executionMessage: String?,
    executionRequestCount: Int?,
    onGeneratePlan: () -> Unit,
    onClearPlan: () -> Unit,
    onExecute: () -> Unit,
    onClearExecutionStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
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
                Button(onClick = onGeneratePlan, enabled = hasSelection) { Text("Generate request plan") }
                Button(onClick = onClearPlan, enabled = hasPlan) { Text("Clear plan") }
                Button(
                    onClick = onExecute,
                    enabled = hasPlan && executionStatus != UploadExecutionStatus.Executing
                ) { Text("Execute API upload") }
                Button(
                    onClick = onClearExecutionStatus,
                    enabled = executionMessage != null || executionStatus != UploadExecutionStatus.Idle
                ) { Text("Clear execution status") }
            }

            HorizontalDivider()
            Text("Execution status: $executionStatus")
            if (executionMessage != null) Text("Execution message: $executionMessage")
            if (executionRequestCount != null) Text("Submitted requests: $executionRequestCount")
        }
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
