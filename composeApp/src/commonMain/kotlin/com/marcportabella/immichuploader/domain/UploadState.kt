package com.marcportabella.immichuploader.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

data class BulkEditDraft(
    val includeDescription: Boolean = false,
    val description: String = "",
    val includeFavorite: Boolean = false,
    val isFavorite: Boolean = false,
    val includeDateTimeOriginal: Boolean = false,
    val dateTimeOriginal: String = "",
    val includeTimeZone: Boolean = false,
    val timeZone: String = "",
    val includeAlbumId: Boolean = false,
    val albumId: String = "",
    val addTagIds: String = "",
    val removeTagIds: String = ""
)

data class UploadPrepState(
    val assets: Map<LocalAssetId, LocalAsset> = emptyMap(),
    val selectedAssetIds: Set<LocalAssetId> = emptySet(),
    val stagedEditsByAssetId: Map<LocalAssetId, AssetEditPatch> = emptyMap(),
    val bulkEditDraft: BulkEditDraft = BulkEditDraft(),
    val apiKey: String = "",
    val albumCreateDraft: String = "",
    val tagCreateDraft: String = "",
    val availableAlbums: List<UploadCatalogEntry> = emptyList(),
    val availableTags: List<UploadCatalogEntry> = emptyList(),
    val sessionAlbumsById: Map<String, String> = emptyMap(),
    val sessionTagsById: Map<String, String> = emptyMap(),
    val duplicateAssetIds: Set<LocalAssetId> = emptySet(),
    val duplicateCheckStatus: DuplicateCheckStatus = DuplicateCheckStatus.Idle,
    val duplicateCheckMessage: String? = null,
    val catalogStatus: CatalogUiStatus = CatalogUiStatus.Idle,
    val catalogMessage: String? = null,
    val dryRunPlan: UploadRequestPlan? = null,
    val dryRunApiRequests: List<UploadApiRequest> = emptyList(),
    val dryRunMessage: String? = null,
    val executionStatus: UploadExecutionStatus = UploadExecutionStatus.Idle,
    val executionMessage: String? = null,
    val executionRequestCount: Int? = null,
    val batchFeedback: BatchFeedback? = null
)

data class BatchFeedback(
    val level: BatchFeedbackLevel,
    val message: String
)

enum class BatchFeedbackLevel {
    Error,
    Warning,
    Success
}

enum class CatalogUiStatus {
    Idle,
    Loading,
    Ready,
    BlockedMissingApiKey
}

enum class DuplicateCheckStatus {
    Idle,
    Checking,
    Ready,
    BlockedMissingApiKey
}

enum class UploadExecutionStatus {
    Idle,
    Executing,
    BlockedMissingApiKey,
    Submitted,
    Failed
}

sealed interface UploadPrepAction {
    data class ReplaceAssets(val assets: List<LocalAsset>) : UploadPrepAction
    data class ReplaceAssetMetadata(
        val assetId: LocalAssetId,
        val captureDateTime: String?,
        val timeZone: String?,
        val cameraMake: String?,
        val cameraModel: String?,
        val exifMetadata: Map<String, String>,
        val exifSummary: String?
    ) : UploadPrepAction
    data class ToggleSelection(val assetId: LocalAssetId) : UploadPrepAction
    data class SetSelection(val assetIds: Set<LocalAssetId>) : UploadPrepAction
    data object SelectAll : UploadPrepAction
    data object ClearSelection : UploadPrepAction
    data class StageEditForSelected(val patch: AssetEditPatch) : UploadPrepAction
    data class StageEditForAsset(val assetId: LocalAssetId, val patch: AssetEditPatch) : UploadPrepAction
    data class ReplaceTagEditsForAsset(
        val assetId: LocalAssetId,
        val addTagIds: Set<String>,
        val removeTagIds: Set<String>
    ) : UploadPrepAction
    data object ClearStagedForSelected : UploadPrepAction
    data class SetBulkEditDraft(val draft: BulkEditDraft) : UploadPrepAction
    data object ApplyBulkEditDraftToSelected : UploadPrepAction
    data object ClearBulkEditDraft : UploadPrepAction
    data class SetApiKey(val value: String) : UploadPrepAction
    data class SetAlbumCreateDraft(val value: String) : UploadPrepAction
    data class SetTagCreateDraft(val value: String) : UploadPrepAction
    data class CreateSessionAlbumForBulk(val name: String) : UploadPrepAction
    data class CreateSessionAlbumForAsset(val assetId: LocalAssetId, val name: String) : UploadPrepAction
    data class CreateSessionTagForBulk(val name: String) : UploadPrepAction
    data class CreateSessionTagForAsset(val assetId: LocalAssetId, val name: String) : UploadPrepAction
    data object DuplicateCheckStarted : UploadPrepAction
    data class DuplicateCheckCompleted(val duplicateAssetIds: Set<LocalAssetId>, val message: String) : UploadPrepAction
    data class DuplicateCheckBlockedMissingApiKey(val message: String) : UploadPrepAction
    data object CatalogRequestStarted : UploadPrepAction
    data class CatalogAlbumsLoaded(val albums: List<UploadCatalogEntry>, val message: String) : UploadPrepAction
    data class CatalogTagsLoaded(val tags: List<UploadCatalogEntry>, val message: String) : UploadPrepAction
    data class CatalogBlockedMissingApiKey(val message: String) : UploadPrepAction
    data object ClearCatalogMessage : UploadPrepAction
    data class DryRunPreviewGenerated(
        val plan: UploadRequestPlan,
        val requests: List<UploadApiRequest>,
        val message: String
    ) : UploadPrepAction
    data object GenerateDryRunPreview : UploadPrepAction
    data object ClearDryRunPreview : UploadPrepAction
    data class UploadExecutionStarted(val message: String) : UploadPrepAction
    data class UploadExecutionBlocked(val message: String) : UploadPrepAction
    data class UploadExecutionSubmitted(val requestCount: Int, val message: String) : UploadPrepAction
    data class UploadExecutionFailed(val message: String) : UploadPrepAction
    data object ClearUploadExecutionStatus : UploadPrepAction
    data object ClearBatchFeedback : UploadPrepAction
}

