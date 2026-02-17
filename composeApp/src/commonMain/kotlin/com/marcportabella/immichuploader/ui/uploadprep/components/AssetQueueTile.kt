package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId

@Composable
internal fun AssetQueueTile(
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
