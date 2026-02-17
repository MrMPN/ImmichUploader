package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.marcportabella.immichuploader.domain.LocalAsset

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
