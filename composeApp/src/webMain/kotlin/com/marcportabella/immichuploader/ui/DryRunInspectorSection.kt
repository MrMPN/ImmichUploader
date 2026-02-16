package com.marcportabella.immichuploader.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.marcportabella.immichuploader.domain.UploadPrepState

@Composable
fun DryRunInspectorSection(state: UploadPrepState) {
    Text("Dry-run payload inspector")
    val plan = state.dryRunPlan
    val requests = state.dryRunApiRequests
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("${index + 1}. ${request.method} ${request.url}")
                Text("Payload: ${request.body ?: "<none>"}")
            }
        }
    }
}
