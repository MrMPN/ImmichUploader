package com.marcportabella.immichuploader.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.platform.decodePreviewBitmap
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset

fun LazyListScope.assetQueueSection(
    selectedAssetIds: Set<LocalAssetId>,
    stagedEditsByAssetId: Map<LocalAssetId, AssetEditPatch>,
    sortedAssets: List<LocalAsset>,
    thumbnailCache: MutableMap<LocalAssetId, ImageBitmap?>,
    columns: Int,
    onToggleSelection: (LocalAssetId) -> Unit
) {
    if (sortedAssets.isEmpty()) {
        item(key = "asset-queue-empty") {
            Text(
                text = "No files selected yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val columnCount = columns.coerceAtLeast(1)
    val rows = sortedAssets.chunked(columnCount)
    itemsIndexed(
        items = rows,
        key = { index, row -> "asset-row-${index}-${row.first().id.value}" }
    ) { _, rowAssets ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rowAssets.forEach { asset ->
                val patch = stagedEditsByAssetId[asset.id]
                val metadata = asset.toDisplayMetadata(patch)
                AssetQueueTile(
                    asset = asset,
                    metadata = metadata,
                    isSelected = asset.id in selectedAssetIds,
                    thumbnailCache = thumbnailCache,
                    onToggleSelection = onToggleSelection,
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(columnCount - rowAssets.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview
@Composable
private fun AssetQueueTilePreview(
    @PreviewParameter(LocalAssetPreviewProvider::class) asset: LocalAsset
) {
    MaterialTheme {
        AssetQueueTile(
            asset = asset,
            metadata = asset.toDisplayMetadata(previewSinglePatch()),
            isSelected = true,
            thumbnailCache = mutableMapOf(),
            onToggleSelection = {}
        )
    }
}

@Preview
@Composable
private fun AssetPreviewThumbnailPreview(
    @PreviewParameter(LocalAssetPreviewProvider::class) asset: LocalAsset
) {
    MaterialTheme {
        AssetPreviewThumbnail(
            asset = asset,
            thumbnailCache = mutableMapOf(),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
        )
    }
}

@Preview
@Composable
private fun PreviewDisabledPlaceholderPreview() {
    MaterialTheme {
        PreviewDisabledPlaceholder(
            mimeType = "image/jpeg",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
        )
    }
}

private const val ENABLE_QUEUE_PREVIEWS = true

@Composable
private fun AssetQueueTile(
    asset: LocalAsset,
    metadata: DisplayMetadata,
    isSelected: Boolean,
    thumbnailCache: MutableMap<LocalAssetId, ImageBitmap?>,
    onToggleSelection: (LocalAssetId) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onToggleSelection(asset.id) },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = asset.fileName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection(asset.id) }
                )
            }

            if (ENABLE_QUEUE_PREVIEWS) {
                AssetPreviewThumbnail(
                    asset = asset,
                    thumbnailCache = thumbnailCache,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f)
                )
            } else {
                PreviewDisabledPlaceholder(
                    mimeType = asset.mimeType,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.4f)
                )
            }

            Text(
                text = metadata.captureDisplay ?: "Capture date not available",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AssetPreviewThumbnail(
    asset: LocalAsset,
    thumbnailCache: MutableMap<LocalAssetId, ImageBitmap?>,
    modifier: Modifier = Modifier
) {
    val imageBitmap = thumbnailCache.getOrPut(asset.id) {
        val bytes = asset.previewBytes ?: return@getOrPut null
        decodePreviewBitmap(bytes)
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "${asset.fileName} preview",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface)
        )
        return
    }

    val fallback = when {
        asset.previewUrl == null -> "No preview"
        asset.mimeType.startsWith("video/") -> "Video preview n/a"
        else -> "Preview unavailable"
    }

    Text(
        text = fallback,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    )
}

@Composable
private fun PreviewDisabledPlaceholder(
    mimeType: String,
    modifier: Modifier = Modifier
) {
    val label = when {
        mimeType.startsWith("image/") -> "Image preview disabled"
        mimeType.startsWith("video/") -> "Video preview disabled"
        else -> "Preview disabled"
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp)
    )
}

data class DisplayMetadata(
    val dateTimeOriginal: String?,
    val timeZone: String?,
    val captureDisplay: String?,
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
    val timeZone = (patch?.timeZone as? FieldPatch.Set<String>)?.value ?: timeZone
    val albumId = (patch?.albumId as? FieldPatch.Set<String?>)?.value ?: albumId

    val addTags = patch?.addTagIds ?: emptySet()
    val removeTags = patch?.removeTagIds ?: emptySet()

    return DisplayMetadata(
        dateTimeOriginal = dateTimeOriginal,
        timeZone = timeZone,
        captureDisplay = formatCaptureDateTime(dateTimeOriginal, timeZone),
        cameraLabel = listOfNotNull(cameraMake, cameraModel).joinToString(" ").ifBlank { null },
        description = description,
        isFavorite = isFavorite,
        albumId = albumId,
        tagIds = (tagIds + addTags) - removeTags,
        exifSummary = exifSummary
    )
}

fun formatCaptureDateTime(
    dateTimeOriginal: String?,
    timeZone: String?
): String? {
    val dateTime = dateTimeOriginal?.trim().orEmpty()
    if (dateTime.isBlank()) return null

    val normalizedDateTime = runCatching { LocalDateTime.parse(dateTime).toString() }.getOrElse { dateTime }
    val normalizedOffset = timeZone
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { UtcOffset.parse(it).toString() }.getOrElse { it } }

    return if (normalizedOffset == null) normalizedDateTime else "$normalizedDateTime $normalizedOffset"
}
