package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.UploadApiRequest
import com.marcportabella.immichuploader.domain.UploadRequestPlan

@Composable
fun DryRunInspectorSection(
    plan: UploadRequestPlan?,
    requests: List<UploadApiRequest>,
    message: String?
) {
    var showPayloadDetails by rememberSaveable { mutableStateOf(false) }
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Step 4 · Review Plan Details",
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

                if (message != null) {
                    Text(message)
                }

                if (requests.isEmpty()) {
                    Text("No payload preview available yet.")
                } else {
                    TextButton(onClick = { showPayloadDetails = !showPayloadDetails }) {
                        Text(if (showPayloadDetails) "Hide payload details" else "Show payload details")
                    }
                    if (showPayloadDetails) {
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
    }
}

@Preview
@Composable
private fun DryRunInspectorSectionPreview(
    @PreviewParameter(DryRunPreviewProvider::class) model: DryRunPreviewModel
) {
    MaterialTheme {
        DryRunInspectorSection(
            plan = model.plan,
            requests = model.requests,
            message = model.message
        )
    }
}
