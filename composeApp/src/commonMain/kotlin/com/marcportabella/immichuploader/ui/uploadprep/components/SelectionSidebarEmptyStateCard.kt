package com.marcportabella.immichuploader.ui.uploadprep

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

@Composable
internal fun SelectionSidebarEmptyStateCard(
    albumsCount: Int,
    tagsCount: Int,
    catalogMessage: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Step 2 · Edit Batch Metadata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Pick media first to enable batch editing.")
            Text("When a batch is loaded, edits from this panel apply to all non-duplicate assets.")
            Text("Albums loaded: $albumsCount | Tags loaded: $tagsCount")
            catalogMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
