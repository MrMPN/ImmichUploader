package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import com.marcportabella.immichuploader.domain.LocalAsset
import io.github.vinceglb.filekit.coil.addPlatformFileSupport

internal const val ENABLE_QUEUE_PREVIEWS = true

@Composable
internal fun AssetPreviewThumbnail(
    asset: LocalAsset,
    modifier: Modifier = Modifier
) {
    val platformContext = LocalPlatformContext.current
    val imageLoader = remember(platformContext) {
        ImageLoader.Builder(platformContext)
            .components { addPlatformFileSupport() }
            .build()
    }

    if (asset.sourceFile != null && asset.mimeType.startsWith("image/")) {
        AsyncImage(
            model = asset.sourceFile,
            contentDescription = "${asset.fileName} preview",
            imageLoader = imageLoader,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface)
        )
        return
    }

    val fallback = when {
        asset.sourceFile == null -> "No preview"
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

@Preview
@Composable
private fun AssetPreviewThumbnailPreview(
    @PreviewParameter(LocalAssetPreviewProvider::class) asset: LocalAsset
) {
    MaterialTheme {
        AssetPreviewThumbnail(
            asset = asset,
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
