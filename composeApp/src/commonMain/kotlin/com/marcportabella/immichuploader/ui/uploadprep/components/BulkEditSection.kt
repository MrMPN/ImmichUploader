package com.marcportabella.immichuploader.ui.uploadprep

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.UploadCatalogEntry

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulkEditSection(
    draft: BulkEditDraft,
    batchAssetCount: Int,
    applyEnabled: Boolean,
    preflightMessage: String?,
    availableAlbums: List<UploadCatalogEntry>,
    availableTags: List<UploadCatalogEntry>,
    selectedTagIds: Set<String>,
    onDraftChange: (BulkEditDraft) -> Unit,
    onCreateSessionAlbum: (String) -> Unit,
    onCreateSessionTag: (String) -> Unit,
    onApply: () -> Unit,
    onClearDraft: () -> Unit,
    onClearBatchStaged: () -> Unit
) {
    var newAlbumName by rememberSaveable { mutableStateOf("") }
    var newTagName by rememberSaveable { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Bulk edit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Batch size: $batchAssetCount",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Toggle each field to include it in the patch before applying to the whole batch.",
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = draft.includeDescription,
                    onCheckedChange = { onDraftChange(draft.copy(includeDescription = it)) }
                )
                OutlinedTextField(
                    value = draft.description,
                    onValueChange = { onDraftChange(draft.copy(description = it)) },
                    label = { Text("Description") },
                    modifier = Modifier.weight(1f)
                )
            }

            SelectableChipGroup(
                title = "Choose album",
                options = availableAlbums.map { ChipOption(id = it.id, label = it.name) },
                selectedIds = if (draft.includeAlbumId && draft.albumId.isNotBlank()) setOf(draft.albumId) else emptySet(),
                onToggle = { albumId ->
                    val selected = draft.includeAlbumId && draft.albumId == albumId
                    onDraftChange(
                        if (selected) {
                            draft.copy(includeAlbumId = false, albumId = "")
                        } else {
                            draft.copy(includeAlbumId = true, albumId = albumId)
                        }
                    )
                }
            )

            OutlinedTextField(
                value = newAlbumName,
                onValueChange = { newAlbumName = it },
                label = { Text("New session album") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val name = newAlbumName.trim()
                    if (name.isNotEmpty()) {
                        onCreateSessionAlbum(name)
                        newAlbumName = ""
                    }
                },
                enabled = newAlbumName.isNotBlank()
            ) {
                Text("Create and select album")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = draft.includeTimeZone,
                    onCheckedChange = { onDraftChange(draft.copy(includeTimeZone = it)) }
                )
                TimeZoneDropdownField(
                    value = draft.timeZone,
                    onValueChange = { onDraftChange(draft.copy(timeZone = it)) },
                    label = "Timezone (IANA, +02:00, or Z)",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = draft.includeDateTimeOriginal,
                    onCheckedChange = { onDraftChange(draft.copy(includeDateTimeOriginal = it)) }
                )
                OutlinedTextField(
                    value = draft.dateTimeOriginal,
                    onValueChange = { onDraftChange(draft.copy(dateTimeOriginal = it)) },
                    label = { Text("Date/time original (ISO 8601)") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = draft.includeAlbumId,
                    onCheckedChange = { onDraftChange(draft.copy(includeAlbumId = it)) }
                )
                OutlinedTextField(
                    value = draft.albumId,
                    onValueChange = { onDraftChange(draft.copy(albumId = it)) },
                    label = { Text("Album ID") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = draft.includeFavorite,
                    onCheckedChange = { onDraftChange(draft.copy(includeFavorite = it)) }
                )
                Button(onClick = { onDraftChange(draft.copy(isFavorite = !draft.isFavorite)) }) {
                    Text("Favorite = ${draft.isFavorite}")
                }
            }

            OutlinedTextField(
                value = draft.addTagIds,
                onValueChange = { onDraftChange(draft.copy(addTagIds = it)) },
                label = { Text("Add tag IDs (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            SelectableChipGroup(
                title = "Select tags to add",
                options = availableTags.map { ChipOption(id = it.id, label = it.name) },
                selectedIds = selectedTagIds,
                onToggle = { tagId ->
                    val selected = tagId in selectedTagIds
                    val next = if (selected) selectedTagIds - tagId else selectedTagIds + tagId
                    onDraftChange(draft.copy(addTagIds = next.sorted().joinToString(",")))
                }
            )

            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                label = { Text("New session tag") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val name = newTagName.trim()
                    if (name.isNotEmpty()) {
                        onCreateSessionTag(name)
                        newTagName = ""
                    }
                },
                enabled = newTagName.isNotBlank()
            ) {
                Text("Create and select tag")
            }

            OutlinedTextField(
                value = draft.removeTagIds,
                onValueChange = { onDraftChange(draft.copy(removeTagIds = it)) },
                label = { Text("Remove tag IDs (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (preflightMessage != null) {
                Text(preflightMessage)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onApply, enabled = applyEnabled) {
                    Text("Apply to batch")
                }
                Button(onClick = onClearDraft) {
                    Text("Reset bulk draft")
                }
                Button(onClick = onClearBatchStaged, enabled = batchAssetCount > 0) {
                    Text("Clear batch staged")
                }
            }

            Text("Timezone can be assigned to all batch assets.")
        }
    }
}

@Preview
@Composable
private fun BulkEditSectionPreview(
    @PreviewParameter(BulkEditDraftPreviewProvider::class) draft: BulkEditDraft
) {
    MaterialTheme {
        BulkEditSection(
            draft = draft,
            batchAssetCount = 3,
            applyEnabled = true,
            preflightMessage = PREVIEW_PREFLIGHT_MESSAGE,
            availableAlbums = previewCatalogAlbums(),
            availableTags = previewCatalogTags(),
            selectedTagIds = setOf("tag-1", "tag-3"),
            onDraftChange = {},
            onCreateSessionAlbum = {},
            onCreateSessionTag = {},
            onApply = {},
            onClearDraft = {},
            onClearBatchStaged = {}
        )
    }
}
