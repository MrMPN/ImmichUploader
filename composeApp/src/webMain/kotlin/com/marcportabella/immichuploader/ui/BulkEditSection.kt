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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.data.ImmichCatalogEntry
import com.marcportabella.immichuploader.domain.BulkEditDraft

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulkEditSection(
    draft: BulkEditDraft,
    selectedCount: Int,
    applyEnabled: Boolean,
    preflightMessage: String?,
    availableAlbums: List<ImmichCatalogEntry>,
    availableTags: List<ImmichCatalogEntry>,
    selectedTagIds: Set<String>,
    onDraftChange: (BulkEditDraft) -> Unit,
    onApply: () -> Unit,
    onClearDraft: () -> Unit,
    onClearSelectedStaged: () -> Unit
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
                "Bulk edit",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Selected subset: $selectedCount",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "Toggle each field to include it in the patch before applying to selected assets.",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = draft.includeTimeZone,
                    onCheckedChange = { onDraftChange(draft.copy(includeTimeZone = it)) }
                )
                OutlinedTextField(
                    value = draft.timeZone,
                    onValueChange = { onDraftChange(draft.copy(timeZone = it)) },
                    label = { Text("Timezone (e.g. +02:00 or Z)") },
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
                    Text("Apply to selected")
                }
                Button(onClick = onClearDraft) {
                    Text("Reset bulk draft")
                }
                Button(onClick = onClearSelectedStaged, enabled = selectedCount > 0) {
                    Text("Clear selected staged")
                }
            }

            Text("Timezone can be bulk-assigned for selected assets.")
        }
    }
}

@Preview
@Composable
private fun BulkEditSectionPreview() {
    MaterialTheme {
        BulkEditSection(
            draft = previewBulkDraft(),
            selectedCount = 3,
            applyEnabled = true,
            preflightMessage = PREVIEW_PREFLIGHT_MESSAGE,
            availableAlbums = previewCatalogAlbums(),
            availableTags = previewCatalogTags(),
            selectedTagIds = setOf("tag-1", "tag-3"),
            onDraftChange = {},
            onApply = {},
            onClearDraft = {},
            onClearSelectedStaged = {}
        )
    }
}
