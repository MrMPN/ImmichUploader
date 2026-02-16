package com.marcportabella.immichuploader.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.UploadPrepState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogSection(
    state: UploadPrepState,
    onLookupAlbums: () -> Unit,
    onLookupTags: () -> Unit,
    onAlbumDraftChange: (String) -> Unit,
    onTagDraftChange: (String) -> Unit,
    onCreateAlbum: () -> Unit,
    onCreateTag: () -> Unit,
    onClearMessage: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Immich catalog",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Load existing albums/tags and create missing entries from this panel.",
                style = MaterialTheme.typography.bodySmall
            )
            Text("Catalog status: ${state.catalogStatus}")

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onLookupAlbums) {
                    Text("Load albums")
                }
                Button(onClick = onLookupTags) {
                    Text("Load tags")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.albumCreateDraft,
                    onValueChange = onAlbumDraftChange,
                    label = { Text("Create album if missing") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onCreateAlbum) {
                    Text("Create")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.tagCreateDraft,
                    onValueChange = onTagDraftChange,
                    label = { Text("Create tag if missing") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onCreateTag) {
                    Text("Create")
                }
            }

            Text("Albums loaded: ${state.availableAlbums.size}")
            Text("Tags loaded: ${state.availableTags.size}")
            Text(
                "Album names: ${
                    if (state.availableAlbums.isEmpty()) "None"
                    else state.availableAlbums.joinToString(", ") { it.name }
                }"
            )
            Text(
                "Tag names: ${
                    if (state.availableTags.isEmpty()) "None"
                    else state.availableTags.joinToString(", ") { it.name }
                }"
            )

            if (state.catalogMessage != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(state.catalogMessage, modifier = Modifier.weight(1f))
                    Button(onClick = onClearMessage) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}
