package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import immichuploader.composeapp.generated.resources.Res
import immichuploader.composeapp.generated.resources.asset_queue_empty
import immichuploader.composeapp.generated.resources.asset_queue_empty_ca

fun LazyListScope.assetQueueSection(
    duplicateAssetIds: Set<LocalAssetId>,
    stagedEditsByAssetId: Map<LocalAssetId, AssetEditPatch>,
    rows: List<List<LocalAsset>>,
    columnCount: Int
) {
    if (rows.isEmpty()) {
        item(key = "asset-queue-empty") {
            Text(
                text = i18nString(
                    english = Res.string.asset_queue_empty,
                    catalan = Res.string.asset_queue_empty_ca
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

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
                    isDuplicate = asset.id in duplicateAssetIds,
                    modifier = Modifier.weight(1f)
                )
            }
            repeat(columnCount - rowAssets.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
