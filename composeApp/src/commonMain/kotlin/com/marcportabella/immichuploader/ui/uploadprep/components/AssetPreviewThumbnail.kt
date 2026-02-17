package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.platform.decodePreviewBitmap

internal const val ENABLE_QUEUE_PREVIEWS = true

@Composable
internal fun AssetPreviewThumbnail(
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
internal fun PreviewDisabledPlaceholder(
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
