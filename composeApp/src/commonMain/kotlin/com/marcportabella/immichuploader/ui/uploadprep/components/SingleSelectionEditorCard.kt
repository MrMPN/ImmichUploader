package com.marcportabella.immichuploader.ui.uploadprep

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.UploadCatalogEntry

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SingleSelectionEditorCard(
    asset: LocalAsset,
    patch: AssetEditPatch?,
    availableAlbums: List<UploadCatalogEntry>,
    availableTags: List<UploadCatalogEntry>,
    onPatch: (AssetEditPatch) -> Unit,
    onClearStaged: () -> Unit
) {
    val metadata = asset.toDisplayMetadata(patch)
    val favorite = metadata.isFavorite ?: false

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Asset details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(asset.fileName, fontWeight = FontWeight.Medium)
            Text("${asset.mimeType} - ${asset.fileSizeBytes} bytes")
            metadata.captureDisplay?.let { Text("Capture: $it") }
            Text("Camera: ${metadata.cameraLabel ?: "Unknown"}")
            if (metadata.exifSummary != null) Text("EXIF: ${metadata.exifSummary}")

            HorizontalDivider()
            Text("Edit metadata to stage single-asset changes.")

            OutlinedTextField(
                value = metadata.description.orEmpty(),
                onValueChange = { onPatch(AssetEditPatch(description = FieldPatch.Set(it.ifBlank { null }))) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = metadata.dateTimeOriginal.orEmpty(),
                onValueChange = { onPatch(AssetEditPatch(dateTimeOriginal = FieldPatch.Set(it))) },
                label = { Text("Date/time original (ISO 8601)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = metadata.timeZone.orEmpty(),
                onValueChange = { onPatch(AssetEditPatch(timeZone = FieldPatch.Set(it))) },
                label = { Text("Timezone (e.g. +02:00 or Z)") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = metadata.albumId.orEmpty(),
                onValueChange = { onPatch(AssetEditPatch(albumId = FieldPatch.Set(it.ifBlank { null }))) },
                label = { Text("Album ID") },
                modifier = Modifier.fillMaxWidth()
            )

            SelectableChipGroup(
                title = "Choose album",
                options = availableAlbums.map { ChipOption(id = it.id, label = it.name) },
                selectedIds = metadata.albumId?.let(::setOf) ?: emptySet(),
                onToggle = { albumId ->
                    val nextAlbumId = if (metadata.albumId == albumId) null else albumId
                    onPatch(AssetEditPatch(albumId = FieldPatch.Set(nextAlbumId)))
                }
            )

            SelectableChipGroup(
                title = "Select tags for upload",
                options = availableTags.map { ChipOption(id = it.id, label = it.name) },
                selectedIds = metadata.tagIds,
                onToggle = { tagId ->
                    val selected = tagId in metadata.tagIds
                    val desired = if (selected) metadata.tagIds - tagId else metadata.tagIds + tagId
                    val addTagIds = desired - asset.tagIds
                    val removeTagIds = asset.tagIds - desired
                    onPatch(AssetEditPatch(addTagIds = addTagIds, removeTagIds = removeTagIds))
                }
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { onPatch(AssetEditPatch(isFavorite = FieldPatch.Set(!favorite))) }) {
                    Text("Favorite = ${!favorite}")
                }
                Button(onClick = onClearStaged) { Text("Clear staged for selected") }
            }
        }
    }
}

@Preview
@Composable
private fun SingleSelectionEditorCardPreview() {
    val asset = previewAsset(id = "a1", name = "2016-11-08_02-43-27.jpg")
    MaterialTheme {
        SingleSelectionEditorCard(
            asset = asset,
            patch = previewSinglePatch(),
            availableAlbums = previewCatalogAlbums(),
            availableTags = previewCatalogTags(),
            onPatch = {},
            onClearStaged = {}
        )
    }
}
