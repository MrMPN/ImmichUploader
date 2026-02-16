package com.marcportabella.immichuploader.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.UploadPrepState

@Composable
fun DryRunInspectorSection(state: UploadPrepState) {
    val plan = state.dryRunPlan
    val requests = state.dryRunApiRequests

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Request payload inspector",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text("Planned operations: ${requests.size}")

            if (plan != null) {
                Text("Upload ops: ${plan.uploadRequests.size}")
                Text("Metadata ops: ${plan.bulkMetadataRequests.size}")
                Text("Tag ops: ${plan.tagAssignRequests.size}")
                Text("Album ops: ${plan.albumAddRequests.size}")
                Text("Lookup hooks: ${plan.lookupHooks.size}")
            }

            if (state.dryRunMessage != null) {
                Text(state.dryRunMessage)
            }

            if (requests.isEmpty()) {
                Text("No payload preview available yet.")
            } else {
                requests.forEachIndexed { index, request ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("${index + 1}. ${request.method} ${request.url}")
                            Text("Payload: ${request.body ?: "<none>"}")
                        }
                    }
                }
            }
        }
    }
}
