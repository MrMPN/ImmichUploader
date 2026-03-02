package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import immichuploader.composeapp.generated.resources.Res
import immichuploader.composeapp.generated.resources.queue_button_replace_batch_media
import immichuploader.composeapp.generated.resources.queue_button_replace_batch_media_ca
import immichuploader.composeapp.generated.resources.queue_button_select_local_media
import immichuploader.composeapp.generated.resources.queue_button_select_local_media_ca
import immichuploader.composeapp.generated.resources.queue_duplicate_check_prefix
import immichuploader.composeapp.generated.resources.queue_duplicate_check_prefix_ca
import immichuploader.composeapp.generated.resources.queue_step_description
import immichuploader.composeapp.generated.resources.queue_step_description_ca
import immichuploader.composeapp.generated.resources.queue_step_title
import immichuploader.composeapp.generated.resources.queue_step_title_ca

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QueueSelectionCard(
    hasAssets: Boolean,
    duplicateCheckMessage: String?,
    onOpenFilePicker: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                i18nString(
                    english = Res.string.queue_step_title,
                    catalan = Res.string.queue_step_title_ca
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                i18nString(
                    english = Res.string.queue_step_description,
                    catalan = Res.string.queue_step_description_ca
                ),
                style = MaterialTheme.typography.bodySmall
            )
            duplicateCheckMessage?.let { message ->
                Text(
                    text = i18nString(
                        english = Res.string.queue_duplicate_check_prefix,
                        catalan = Res.string.queue_duplicate_check_prefix_ca,
                        message
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenFilePicker) {
                    Text(
                        if (hasAssets) {
                            i18nString(
                                english = Res.string.queue_button_replace_batch_media,
                                catalan = Res.string.queue_button_replace_batch_media_ca
                            )
                        } else {
                            i18nString(
                                english = Res.string.queue_button_select_local_media,
                                catalan = Res.string.queue_button_select_local_media_ca
                            )
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun QueueSelectionCardPreview() {
    MaterialTheme {
        QueueSelectionCard(
            hasAssets = true,
            duplicateCheckMessage = "Duplicate check completed.",
            onOpenFilePicker = {}
        )
    }
}