fun reduceUploadPrepState(state: UploadPrepState, action: UploadPrepAction): UploadPrepState =
    when (action) {
        is UploadPrepAction.ReplaceAssets -> {
            val nextAssets = action.assets.associateBy { it.id }
            val validIds = nextAssets.keys
            state.copy(
                assets = nextAssets,
                selectedAssetIds = validIds,
                stagedEditsByAssetId = state.stagedEditsByAssetId.filterKeys { it in validIds },
                duplicateAssetIds = emptySet(),
                duplicateCheckStatus = DuplicateCheckStatus.Idle,
                duplicateCheckMessage = null,
                executionStatus = UploadExecutionStatus.Idle,
                executionMessage = null,
                executionRequestCount = null,
                batchFeedback = null
            )
        }

        is UploadPrepAction.ReplaceAssetMetadata -> {
            val asset = state.assets[action.assetId] ?: return state
            val nextAsset = asset.copy(
                captureDateTime = action.captureDateTime,
                timeZone = action.timeZone,
                cameraMake = action.cameraMake,
                cameraModel = action.cameraModel,
                exifMetadata = action.exifMetadata,
                exifSummary = action.exifSummary
            )
            state.copy(assets = state.assets + (action.assetId to nextAsset))
        }

        is UploadPrepAction.ToggleSelection -> {
            if (action.assetId !in state.assets) return state
            if (action.assetId in state.duplicateAssetIds) return state
            val nextSelection = state.selectedAssetIds.toMutableSet()
            if (!nextSelection.add(action.assetId)) {
                nextSelection.remove(action.assetId)
            }
            state.copy(selectedAssetIds = nextSelection, batchFeedback = null)
        }

        is UploadPrepAction.SetSelection -> state.copy(
            selectedAssetIds = action.assetIds.intersect(state.assets.keys).filterNot { it in state.duplicateAssetIds }.toSet(),
            batchFeedback = null
        )

        UploadPrepAction.SelectAll -> state.copy(
            selectedAssetIds = state.assets.keys.filterNot { it in state.duplicateAssetIds }.toSet(),
            batchFeedback = null
        )

        UploadPrepAction.ClearSelection -> state.copy(selectedAssetIds = emptySet(), batchFeedback = null)

        is UploadPrepAction.StageEditForSelected ->
            stagePatchForIds(state, state.selectedAssetIds, action.patch)

        is UploadPrepAction.StageEditForAsset -> {
            if (action.assetId !in state.assets) return state
            stagePatchForIds(state, setOf(action.assetId), action.patch)
        }

        is UploadPrepAction.ReplaceTagEditsForAsset -> {
            if (action.assetId !in state.assets) return state
            val current = state.stagedEditsByAssetId[action.assetId] ?: AssetEditPatch()
            val nextPatch = current.copy(
                addTagIds = action.addTagIds,
                removeTagIds = action.removeTagIds
            )
            val nextStaged = state.stagedEditsByAssetId.toMutableMap()
            if (isNoopPatch(nextPatch)) {
                nextStaged.remove(action.assetId)
            } else {
                nextStaged[action.assetId] = nextPatch
            }
            state.copy(stagedEditsByAssetId = nextStaged)
        }

        UploadPrepAction.ClearStagedForSelected -> {
            if (state.selectedAssetIds.isEmpty()) {
                state.copy(
                    batchFeedback = BatchFeedback(
                        level = BatchFeedbackLevel.Warning,
                        message = "No editable assets in the batch to clear."
                    )
                )
            } else {
                state.copy(
                    stagedEditsByAssetId = state.stagedEditsByAssetId.filterKeys { it !in state.selectedAssetIds },
                    batchFeedback = BatchFeedback(
                        level = BatchFeedbackLevel.Success,
                        message = "Cleared staged edits for ${state.selectedAssetIds.size} batch assets."
                    )
                )
            }
        }

        is UploadPrepAction.SetBulkEditDraft -> state.copy(
            bulkEditDraft = action.draft,
            batchFeedback = null
        )

        UploadPrepAction.ApplyBulkEditDraftToSelected -> {
            val preflightFeedback = preflightBulkEditDraft(state)
            if (preflightFeedback != null) {
                state.copy(batchFeedback = preflightFeedback)
            } else {
                val patch = state.bulkEditDraft.toPatch()
                    ?: return state.copy(
                        batchFeedback = BatchFeedback(
                            level = BatchFeedbackLevel.Warning,
                            message = "No bulk fields selected to apply."
                        )
                    )

                stagePatchForIds(state, state.selectedAssetIds, patch).copy(
                    batchFeedback = BatchFeedback(
                        level = BatchFeedbackLevel.Success,
                        message = "Applied bulk edits to ${state.selectedAssetIds.size} batch assets."
                    )
                )
            }
        }

        UploadPrepAction.ClearBulkEditDraft -> state.copy(
            bulkEditDraft = BulkEditDraft(),
            batchFeedback = null
        )

        is UploadPrepAction.SetApiKey -> state.copy(
            apiKey = action.value,
            executionStatus = UploadExecutionStatus.Idle,
            executionMessage = null,
            executionRequestCount = null
        )

        is UploadPrepAction.SetAlbumCreateDraft -> state.copy(albumCreateDraft = action.value)

        is UploadPrepAction.SetTagCreateDraft -> state.copy(tagCreateDraft = action.value)

        UploadPrepAction.CatalogRequestStarted -> state.copy(
            catalogStatus = CatalogUiStatus.Loading,
            catalogMessage = null
        )

        is UploadPrepAction.CatalogAlbumsLoaded -> state.copy(
            availableAlbums = action.albums.sortedBy { it.name.lowercase() },
            catalogStatus = CatalogUiStatus.Ready,
            catalogMessage = action.message
        )

        is UploadPrepAction.CatalogTagsLoaded -> state.copy(
            availableTags = action.tags.sortedBy { it.name.lowercase() },
            catalogStatus = CatalogUiStatus.Ready,
            catalogMessage = action.message
        )

        is UploadPrepAction.CreateSessionAlbumForBulk -> {
            val resolved = resolveOrCreateAlbumEntry(state, action.name) ?: return state
            state.copy(
                availableAlbums = resolved.availableAlbums,
                sessionAlbumsById = resolved.sessionAlbumsById,
                bulkEditDraft = state.bulkEditDraft.copy(
                    includeAlbumId = true,
                    albumId = resolved.id
                ),
                batchFeedback = null
            )
        }

        is UploadPrepAction.CreateSessionAlbumForAsset -> {
            val asset = state.assets[action.assetId] ?: return state
            val resolved = resolveOrCreateAlbumEntry(state, action.name) ?: return state
            val currentPatch = state.stagedEditsByAssetId[action.assetId]
            val selectedAlbumId = (currentPatch?.albumId as? FieldPatch.Set<String?>)?.value ?: asset.albumId
            if (selectedAlbumId == resolved.id &&
                resolved.availableAlbums == state.availableAlbums &&
                resolved.sessionAlbumsById == state.sessionAlbumsById
            ) {
                return state
            }

            val patch = AssetEditPatch(albumId = FieldPatch.Set(resolved.id))
            stagePatchForIds(state, setOf(action.assetId), patch).copy(
                availableAlbums = resolved.availableAlbums,
                sessionAlbumsById = resolved.sessionAlbumsById,
                batchFeedback = null
            )
        }

        is UploadPrepAction.CreateSessionTagForBulk -> {
            val resolved = resolveOrCreateTagEntry(state, action.name) ?: return state
            val addIds = parseTagList(state.bulkEditDraft.addTagIds) + resolved.id
            val removeIds = parseTagList(state.bulkEditDraft.removeTagIds) - resolved.id
            state.copy(
                availableTags = resolved.availableTags,
                sessionTagsById = resolved.sessionTagsById,
                bulkEditDraft = state.bulkEditDraft.copy(
                    addTagIds = addIds.sorted().joinToString(","),
                    removeTagIds = removeIds.sorted().joinToString(",")
                ),
                batchFeedback = null
            )
        }

        is UploadPrepAction.CreateSessionTagForAsset -> {
            val asset = state.assets[action.assetId] ?: return state
            val resolved = resolveOrCreateTagEntry(state, action.name) ?: return state
            val currentPatch = state.stagedEditsByAssetId[action.assetId]
            val selectedIds = (asset.tagIds + (currentPatch?.addTagIds ?: emptySet())) - (currentPatch?.removeTagIds ?: emptySet())
            if (resolved.id in selectedIds &&
                resolved.availableTags == state.availableTags &&
                resolved.sessionTagsById == state.sessionTagsById
            ) {
                return state
            }

            val desiredIds = selectedIds + resolved.id
            val patch = AssetEditPatch(
                addTagIds = desiredIds - asset.tagIds,
                removeTagIds = asset.tagIds - desiredIds
            )
            stagePatchForIds(state, setOf(action.assetId), patch).copy(
                availableTags = resolved.availableTags,
                sessionTagsById = resolved.sessionTagsById,
                batchFeedback = null
            )
        }

        UploadPrepAction.DuplicateCheckStarted -> state.copy(
            duplicateCheckStatus = DuplicateCheckStatus.Checking,
            duplicateCheckMessage = "Checking server for duplicates."
        )

        is UploadPrepAction.DuplicateCheckCompleted -> {
            val duplicateIds = action.duplicateAssetIds.intersect(state.assets.keys)
            state.copy(
                duplicateAssetIds = duplicateIds,
                duplicateCheckStatus = DuplicateCheckStatus.Ready,
                duplicateCheckMessage = action.message,
                selectedAssetIds = state.assets.keys - duplicateIds,
                stagedEditsByAssetId = state.stagedEditsByAssetId.filterKeys { it !in duplicateIds }
            )
        }

        is UploadPrepAction.DuplicateCheckBlockedMissingApiKey -> state.copy(
            duplicateAssetIds = emptySet(),
            duplicateCheckStatus = DuplicateCheckStatus.BlockedMissingApiKey,
            duplicateCheckMessage = action.message
        )

        is UploadPrepAction.CatalogBlockedMissingApiKey -> state.copy(
            catalogStatus = CatalogUiStatus.BlockedMissingApiKey,
            catalogMessage = action.message
        )

        UploadPrepAction.ClearCatalogMessage -> state.copy(catalogMessage = null)

        is UploadPrepAction.DryRunPreviewGenerated -> state.copy(
            dryRunPlan = action.plan,
            dryRunApiRequests = action.requests,
            dryRunMessage = action.message,
            executionStatus = UploadExecutionStatus.Idle,
            executionMessage = null,
            executionRequestCount = null,
            batchFeedback = BatchFeedback(
                level = if (action.requests.isEmpty()) BatchFeedbackLevel.Warning else BatchFeedbackLevel.Success,
                message = action.message
            )
        )

        UploadPrepAction.GenerateDryRunPreview -> {
            val preflightFeedback = preflightDryRun(state)
            if (preflightFeedback != null) {
                state.copy(
                    dryRunPlan = null,
                    dryRunApiRequests = emptyList(),
                    dryRunMessage = preflightFeedback.message,
                    executionStatus = UploadExecutionStatus.Idle,
                    executionMessage = null,
                    executionRequestCount = null,
                    batchFeedback = preflightFeedback
                )
            } else {
                val plan = UploadRequestPlanner.buildDryRunPlan(state)
                val requests = UploadRequestPlanner.buildPayloadInspectorRequests(plan)
                val feedback = if (requests.isEmpty()) {
                    BatchFeedback(
                        level = BatchFeedbackLevel.Warning,
                        message = "No operations planned. Pick a batch and/or stage edits first."
                    )
                } else {
                    BatchFeedback(
                        level = BatchFeedbackLevel.Success,
                        message = "Dry-run generated ${requests.size} operations."
                    )
                }
                state.copy(
                    dryRunPlan = plan,
                    dryRunApiRequests = requests,
                    dryRunMessage = feedback.message,
                    executionStatus = UploadExecutionStatus.Idle,
                    executionMessage = null,
                    executionRequestCount = null,
                    batchFeedback = feedback
                )
            }
        }

        UploadPrepAction.ClearDryRunPreview -> state.copy(
            dryRunPlan = null,
            dryRunApiRequests = emptyList(),
            dryRunMessage = null,
            executionStatus = UploadExecutionStatus.Idle,
            executionMessage = null,
            executionRequestCount = null,
            batchFeedback = null
        )

        is UploadPrepAction.UploadExecutionStarted -> state.copy(
            executionStatus = UploadExecutionStatus.Executing,
            executionMessage = action.message,
            executionRequestCount = null
        )

        is UploadPrepAction.UploadExecutionBlocked -> state.copy(
            executionStatus = UploadExecutionStatus.BlockedMissingApiKey,
            executionMessage = action.message,
            executionRequestCount = null
        )

        is UploadPrepAction.UploadExecutionSubmitted -> state.copy(
            executionStatus = UploadExecutionStatus.Submitted,
            executionMessage = action.message,
            executionRequestCount = action.requestCount
        )

        is UploadPrepAction.UploadExecutionFailed -> state.copy(
            executionStatus = UploadExecutionStatus.Failed,
            executionMessage = action.message,
            executionRequestCount = null
        )

        UploadPrepAction.ClearUploadExecutionStatus -> state.copy(
            executionStatus = UploadExecutionStatus.Idle,
            executionMessage = null,
            executionRequestCount = null
        )

        UploadPrepAction.ClearBatchFeedback -> state.copy(batchFeedback = null)
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

private fun isNoopPatch(patch: AssetEditPatch): Boolean =
    patch.description is FieldPatch.Unset &&
        patch.isFavorite is FieldPatch.Unset &&
        patch.dateTimeOriginal is FieldPatch.Unset &&
        patch.timeZone is FieldPatch.Unset &&
        patch.albumId is FieldPatch.Unset &&
        patch.addTagIds.isEmpty() &&
        patch.removeTagIds.isEmpty() &&
        patch.customMetadata.isEmpty()

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
        timeZone = if (includeTimeZone && timeZone.isNotBlank()) {
            FieldPatch.Set(normalizeTimeZoneOffset(timeZone) ?: normalizeTimeZoneId(timeZone) ?: timeZone)
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
        patch.timeZone is FieldPatch.Unset &&
        patch.albumId is FieldPatch.Unset &&
        patch.addTagIds.isEmpty() &&
        patch.removeTagIds.isEmpty()
    ) {
        null
    } else {
        patch
    }
}

