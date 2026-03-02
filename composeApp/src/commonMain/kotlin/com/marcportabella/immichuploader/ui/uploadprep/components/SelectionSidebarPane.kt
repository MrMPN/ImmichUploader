package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.UploadCatalogEntry

@Composable
fun SelectionSidebarPane(
    hasAssets: Boolean,
    batchAssetCount: Int,
    bulkDraft: BulkEditDraft,
    applyEnabled: Boolean,
    availableAlbums: List<UploadCatalogEntry>,
    availableTags: List<UploadCatalogEntry>,
    catalogMessage: String?,
    preflightMessage: String?,
    onBulkDraftChange: (BulkEditDraft) -> Unit,
    onCreateSessionAlbumForBulk: (String) -> Unit,
    onCreateSessionTagForBulk: (String) -> Unit,
    onApplyBulk: () -> Unit,
    onClearBulkDraft: () -> Unit,
    onClearBatchStaged: () -> Unit,
    onClearCatalogMessage: () -> Unit
) {
    if (!hasAssets) {
        SelectionSidebarEmptyStateCard(
            albumsCount = availableAlbums.size,
            tagsCount = availableTags.size,
            catalogMessage = catalogMessage
        )
        return
    }

    val selectedCatalogTagIds = parseCsvIds(bulkDraft.addTagIds)

    BulkEditIntroCard(batchAssetCount = batchAssetCount)
    BulkEditSection(
        draft = bulkDraft,
        batchAssetCount = batchAssetCount,
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
        onClearBatchStaged = onClearBatchStaged
    )

    catalogMessage?.let { message ->
        CatalogMessageCard(
            message = message,
            onDismiss = onClearCatalogMessage
        )
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
private fun SelectionSidebarNoBatchPreview(
    @PreviewParameter(SidebarPreviewProvider::class) model: SidebarPreviewModel
) {
    MaterialTheme {
        SelectionSidebarPane(
            hasAssets = false,
            batchAssetCount = 0,
            bulkDraft = model.bulkDraft,
            applyEnabled = model.applyEnabled,
            availableAlbums = model.availableAlbums,
            availableTags = model.availableTags,
            catalogMessage = model.catalogMessage,
            preflightMessage = model.preflightMessage,
            onBulkDraftChange = {},
            onCreateSessionAlbumForBulk = {},
            onCreateSessionTagForBulk = {},
            onApplyBulk = {},
            onClearBulkDraft = {},
            onClearBatchStaged = {},
            onClearCatalogMessage = {}
        )
    }
}

@Preview
@Composable
private fun SelectionSidebarBatchPreview(
    @PreviewParameter(SidebarPreviewProvider::class) model: SidebarPreviewModel
) {
    MaterialTheme {
        SelectionSidebarPane(
            hasAssets = true,
            batchAssetCount = 5,
            bulkDraft = model.bulkDraft,
            applyEnabled = model.applyEnabled,
            availableAlbums = model.availableAlbums,
            availableTags = model.availableTags,
            catalogMessage = model.catalogMessage,
            preflightMessage = model.preflightMessage,
            onBulkDraftChange = {},
            onCreateSessionAlbumForBulk = {},
            onCreateSessionTagForBulk = {},
            onApplyBulk = {},
            onClearBulkDraft = {},
            onClearBatchStaged = {},
            onClearCatalogMessage = {}
        )
    }
}
