package com.marcportabella.immichuploader.ui

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadPrepState
import com.marcportabella.immichuploader.domain.canApplyBulkEdit

@Composable
fun SelectionSidebarPane(
    state: UploadPrepState,
    selectedAssets: List<LocalAsset>,
    preflightMessage: String?,
    onSingleAssetPatch: (LocalAssetId, AssetEditPatch) -> Unit,
    onClearSingleSelectionStaged: () -> Unit,
    onBulkDraftChange: (BulkEditDraft) -> Unit,
    onApplyBulk: () -> Unit,
    onClearBulkDraft: () -> Unit,
    onClearSelectedStaged: () -> Unit,
    onClearCatalogMessage: () -> Unit
) {
    val selectedCatalogTagIds = parseCsvIds(state.bulkEditDraft.addTagIds)

    when (selectedAssets.size) {
        0 -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Details sidebar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("No files selected.")
                    Text("Select one asset to inspect and edit metadata, or select multiple for bulk editing.")
                    Text("Albums loaded: ${state.availableAlbums.size} | Tags loaded: ${state.availableTags.size}")
                    state.catalogMessage?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        1 -> {
            val asset = selectedAssets.first()
            val patch = state.stagedEditsByAssetId[asset.id]
            SingleSelectionEditorCard(
                asset = asset,
                patch = patch,
                availableAlbums = state.availableAlbums,
                availableTags = state.availableTags,
                onPatch = { onSingleAssetPatch(asset.id, it) },
                onClearStaged = onClearSingleSelectionStaged
            )
        }

        else -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Bulk edit mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("${selectedAssets.size} assets selected. Changes from this panel apply to the whole selection.")
                }
            }

            BulkEditSection(
                draft = state.bulkEditDraft,
                selectedCount = state.selectedAssetIds.size,
                applyEnabled = canApplyBulkEdit(state),
                preflightMessage = preflightMessage,
                availableAlbums = state.availableAlbums,
                availableTags = state.availableTags,
                selectedTagIds = selectedCatalogTagIds,
                onDraftChange = onBulkDraftChange,
                onApply = onApplyBulk,
                onClearDraft = onClearBulkDraft,
                onClearSelectedStaged = onClearSelectedStaged
            )

            state.catalogMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(message, style = MaterialTheme.typography.bodySmall)
                        Button(onClick = onClearCatalogMessage) {
                            Text("Dismiss message")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleSelectionEditorCard(
    asset: LocalAsset,
    patch: AssetEditPatch?,
    availableAlbums: List<com.marcportabella.immichuploader.data.ImmichCatalogEntry>,
    availableTags: List<com.marcportabella.immichuploader.data.ImmichCatalogEntry>,
    onPatch: (AssetEditPatch) -> Unit,
    onClearStaged: () -> Unit
) {
    val metadata = asset.toDisplayMetadata(patch)
    val favorite = metadata.isFavorite ?: false

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
                "Asset details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(asset.fileName, fontWeight = FontWeight.Medium)
            Text("${asset.mimeType} - ${asset.fileSizeBytes} bytes")
            metadata.captureDisplay?.let { Text("Capture: $it") }
            Text("Camera: ${metadata.cameraLabel ?: "Unknown"}")
            if (metadata.exifSummary != null) {
                Text("EXIF: ${metadata.exifSummary}")
            }

            HorizontalDivider()
            Text("Edit metadata to stage single-asset changes.")

            OutlinedTextField(
                value = metadata.description.orEmpty(),
                onValueChange = {
                    onPatch(
                        AssetEditPatch(description = FieldPatch.Set(it.ifBlank { null }))
                    )
                },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = metadata.dateTimeOriginal.orEmpty(),
                onValueChange = {
                    onPatch(
                        AssetEditPatch(dateTimeOriginal = FieldPatch.Set(it))
                    )
                },
                label = { Text("Date/time original (ISO 8601)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = metadata.timeZone.orEmpty(),
                onValueChange = {
                    onPatch(
                        AssetEditPatch(timeZone = FieldPatch.Set(it))
                    )
                },
                label = { Text("Timezone (e.g. +02:00 or Z)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = metadata.albumId.orEmpty(),
                onValueChange = {
                    onPatch(
                        AssetEditPatch(albumId = FieldPatch.Set(it.ifBlank { null }))
                    )
                },
                label = { Text("Album ID") },
                modifier = Modifier.fillMaxWidth()
            )

            if (availableAlbums.isNotEmpty()) {
                Text("Choose album", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableAlbums.forEach { album ->
                        FilterChip(
                            selected = metadata.albumId == album.id,
                            onClick = {
                                val nextAlbumId = if (metadata.albumId == album.id) null else album.id
                                onPatch(AssetEditPatch(albumId = FieldPatch.Set(nextAlbumId)))
                            },
                            label = { Text(album.name) }
                        )
                    }
                }
            }

            if (availableTags.isNotEmpty()) {
                Text("Select tags for upload", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableTags.forEach { tag ->
                        val selected = tag.id in metadata.tagIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val desired = if (selected) metadata.tagIds - tag.id else metadata.tagIds + tag.id
                                val addTagIds = desired - asset.tagIds
                                val removeTagIds = asset.tagIds - desired
                                onPatch(AssetEditPatch(addTagIds = addTagIds, removeTagIds = removeTagIds))
                            },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onPatch(
                            AssetEditPatch(isFavorite = FieldPatch.Set(!favorite))
                        )
                    }
                ) {
                    Text("Favorite = ${!favorite}")
                }
                Button(onClick = onClearStaged) {
                    Text("Clear staged for selected")
                }
            }
        }
    }
}

private fun parseCsvIds(value: String): Set<String> =
    value
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