fun preflightBulkEditDraft(state: UploadPrepState): BatchFeedback? {
    if (state.selectedAssetIds.isEmpty()) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Error,
            message = "Pick a batch with at least one non-duplicate asset before applying bulk edits."
        )
    }

    val draft = state.bulkEditDraft
    if (draft.includeDateTimeOriginal && draft.dateTimeOriginal.isBlank()) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Error,
            message = "Date/time is required when date/time edit is enabled."
        )
    }

    if (draft.includeDateTimeOriginal && !isIsoUtcDateTime(draft.dateTimeOriginal)) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Error,
            message = "Date/time must use ISO 8601 UTC format: YYYY-MM-DDTHH:MM:SSZ."
        )
    }

    if (draft.includeTimeZone && draft.timeZone.isBlank()) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Error,
            message = "Timezone is required when timezone edit is enabled."
        )
    }

    if (draft.includeTimeZone &&
        normalizeTimeZoneOffset(draft.timeZone) == null &&
        normalizeTimeZoneId(draft.timeZone) == null
    ) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Error,
            message = "Timezone must be an IANA zone (e.g. Europe/Berlin) or offset like +02:00 / Z."
        )
    }

    val addTags = parseTagList(draft.addTagIds)
    val removeTags = parseTagList(draft.removeTagIds)
    val conflictingTags = addTags.intersect(removeTags)
    if (conflictingTags.isNotEmpty()) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Error,
            message = "Tag IDs cannot be both added and removed: ${conflictingTags.sorted().joinToString(", ")}."
        )
    }

    if (draft.toPatch() == null) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Warning,
            message = "No bulk fields selected to apply."
        )
    }

    return null
}

