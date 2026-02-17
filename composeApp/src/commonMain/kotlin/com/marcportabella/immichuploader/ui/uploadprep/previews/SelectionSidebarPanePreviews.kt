package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter

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
            onClearSingleSelectionStaged = {},
            onBulkDraftChange = {},
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
            onClearSingleSelectionStaged = {},
            onBulkDraftChange = {},
            onApplyBulk = {},
            onClearBulkDraft = {},
            onClearSelectedStaged = {},
            onClearCatalogMessage = {}
        )
    }
}

@Preview
@Composable
private fun SingleSelectionEditorCardPreview() {
    val asset = previewAsset(id = "a1", name = "2016-11-08_02-43-27.jpg")
    MaterialTheme {
        SingleSelectionEditorCard(
            asset = asset,
            patch = previewSinglePatch(),
            availableAlbums = previewCatalogAlbums(),
            availableTags = previewCatalogTags(),
            onPatch = {},
            onClearStaged = {}
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
            onClearSingleSelectionStaged = {},
            onBulkDraftChange = {},
            onApplyBulk = {},
            onClearBulkDraft = {},
            onClearSelectedStaged = {},
            onClearCatalogMessage = {}
        )
    }
}
