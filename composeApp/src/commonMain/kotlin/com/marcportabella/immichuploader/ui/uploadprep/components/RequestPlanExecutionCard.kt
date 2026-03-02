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
import immichuploader.composeapp.generated.resources.Res
import immichuploader.composeapp.generated.resources.request_button_clear_plan
import immichuploader.composeapp.generated.resources.request_button_clear_plan_ca
import immichuploader.composeapp.generated.resources.request_button_clear_status
import immichuploader.composeapp.generated.resources.request_button_clear_status_ca
import immichuploader.composeapp.generated.resources.request_button_execute_upload
import immichuploader.composeapp.generated.resources.request_button_execute_upload_ca
import immichuploader.composeapp.generated.resources.request_button_generate_plan
import immichuploader.composeapp.generated.resources.request_button_generate_plan_ca
import immichuploader.composeapp.generated.resources.request_execution_message
import immichuploader.composeapp.generated.resources.request_execution_message_ca
import immichuploader.composeapp.generated.resources.request_execution_status
import immichuploader.composeapp.generated.resources.request_execution_status_ca
import immichuploader.composeapp.generated.resources.request_plan_message
import immichuploader.composeapp.generated.resources.request_plan_message_ca
import immichuploader.composeapp.generated.resources.request_step_description
import immichuploader.composeapp.generated.resources.request_step_description_ca
import immichuploader.composeapp.generated.resources.request_step_title
import immichuploader.composeapp.generated.resources.request_step_title_ca
import immichuploader.composeapp.generated.resources.request_submitted_requests
import immichuploader.composeapp.generated.resources.request_submitted_requests_ca
import immichuploader.composeapp.generated.resources.request_toggle_hide_technical_status
import immichuploader.composeapp.generated.resources.request_toggle_hide_technical_status_ca
import immichuploader.composeapp.generated.resources.request_toggle_show_technical_status
import immichuploader.composeapp.generated.resources.request_toggle_show_technical_status_ca

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
                i18nString(
                    english = Res.string.request_step_title,
                    catalan = Res.string.request_step_title_ca
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                i18nString(
                    english = Res.string.request_step_description,
                    catalan = Res.string.request_step_description_ca
                ),
                style = MaterialTheme.typography.bodySmall
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onGeneratePlan) {
                    Text(
                        i18nString(
                            english = Res.string.request_button_generate_plan,
                            catalan = Res.string.request_button_generate_plan_ca
                        )
                    )
                }
                Button(
                    onClick = onExecute,
                    enabled = hasPlan && executionStatus != UploadExecutionStatus.Executing
                ) {
                    Text(
                        i18nString(
                            english = Res.string.request_button_execute_upload,
                            catalan = Res.string.request_button_execute_upload_ca
                        )
                    )
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onClearPlan, enabled = hasPlan) {
                    Text(
                        i18nString(
                            english = Res.string.request_button_clear_plan,
                            catalan = Res.string.request_button_clear_plan_ca
                        )
                    )
                }
                Button(
                    onClick = onClearExecutionStatus,
                    enabled = executionMessage != null || executionStatus != UploadExecutionStatus.Idle
                ) {
                    Text(
                        i18nString(
                            english = Res.string.request_button_clear_status,
                            catalan = Res.string.request_button_clear_status_ca
                        )
                    )
                }
            }
            TextButton(onClick = { showTechnicalStatus = !showTechnicalStatus }) {
                Text(
                    if (showTechnicalStatus) {
                        i18nString(
                            english = Res.string.request_toggle_hide_technical_status,
                            catalan = Res.string.request_toggle_hide_technical_status_ca
                        )
                    } else {
                        i18nString(
                            english = Res.string.request_toggle_show_technical_status,
                            catalan = Res.string.request_toggle_show_technical_status_ca
                        )
                    }
                )
            }

            if (showTechnicalStatus) {
                HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (planMessage != null) {
                        Text(
                            i18nString(
                                english = Res.string.request_plan_message,
                                catalan = Res.string.request_plan_message_ca,
                                planMessage
                            )
                        )
                    }
                    Text(
                        i18nString(
                            english = Res.string.request_execution_status,
                            catalan = Res.string.request_execution_status_ca,
                            executionStatus.toString()
                        )
                    )
                    if (executionMessage != null) {
                        Text(
                            i18nString(
                                english = Res.string.request_execution_message,
                                catalan = Res.string.request_execution_message_ca,
                                executionMessage
                            )
                        )
                    }
                    if (executionRequestCount != null) {
                        Text(
                            i18nString(
                                english = Res.string.request_submitted_requests,
                                catalan = Res.string.request_submitted_requests_ca,
                                executionRequestCount
                            )
                        )
                    }
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