fun canApplyBulkEdit(state: UploadPrepState): Boolean =
    preflightBulkEditDraft(state) == null

private fun preflightDryRun(state: UploadPrepState): BatchFeedback? {
    if (state.selectedAssetIds.isEmpty()) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Error,
            message = "Pick a batch with at least one non-duplicate asset before generating a request plan."
        )
    }

    state.selectedAssetIds.forEach { assetId ->
        val patch = state.stagedEditsByAssetId[assetId] ?: return@forEach
        val dateTime = (patch.dateTimeOriginal as? FieldPatch.Set<String>)?.value ?: return@forEach
        if (!isIsoUtcDateTime(dateTime)) {
            return BatchFeedback(
                level = BatchFeedbackLevel.Error,
                message = "One or more staged date/time values are invalid. Use YYYY-MM-DDTHH:MM:SSZ."
            )
        }
    }

    return null
}

private fun parseTagList(value: String): Set<String> =
    value.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

private fun isIsoUtcDateTime(value: String): Boolean {
    if (!value.endsWith("Z")) return false
    return runCatching { Instant.parse(value) }.isSuccess
}

private fun normalizeTimeZoneId(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    return runCatching { TimeZone.of(trimmed).id }.getOrNull()
}

class UploadPrepStore(initialState: UploadPrepState = UploadPrepState()) {
    var state by mutableStateOf(initialState)
        private set

