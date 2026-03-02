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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import immichuploader.composeapp.generated.resources.Res
import immichuploader.composeapp.generated.resources.summary_header_subtitle
import immichuploader.composeapp.generated.resources.summary_header_subtitle_ca
import immichuploader.composeapp.generated.resources.summary_header_title
import immichuploader.composeapp.generated.resources.summary_header_title_ca
import immichuploader.composeapp.generated.resources.summary_language_catalan
import immichuploader.composeapp.generated.resources.summary_language_catalan_ca
import immichuploader.composeapp.generated.resources.summary_language_english
import immichuploader.composeapp.generated.resources.summary_language_english_ca
import immichuploader.composeapp.generated.resources.summary_language_label
import immichuploader.composeapp.generated.resources.summary_language_label_ca
import immichuploader.composeapp.generated.resources.summary_pill_catalog
import immichuploader.composeapp.generated.resources.summary_pill_catalog_ca
import immichuploader.composeapp.generated.resources.summary_pill_dup_check
import immichuploader.composeapp.generated.resources.summary_pill_dup_check_ca
import immichuploader.composeapp.generated.resources.summary_pill_duplicates
import immichuploader.composeapp.generated.resources.summary_pill_duplicates_ca
import immichuploader.composeapp.generated.resources.summary_pill_execution
import immichuploader.composeapp.generated.resources.summary_pill_execution_ca
import immichuploader.composeapp.generated.resources.summary_pill_in_batch
import immichuploader.composeapp.generated.resources.summary_pill_in_batch_ca
import immichuploader.composeapp.generated.resources.summary_pill_ready
import immichuploader.composeapp.generated.resources.summary_pill_ready_ca
import immichuploader.composeapp.generated.resources.summary_pill_staged
import immichuploader.composeapp.generated.resources.summary_pill_staged_ca
import immichuploader.composeapp.generated.resources.summary_pill_total_picked
import immichuploader.composeapp.generated.resources.summary_pill_total_picked_ca
import immichuploader.composeapp.generated.resources.summary_pill_transport
import immichuploader.composeapp.generated.resources.summary_pill_transport_ca
import immichuploader.composeapp.generated.resources.summary_toggle_hide_system_status
import immichuploader.composeapp.generated.resources.summary_toggle_hide_system_status_ca
import immichuploader.composeapp.generated.resources.summary_toggle_show_system_status
import immichuploader.composeapp.generated.resources.summary_toggle_show_system_status_ca

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SummaryHeaderCard(
    uiLanguage: UiLanguage,
    onUiLanguageChange: (UiLanguage) -> Unit,
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
                        text = i18nString(
                            english = Res.string.summary_header_title,
                            catalan = Res.string.summary_header_title_ca
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = i18nString(
                            english = Res.string.summary_header_subtitle,
                            catalan = Res.string.summary_header_subtitle_ca
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = i18nString(
                                english = Res.string.summary_language_label,
                                catalan = Res.string.summary_language_label_ca
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { onUiLanguageChange(UiLanguage.Catalan) },
                            enabled = uiLanguage != UiLanguage.Catalan
                        ) {
                            Text(
                                i18nString(
                                    english = Res.string.summary_language_catalan,
                                    catalan = Res.string.summary_language_catalan_ca
                                )
                            )
                        }
                        TextButton(
                            onClick = { onUiLanguageChange(UiLanguage.English) },
                            enabled = uiLanguage != UiLanguage.English
                        ) {
                            Text(
                                i18nString(
                                    english = Res.string.summary_language_english,
                                    catalan = Res.string.summary_language_english_ca
                                )
                            )
                        }
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryPill(
                            i18nString(
                                english = Res.string.summary_pill_in_batch,
                                catalan = Res.string.summary_pill_in_batch_ca
                            ),
                            batchCount.toString()
                        )
                        SummaryPill(
                            i18nString(
                                english = Res.string.summary_pill_ready,
                                catalan = Res.string.summary_pill_ready_ca
                            ),
                            (batchCount - duplicateCount).coerceAtLeast(0).toString()
                        )
                        SummaryPill(
                            i18nString(
                                english = Res.string.summary_pill_staged,
                                catalan = Res.string.summary_pill_staged_ca
                            ),
                            stagedCount.toString()
                        )
                        SummaryPill(
                            i18nString(
                                english = Res.string.summary_pill_duplicates,
                                catalan = Res.string.summary_pill_duplicates_ca
                            ),
                            duplicateCount.toString()
                        )
                        SummaryPill(
                            i18nString(
                                english = Res.string.summary_pill_total_picked,
                                catalan = Res.string.summary_pill_total_picked_ca
                            ),
                            assetCount.toString()
                        )
                    }
                    TextButton(onClick = { showSystemStatus = !showSystemStatus }) {
                        Text(
                            if (showSystemStatus) {
                                i18nString(
                                    english = Res.string.summary_toggle_hide_system_status,
                                    catalan = Res.string.summary_toggle_hide_system_status_ca
                                )
                            } else {
                                i18nString(
                                    english = Res.string.summary_toggle_show_system_status,
                                    catalan = Res.string.summary_toggle_show_system_status_ca
                                )
                            }
                        )
                    }
                    if (showSystemStatus) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryPill(
                                i18nString(
                                    english = Res.string.summary_pill_dup_check,
                                    catalan = Res.string.summary_pill_dup_check_ca
                                ),
                                duplicateStatus
                            )
                            SummaryPill(
                                i18nString(
                                    english = Res.string.summary_pill_transport,
                                    catalan = Res.string.summary_pill_transport_ca
                                ),
                                gateStatus
                            )
                            SummaryPill(
                                i18nString(
                                    english = Res.string.summary_pill_execution,
                                    catalan = Res.string.summary_pill_execution_ca
                                ),
                                executionPath
                            )
                            SummaryPill(
                                i18nString(
                                    english = Res.string.summary_pill_catalog,
                                    catalan = Res.string.summary_pill_catalog_ca
                                ),
                                catalogGateStatus
                            )
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
            uiLanguage = UiLanguage.Catalan,
            onUiLanguageChange = {},
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
