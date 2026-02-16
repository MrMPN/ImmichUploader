package com.marcportabella.immichuploader.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadPrepState
import org.jetbrains.skia.Image

fun LazyListScope.assetQueueSection(
    state: UploadPrepState,
    sortedAssets: List<LocalAsset>,
    thumbnailCache: MutableMap<LocalAssetId, ImageBitmap?>,
    onToggleSelection: (LocalAssetId) -> Unit
) {
    item(key = "asset-queue-header") {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Asset queue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (sortedAssets.isEmpty()) {
                    Text("No files selected yet.")
                } else {
                    Text("Sorted by filename for predictable review.")
                }
            }
        }
    }

    if (sortedAssets.isNotEmpty()) {
        items(
            items = sortedAssets,
            key = { it.id.value }
        ) { asset ->
            val patch = state.stagedEditsByAssetId[asset.id]
            val metadata = asset.toDisplayMetadata(patch)
            AssetQueueRow(
                asset = asset,
                metadata = metadata,
                isSelected = asset.id in state.selectedAssetIds,
                thumbnailCache = thumbnailCache,
                onToggleSelection = onToggleSelection
            )
        }
    }
}

private const val ENABLE_QUEUE_PREVIEWS = false

@Composable
private fun AssetQueueRow(
    asset: LocalAsset,
    metadata: DisplayMetadata,
    isSelected: Boolean,
    thumbnailCache: MutableMap<LocalAssetId, ImageBitmap?>,
    onToggleSelection: (LocalAssetId) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection(asset.id) }
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (ENABLE_QUEUE_PREVIEWS) {
                        AssetPreviewThumbnail(
                            asset = asset,
                            thumbnailCache = thumbnailCache
                        )
                    } else {
                        PreviewDisabledPlaceholder(asset.mimeType)
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(asset.fileName, fontWeight = FontWeight.Medium)
                        Text("${asset.mimeType} · ${asset.fileSizeBytes} bytes")
                        Text("Date/time: ${metadata.dateTimeOriginal ?: "Unknown"}")
                        Text("Timezone: ${metadata.timeZone ?: "Unknown"} (read-only)")
                        Text("Camera: ${metadata.cameraLabel ?: "Unknown"}")
                        Text("Description: ${metadata.description ?: "None"}")
                        Text("Favorite: ${metadata.isFavorite?.toString() ?: "None"}")
                        Text("Album: ${metadata.albumId ?: "None"}")
                        Text("Tags: ${if (metadata.tagIds.isEmpty()) "None" else metadata.tagIds.joinToString(", ")}")
                        if (metadata.exifSummary != null) {
                            Text("EXIF: ${metadata.exifSummary}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetPreviewThumbnail(
    asset: LocalAsset,
    thumbnailCache: MutableMap<LocalAssetId, ImageBitmap?>
) {
    val imageBitmap = thumbnailCache.getOrPut(asset.id) {
        val bytes = asset.previewBytes ?: return@getOrPut null
        runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "${asset.fileName} preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(92.dp)
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface)
        )
        return
    }

    val fallback = when {
        asset.previewUrl == null -> "No preview"
        asset.mimeType.startsWith("video/") -> "Video\npreview\nn/a"
        else -> "Preview\nunavailable"
    }

    Text(
        text = fallback,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .size(92.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    )
}

@Composable
private fun PreviewDisabledPlaceholder(mimeType: String) {
    val label = when {
        mimeType.startsWith("image/") -> "Image preview disabled"
        mimeType.startsWith("video/") -> "Video preview disabled"
        else -> "Preview disabled"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .size(92.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    )
}

data class DisplayMetadata(
    val dateTimeOriginal: String?,
    val timeZone: String?,
    val cameraLabel: String?,
    val description: String?,
    val isFavorite: Boolean?,
    val albumId: String?,
    val tagIds: Set<String>,
    val exifSummary: String?
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
        cameraLabel = listOfNotNull(cameraMake, cameraModel).joinToString(" ").ifBlank { null },
        description = description,
        isFavorite = isFavorite,
        albumId = albumId,
        tagIds = (tagIds + addTags) - removeTags,
        exifSummary = exifSummary
    )
}
