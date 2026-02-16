package com.marcportabella.immichuploader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class BulkEditDraft(
    val includeDescription: Boolean = false,
    val description: String = "",
    val includeFavorite: Boolean = false,
    val isFavorite: Boolean = false,
    val includeDateTimeOriginal: Boolean = false,
    val dateTimeOriginal: String = "",
    val includeAlbumId: Boolean = false,
    val albumId: String = "",
    val addTagIds: String = "",
    val removeTagIds: String = ""
)

data class UploadPrepState(
    val assets: Map<LocalAssetId, LocalAsset> = emptyMap(),
    val selectedAssetIds: Set<LocalAssetId> = emptySet(),
    val stagedEditsByAssetId: Map<LocalAssetId, AssetEditPatch> = emptyMap(),
    val bulkEditDraft: BulkEditDraft = BulkEditDraft()
)

sealed interface UploadPrepAction {
    data class ReplaceAssets(val assets: List<LocalAsset>) : UploadPrepAction
    data class ToggleSelection(val assetId: LocalAssetId) : UploadPrepAction
    data class SetSelection(val assetIds: Set<LocalAssetId>) : UploadPrepAction
    data object SelectAll : UploadPrepAction
    data object ClearSelection : UploadPrepAction
    data class StageEditForSelected(val patch: AssetEditPatch) : UploadPrepAction
    data class StageEditForAsset(val assetId: LocalAssetId, val patch: AssetEditPatch) : UploadPrepAction
    data object ClearStagedForSelected : UploadPrepAction
    data class SetBulkEditDraft(val draft: BulkEditDraft) : UploadPrepAction
    data object ApplyBulkEditDraftToSelected : UploadPrepAction
    data object ClearBulkEditDraft : UploadPrepAction
}

fun reduceUploadPrepState(state: UploadPrepState, action: UploadPrepAction): UploadPrepState =
    when (action) {
        is UploadPrepAction.ReplaceAssets -> {
            val nextAssets = action.assets.associateBy { it.id }
            val validIds = nextAssets.keys
            state.copy(
                assets = nextAssets,
                selectedAssetIds = state.selectedAssetIds.intersect(validIds),
                stagedEditsByAssetId = state.stagedEditsByAssetId.filterKeys { it in validIds }
            )
        }

        is UploadPrepAction.ToggleSelection -> {
            if (action.assetId !in state.assets) return state
            val nextSelection = state.selectedAssetIds.toMutableSet()
            if (!nextSelection.add(action.assetId)) {
                nextSelection.remove(action.assetId)
            }
            state.copy(selectedAssetIds = nextSelection)
        }

        is UploadPrepAction.SetSelection -> state.copy(
            selectedAssetIds = action.assetIds.intersect(state.assets.keys)
        )

        UploadPrepAction.SelectAll -> state.copy(selectedAssetIds = state.assets.keys)

        UploadPrepAction.ClearSelection -> state.copy(selectedAssetIds = emptySet())

        is UploadPrepAction.StageEditForSelected ->
            stagePatchForIds(state, state.selectedAssetIds, action.patch)

        is UploadPrepAction.StageEditForAsset -> {
            if (action.assetId !in state.assets) return state
            stagePatchForIds(state, setOf(action.assetId), action.patch)
        }

        UploadPrepAction.ClearStagedForSelected -> state.copy(
            stagedEditsByAssetId = state.stagedEditsByAssetId.filterKeys { it !in state.selectedAssetIds }
        )

        is UploadPrepAction.SetBulkEditDraft -> state.copy(bulkEditDraft = action.draft)

        UploadPrepAction.ApplyBulkEditDraftToSelected -> {
            val patch = state.bulkEditDraft.toPatch() ?: return state
            stagePatchForIds(state, state.selectedAssetIds, patch)
        }

        UploadPrepAction.ClearBulkEditDraft -> state.copy(bulkEditDraft = BulkEditDraft())
    }

private fun stagePatchForIds(
    state: UploadPrepState,
    targetIds: Set<LocalAssetId>,
    patch: AssetEditPatch
): UploadPrepState {
    if (targetIds.isEmpty()) return state

    val nextStaged = state.stagedEditsByAssetId.toMutableMap()
    targetIds.forEach { assetId ->
        val current = nextStaged[assetId]
        nextStaged[assetId] = if (current == null) patch else current.merge(patch)
    }

    return state.copy(stagedEditsByAssetId = nextStaged)
}

private fun BulkEditDraft.toPatch(): AssetEditPatch? {
    val addTags = parseTagList(addTagIds)
    val removeTags = parseTagList(removeTagIds)

    val patch = AssetEditPatch(
        description = if (includeDescription) FieldPatch.Set(description.ifBlank { null }) else FieldPatch.Unset,
        isFavorite = if (includeFavorite) FieldPatch.Set(isFavorite) else FieldPatch.Unset,
        dateTimeOriginal = if (includeDateTimeOriginal && dateTimeOriginal.isNotBlank()) {
            FieldPatch.Set(dateTimeOriginal)
        } else {
            FieldPatch.Unset
        },
        albumId = if (includeAlbumId) FieldPatch.Set(albumId.ifBlank { null }) else FieldPatch.Unset,
        addTagIds = addTags,
        removeTagIds = removeTags
    )

    return if (
        patch.description is FieldPatch.Unset &&
        patch.isFavorite is FieldPatch.Unset &&
        patch.dateTimeOriginal is FieldPatch.Unset &&
        patch.albumId is FieldPatch.Unset &&
        patch.addTagIds.isEmpty() &&
        patch.removeTagIds.isEmpty()
    ) {
        null
    } else {
        patch
    }
}

private fun parseTagList(value: String): Set<String> =
    value.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

class UploadPrepStore(initialState: UploadPrepState = UploadPrepState()) {
    var state by mutableStateOf(initialState)
        private set

    fun dispatch(action: UploadPrepAction) {
        state = reduceUploadPrepState(state, action)
    }
}

data class LocalIntakeFile(
    val name: String,
    val type: String,
    val size: Long,
    val lastModifiedEpochMillis: Long,
    val previewUrl: String?
)

fun mapLocalIntakeFilesToAssets(files: List<LocalIntakeFile>): List<LocalAsset> =
    files.mapIndexed { index, file ->
        val normalizedType = file.type.ifBlank { "application/octet-stream" }
        LocalAsset(
            id = LocalAssetId(
                "local-${file.name}-${file.size}-${file.lastModifiedEpochMillis}-$index"
            ),
            fileName = file.name,
            mimeType = normalizedType,
            fileSizeBytes = file.size,
            previewUrl = file.previewUrl,
            captureDateTime = null,
            timeZone = null
        )
    }
