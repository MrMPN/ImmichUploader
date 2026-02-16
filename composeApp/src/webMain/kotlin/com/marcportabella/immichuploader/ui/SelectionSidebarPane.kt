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
    onClearSelectedStaged: () -> Unit
) {
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
                }
            }
        }

        1 -> {
            val asset = selectedAssets.first()
            val patch = state.stagedEditsByAssetId[asset.id]
            SingleSelectionEditorCard(
                asset = asset,
                patch = patch,
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
                onDraftChange = onBulkDraftChange,
                onApply = onApplyBulk,
                onClearDraft = onClearBulkDraft,
                onClearSelectedStaged = onClearSelectedStaged
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SingleSelectionEditorCard(
    asset: LocalAsset,
    patch: AssetEditPatch?,
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
