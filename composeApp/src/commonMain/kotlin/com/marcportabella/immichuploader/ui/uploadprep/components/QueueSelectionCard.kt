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
    hasSelection: Boolean,
    onOpenFilePicker: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Queue selection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Load local media, review in the explorer pane, then use the sidebar for edit details.",
                style = MaterialTheme.typography.bodySmall
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenFilePicker) { Text("Select local media") }
                Button(onClick = onSelectAll, enabled = hasAssets) { Text("Select all") }
                Button(onClick = onClearSelection, enabled = hasSelection) { Text("Clear selection") }
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
            hasSelection = true,
            onOpenFilePicker = {},
            onSelectAll = {},
            onClearSelection = {}
        )
    }
}