    fun dispatch(action: UploadPrepAction) {
        state = reduceUploadPrepState(state, action)
    }
}

private data class ResolvedAlbumEntry(
    val id: String,
    val availableAlbums: List<UploadCatalogEntry>,
    val sessionAlbumsById: Map<String, String>
)

private data class ResolvedTagEntry(
    val id: String,
    val availableTags: List<UploadCatalogEntry>,
    val sessionTagsById: Map<String, String>
)

private fun resolveOrCreateAlbumEntry(state: UploadPrepState, rawName: String): ResolvedAlbumEntry? {
    val normalizedName = rawName.trim()
    if (normalizedName.isEmpty()) return null

    val existing = state.availableAlbums.firstOrNull { it.name.equals(normalizedName, ignoreCase = true) }
    if (existing != null) {
        return ResolvedAlbumEntry(
            id = existing.id,
            availableAlbums = state.availableAlbums,
            sessionAlbumsById = state.sessionAlbumsById
        )
    }

    val sessionId = buildSessionAlbumId(normalizedName)
    val entry = UploadCatalogEntry(id = sessionId, name = normalizedName)
    val nextSessionAlbums = state.sessionAlbumsById + (sessionId to normalizedName)
    return ResolvedAlbumEntry(
        id = sessionId,
        availableAlbums = (state.availableAlbums + entry).sortedBy { it.name.lowercase() },
        sessionAlbumsById = nextSessionAlbums
    )
}

