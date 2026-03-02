package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.LocalAsset
import immichuploader.composeapp.generated.resources.Res
import immichuploader.composeapp.generated.resources.asset_tile_already_on_server
import immichuploader.composeapp.generated.resources.asset_tile_already_on_server_ca
import immichuploader.composeapp.generated.resources.asset_tile_capture_missing
import immichuploader.composeapp.generated.resources.asset_tile_capture_missing_ca
import immichuploader.composeapp.generated.resources.asset_tile_mime_size
import immichuploader.composeapp.generated.resources.asset_tile_mime_size_ca

@Composable
internal fun AssetQueueTile(
    asset: LocalAsset,
    metadata: DisplayMetadata,
    isDuplicate: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .alpha(if (isDuplicate) 0.55f else 1f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = asset.fileName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = i18nString(
                    english = Res.string.asset_tile_mime_size,
                    catalan = Res.string.asset_tile_mime_size_ca,
                    asset.mimeType,
                    asset.fileSizeBytes
                ),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = metadata.captureDisplay
                    ?: i18nString(
                        english = Res.string.asset_tile_capture_missing,
                        catalan = Res.string.asset_tile_capture_missing_ca
                    ),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isDuplicate) {
                Text(
                    text = i18nString(
                        english = Res.string.asset_tile_already_on_server,
                        catalan = Res.string.asset_tile_already_on_server_ca
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            isDuplicate = false,
        )
    }
}
