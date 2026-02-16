package com.marcportabella.immichuploader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadPrepState

@Composable
fun AssetQueueSection(
    state: UploadPrepState,
    onToggleSelection: (LocalAssetId) -> Unit
) {
    if (state.assets.isEmpty()) {
        Text("No files selected yet.")
    } else {
        Text("Queue")
        state.assets.values.sortedBy { it.fileName }.forEach { asset ->
            val patch = state.stagedEditsByAssetId[asset.id]
            val metadata = asset.toDisplayMetadata(patch)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = asset.id in state.selectedAssetIds,
                    onCheckedChange = { onToggleSelection(asset.id) }
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(asset.fileName)
                    Text("${asset.mimeType} · ${asset.fileSizeBytes} bytes")
                    Text("Date/time: ${metadata.dateTimeOriginal ?: "Unknown"}")
                    Text("Timezone: ${metadata.timeZone ?: "Unknown"} (read-only)")
                    Text("Description: ${metadata.description ?: "None"}")
                    Text("Favorite: ${metadata.isFavorite?.toString() ?: "None"}")
                    Text("Album: ${metadata.albumId ?: "None"}")
                    Text("Tags: ${if (metadata.tagIds.isEmpty()) "None" else metadata.tagIds.joinToString(", ")}")
                    Text(if (asset.previewUrl == null) "No preview" else "Preview ready")
                }
            }
        }
    }
}

data class DisplayMetadata(
    val dateTimeOriginal: String?,
    val timeZone: String?,
    val description: String?,
    val isFavorite: Boolean?,
    val albumId: String?,
    val tagIds: Set<String>
)

fun LocalAsset.toDisplayMetadata(patch: AssetEditPatch?): DisplayMetadata {
    val description = (patch?.description as? FieldPatch.Set<String?>)?.value ?: description
    val isFavorite = (patch?.isFavorite as? FieldPatch.Set<Boolean>)?.value ?: isFavorite
    val dateTimeOriginal = (patch?.dateTimeOriginal as? FieldPatch.Set<String>)?.value ?: captureDateTime
    val albumId = (patch?.albumId as? FieldPatch.Set<String?>)?.value ?: albumId

    val addTags = patch?.addTagIds ?: emptySet()
    val removeTags = patch?.removeTagIds ?: emptySet()

    return DisplayMetadata(
        dateTimeOriginal = dateTimeOriginal,
        timeZone = timeZone,
        description = description,
        isFavorite = isFavorite,
        albumId = albumId,
        tagIds = (tagIds + addTags) - removeTags
    )
}
