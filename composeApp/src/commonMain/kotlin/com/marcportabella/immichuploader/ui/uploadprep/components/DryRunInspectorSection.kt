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
import immichuploader.composeapp.generated.resources.Res
import immichuploader.composeapp.generated.resources.dryrun_album_ops
import immichuploader.composeapp.generated.resources.dryrun_album_ops_ca
import immichuploader.composeapp.generated.resources.dryrun_lookup_hooks
import immichuploader.composeapp.generated.resources.dryrun_lookup_hooks_ca
import immichuploader.composeapp.generated.resources.dryrun_metadata_ops
import immichuploader.composeapp.generated.resources.dryrun_metadata_ops_ca
import immichuploader.composeapp.generated.resources.dryrun_no_payload_preview
import immichuploader.composeapp.generated.resources.dryrun_no_payload_preview_ca
import immichuploader.composeapp.generated.resources.dryrun_payload_line
import immichuploader.composeapp.generated.resources.dryrun_payload_line_ca
import immichuploader.composeapp.generated.resources.dryrun_planned_operations
import immichuploader.composeapp.generated.resources.dryrun_planned_operations_ca
import immichuploader.composeapp.generated.resources.dryrun_request_line
import immichuploader.composeapp.generated.resources.dryrun_request_line_ca
import immichuploader.composeapp.generated.resources.dryrun_step_title
import immichuploader.composeapp.generated.resources.dryrun_step_title_ca
import immichuploader.composeapp.generated.resources.dryrun_tag_ops
import immichuploader.composeapp.generated.resources.dryrun_tag_ops_ca
import immichuploader.composeapp.generated.resources.dryrun_toggle_hide_payload_details
import immichuploader.composeapp.generated.resources.dryrun_toggle_hide_payload_details_ca
import immichuploader.composeapp.generated.resources.dryrun_toggle_show_payload_details
import immichuploader.composeapp.generated.resources.dryrun_toggle_show_payload_details_ca
import immichuploader.composeapp.generated.resources.dryrun_upload_ops
import immichuploader.composeapp.generated.resources.dryrun_upload_ops_ca

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
                    i18nString(
                        english = Res.string.dryrun_step_title,
                        catalan = Res.string.dryrun_step_title_ca
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    i18nString(
                        english = Res.string.dryrun_planned_operations,
                        catalan = Res.string.dryrun_planned_operations_ca,
                        requests.size
                    )
                )

                if (plan != null) {
                    Text(
                        i18nString(
                            english = Res.string.dryrun_upload_ops,
                            catalan = Res.string.dryrun_upload_ops_ca,
                            plan.uploadRequests.size
                        )
                    )
                    Text(
                        i18nString(
                            english = Res.string.dryrun_metadata_ops,
                            catalan = Res.string.dryrun_metadata_ops_ca,
                            plan.bulkMetadataRequests.size
                        )
                    )
                    Text(
                        i18nString(
                            english = Res.string.dryrun_tag_ops,
                            catalan = Res.string.dryrun_tag_ops_ca,
                            plan.tagAssignRequests.size
                        )
                    )
                    Text(
                        i18nString(
                            english = Res.string.dryrun_album_ops,
                            catalan = Res.string.dryrun_album_ops_ca,
                            plan.albumAddRequests.size
                        )
                    )
                    Text(
                        i18nString(
                            english = Res.string.dryrun_lookup_hooks,
                            catalan = Res.string.dryrun_lookup_hooks_ca,
                            plan.lookupHooks.size
                        )
                    )
                }

                if (message != null) {
                    Text(message)
                }

                if (requests.isEmpty()) {
                    Text(
                        i18nString(
                            english = Res.string.dryrun_no_payload_preview,
                            catalan = Res.string.dryrun_no_payload_preview_ca
                        )
                    )
                } else {
                    TextButton(onClick = { showPayloadDetails = !showPayloadDetails }) {
                        Text(
                            if (showPayloadDetails) {
                                i18nString(
                                    english = Res.string.dryrun_toggle_hide_payload_details,
                                    catalan = Res.string.dryrun_toggle_hide_payload_details_ca
                                )
                            } else {
                                i18nString(
                                    english = Res.string.dryrun_toggle_show_payload_details,
                                    catalan = Res.string.dryrun_toggle_show_payload_details_ca
                                )
                            }
                        )
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
                                    Text(
                                        i18nString(
                                            english = Res.string.dryrun_request_line,
                                            catalan = Res.string.dryrun_request_line_ca,
                                            index + 1,
                                            request.method,
                                            request.url
                                        )
                                    )
                                    Text(
                                        i18nString(
                                            english = Res.string.dryrun_payload_line,
                                            catalan = Res.string.dryrun_payload_line_ca,
                                            request.body ?: "<none>"
                                        )
                                    )
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
