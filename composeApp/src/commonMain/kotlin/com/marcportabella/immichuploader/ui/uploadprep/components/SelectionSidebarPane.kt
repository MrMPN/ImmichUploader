package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.marcportabella.immichuploader.data.ImmichCatalogEntry
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId

@Composable
fun SelectionSidebarPane(
    selectedAssets: List<LocalAsset>,
    stagedEditsByAssetId: Map<LocalAssetId, AssetEditPatch>,
    bulkDraft: BulkEditDraft,
    selectedCount: Int,
    applyEnabled: Boolean,
    availableAlbums: List<ImmichCatalogEntry>,
    availableTags: List<ImmichCatalogEntry>,
    catalogMessage: String?,
    preflightMessage: String?,
    onSingleAssetPatch: (LocalAssetId, AssetEditPatch) -> Unit,
    onClearSingleSelectionStaged: () -> Unit,
    onBulkDraftChange: (BulkEditDraft) -> Unit,
    onApplyBulk: () -> Unit,
    onClearBulkDraft: () -> Unit,
    onClearSelectedStaged: () -> Unit,
    onClearCatalogMessage: () -> Unit
) {
    val selectedCatalogTagIds = parseCsvIds(bulkDraft.addTagIds)

    when (selectedAssets.size) {
        0 -> {
            SelectionSidebarEmptyStateCard(
                albumsCount = availableAlbums.size,
                tagsCount = availableTags.size,
                catalogMessage = catalogMessage
            )
        }

        1 -> {
            val asset = selectedAssets.first()
            val patch = stagedEditsByAssetId[asset.id]
            SingleSelectionEditorCard(
                asset = asset,
                patch = patch,
                availableAlbums = availableAlbums,
                availableTags = availableTags,
                onPatch = { onSingleAssetPatch(asset.id, it) },
                onClearStaged = onClearSingleSelectionStaged
            )
        }

        else -> {
            BulkEditIntroCard(selectedAssetsCount = selectedAssets.size)

            BulkEditSection(
                draft = bulkDraft,
                selectedCount = selectedCount,
                applyEnabled = applyEnabled,
                preflightMessage = preflightMessage,
                availableAlbums = availableAlbums,
                availableTags = availableTags,
                selectedTagIds = selectedCatalogTagIds,
                onDraftChange = onBulkDraftChange,
                onApply = onApplyBulk,
                onClearDraft = onClearBulkDraft,
                onClearSelectedStaged = onClearSelectedStaged
            )

            catalogMessage?.let { message ->
                CatalogMessageCard(
                    message = message,
                    onDismiss = onClearCatalogMessage
                )
            }
        }
    }
}

private fun parseCsvIds(value: String): Set<String> =
    value
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
