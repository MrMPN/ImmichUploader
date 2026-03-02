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
                "Step 1 · Pick Batch Media",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Pick photos/videos from this device. Everything picked becomes one batch.",
                style = MaterialTheme.typography.bodySmall
            )
            duplicateCheckMessage?.let { message ->
                Text(
                    text = "Duplicate check: $message",
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
                    Text(if (hasAssets) "Replace batch media" else "Select local media")
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
