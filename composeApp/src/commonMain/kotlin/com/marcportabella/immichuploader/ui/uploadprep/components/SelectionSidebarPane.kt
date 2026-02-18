package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadCatalogEntry

@Composable
fun SelectionSidebarPane(
    selectedAssets: List<LocalAsset>,
    stagedEditsByAssetId: Map<LocalAssetId, AssetEditPatch>,
    bulkDraft: BulkEditDraft,
    selectedCount: Int,
    applyEnabled: Boolean,
    availableAlbums: List<UploadCatalogEntry>,
    availableTags: List<UploadCatalogEntry>,
    catalogMessage: String?,
    preflightMessage: String?,
    onSingleAssetPatch: (LocalAssetId, AssetEditPatch) -> Unit,
    onSingleAssetTagSelectionReplace: (LocalAssetId, Set<String>, Set<String>) -> Unit,
    onClearSingleSelectionStaged: () -> Unit,
    onBulkDraftChange: (BulkEditDraft) -> Unit,
    onCreateSessionAlbumForBulk: (String) -> Unit,
    onCreateSessionTagForBulk: (String) -> Unit,
    onCreateSessionAlbumForAsset: (LocalAssetId, String) -> Unit,
    onCreateSessionTagForAsset: (LocalAssetId, String) -> Unit,
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
                onTagSelectionReplace = { addTagIds, removeTagIds ->
                    onSingleAssetTagSelectionReplace(asset.id, addTagIds, removeTagIds)
                },
                onCreateSessionAlbum = { onCreateSessionAlbumForAsset(asset.id, it) },
                onCreateSessionTag = { onCreateSessionTagForAsset(asset.id, it) },
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
                onCreateSessionAlbum = onCreateSessionAlbumForBulk,
                onCreateSessionTag = onCreateSessionTagForBulk,
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

@Preview
@Composable
private fun SelectionSidebarNoSelectionPreview(
    @PreviewParameter(SidebarPreviewProvider::class) model: SidebarPreviewModel
) {
    if (model.selectedAssets.isNotEmpty()) return
    MaterialTheme {
        SelectionSidebarPane(
            selectedAssets = model.selectedAssets,
            stagedEditsByAssetId = model.stagedEditsByAssetId,
            bulkDraft = model.bulkDraft,
            selectedCount = model.selectedCount,
            applyEnabled = model.applyEnabled,
            availableAlbums = model.availableAlbums,
            availableTags = model.availableTags,
            catalogMessage = model.catalogMessage,
            preflightMessage = model.preflightMessage,
            onSingleAssetPatch = { _, _ -> },
            onSingleAssetTagSelectionReplace = { _, _, _ -> },
            onClearSingleSelectionStaged = {},
            onBulkDraftChange = {},
            onCreateSessionAlbumForBulk = {},
            onCreateSessionTagForBulk = {},
            onCreateSessionAlbumForAsset = { _, _ -> },
            onCreateSessionTagForAsset = { _, _ -> },
            onApplyBulk = {},
            onClearBulkDraft = {},
            onClearSelectedStaged = {},
            onClearCatalogMessage = {}
        )
    }
}

@Preview
@Composable
private fun SelectionSidebarSinglePreview(
    @PreviewParameter(SidebarPreviewProvider::class) model: SidebarPreviewModel
) {
    if (model.selectedAssets.size != 1) return
    MaterialTheme {
        SelectionSidebarPane(
            selectedAssets = model.selectedAssets,
            stagedEditsByAssetId = model.stagedEditsByAssetId,
            bulkDraft = model.bulkDraft,
            selectedCount = model.selectedCount,
            applyEnabled = model.applyEnabled,
            availableAlbums = model.availableAlbums,
            availableTags = model.availableTags,
            catalogMessage = model.catalogMessage,
            preflightMessage = model.preflightMessage,
            onSingleAssetPatch = { _, _ -> },
            onSingleAssetTagSelectionReplace = { _, _, _ -> },
            onClearSingleSelectionStaged = {},
            onBulkDraftChange = {},
            onCreateSessionAlbumForBulk = {},
            onCreateSessionTagForBulk = {},
            onCreateSessionAlbumForAsset = { _, _ -> },
            onCreateSessionTagForAsset = { _, _ -> },
            onApplyBulk = {},
            onClearBulkDraft = {},
            onClearSelectedStaged = {},
            onClearCatalogMessage = {}
        )
    }
}

@Preview
@Composable
private fun SelectionSidebarBulkPreview(
    @PreviewParameter(SidebarPreviewProvider::class) model: SidebarPreviewModel
) {
    if (model.selectedAssets.size < 2) return
    MaterialTheme {
        SelectionSidebarPane(
            selectedAssets = model.selectedAssets,
            stagedEditsByAssetId = model.stagedEditsByAssetId,
            bulkDraft = model.bulkDraft,
            selectedCount = model.selectedCount,
            applyEnabled = model.applyEnabled,
            availableAlbums = model.availableAlbums,
            availableTags = model.availableTags,
            catalogMessage = model.catalogMessage,
            preflightMessage = model.preflightMessage,
            onSingleAssetPatch = { _, _ -> },
            onSingleAssetTagSelectionReplace = { _, _, _ -> },
            onClearSingleSelectionStaged = {},
            onBulkDraftChange = {},
            onCreateSessionAlbumForBulk = {},
            onCreateSessionTagForBulk = {},
            onCreateSessionAlbumForAsset = { _, _ -> },
            onCreateSessionTagForAsset = { _, _ -> },
            onApplyBulk = {},
            onClearBulkDraft = {},
            onClearSelectedStaged = {},
            onClearCatalogMessage = {}
        )
    }
}
