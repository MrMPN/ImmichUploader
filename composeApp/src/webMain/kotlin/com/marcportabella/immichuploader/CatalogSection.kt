package com.marcportabella.immichuploader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    Text("Immich tags/albums")
    Text("Catalog status: ${state.catalogStatus}")

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onCreateAlbum) {
            Text("Create album")
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
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onCreateTag) {
            Text("Create tag")
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.catalogMessage)
            Button(onClick = onClearMessage) {
                Text("Dismiss")
            }
        }
    }
}