private fun resolveOrCreateTagEntry(state: UploadPrepState, rawName: String): ResolvedTagEntry? {
    val normalizedName = rawName.trim()
    if (normalizedName.isEmpty()) return null

    val existing = state.availableTags.firstOrNull { it.name.equals(normalizedName, ignoreCase = true) }
    if (existing != null) {
        return ResolvedTagEntry(
            id = existing.id,
            availableTags = state.availableTags,
            sessionTagsById = state.sessionTagsById
        )
    }

    val sessionId = buildSessionTagId(normalizedName)
    val entry = UploadCatalogEntry(id = sessionId, name = normalizedName)
    val nextSessionTags = state.sessionTagsById + (sessionId to normalizedName)
    return ResolvedTagEntry(
        id = sessionId,
        availableTags = (state.availableTags + entry).sortedBy { it.name.lowercase() },
        sessionTagsById = nextSessionTags
    )
}

private const val SESSION_ALBUM_ID_PREFIX = "session-album:"
private const val SESSION_TAG_ID_PREFIX = "session-tag:"

private fun buildSessionAlbumId(name: String): String =
    "$SESSION_ALBUM_ID_PREFIX${name.lowercase().trim().hashCode().toUInt().toString(16)}"

private fun buildSessionTagId(name: String): String =
    "$SESSION_TAG_ID_PREFIX${name.lowercase().trim().hashCode().toUInt().toString(16)}"
