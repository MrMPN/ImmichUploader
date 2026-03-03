package com.marcportabella.immichuploader

import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.BatchFeedback
import com.marcportabella.immichuploader.domain.BatchFeedbackLevel
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.CatalogUiStatus
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadApiRequest as DomainUploadApiRequest
import com.marcportabella.immichuploader.domain.UploadCatalogEntry
import com.marcportabella.immichuploader.domain.UploadExecutionStatus
import com.marcportabella.immichuploader.domain.UploadPrepAction
import com.marcportabella.immichuploader.domain.UploadPrepState
import com.marcportabella.immichuploader.domain.UploadRequestPlan as DomainUploadRequestPlan
import com.marcportabella.immichuploader.domain.canApplyBulkEdit
import com.marcportabella.immichuploader.domain.reduceUploadPrepState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UploadPrepReducerSliceWebTest {
    private val testApiBaseUrl = "https://immich.test/api"

    @Test
    fun toggleSelectionIgnoresUnknownAssetAndPreservesFeedback() {
        val known = LocalAssetId("known")
        val initial = UploadPrepState(
            assets = mapOf(known to LocalAsset(known, "a.jpg", "image/jpeg", 1, null, null, null)),
            batchFeedback = BatchFeedback(BatchFeedbackLevel.Warning, "keep")
        )

        val next = reduceUploadPrepState(
            initial,
            UploadPrepAction.ToggleSelection(LocalAssetId("missing"))
        )

        assertEquals(initial, next)
    }

    @Test
    fun stageEditForSelectedOnlyAffectsSelectedAssets() {
        val a = LocalAssetId("a")
        val b = LocalAssetId("b")
        val initial = UploadPrepState(
            assets = mapOf(
                a to LocalAsset(a, "a.jpg", "image/jpeg", 1, null, null, null),
                b to LocalAsset(b, "b.jpg", "image/jpeg", 2, null, null, null)
            ),
            selectedAssetIds = setOf(a)
        )

        val next = reduceUploadPrepState(
            initial,
            UploadPrepAction.StageEditForSelected(AssetEditPatch(isFavorite = FieldPatch.Set(true)))
        )

        assertEquals(setOf(a), next.stagedEditsByAssetId.keys)
        val patchA = next.stagedEditsByAssetId[a]
        assertNotNull(patchA)
        assertEquals(true, (patchA.isFavorite as FieldPatch.Set<Boolean>).value)
    }

    @Test
    fun stagedEditsMergeNonDestructively() {
        val id = LocalAssetId("a")
        val initial = UploadPrepState(
            assets = mapOf(id to LocalAsset(id, "a.jpg", "image/jpeg", 1, null, null, null)),
            selectedAssetIds = setOf(id),
            stagedEditsByAssetId = mapOf(
                id to AssetEditPatch(description = FieldPatch.Set("original"))
            )
        )

        val next = reduceUploadPrepState(
            initial,
            UploadPrepAction.StageEditForSelected(AssetEditPatch(isFavorite = FieldPatch.Set(true)))
        )

        val merged = next.stagedEditsByAssetId[id]
        assertNotNull(merged)
        assertEquals("original", (merged.description as FieldPatch.Set<String?>).value)
        assertEquals(true, (merged.isFavorite as FieldPatch.Set<Boolean>).value)
    }

    @Test
    fun replaceAssetsPrunesInvalidSelectionAndStagedEditsAndClearsFeedback() {
        val keep = LocalAssetId("keep")
        val drop = LocalAssetId("drop")
        val initial = UploadPrepState(
            assets = mapOf(
                keep to LocalAsset(keep, "keep.jpg", "image/jpeg", 1, null, null, null),
                drop to LocalAsset(drop, "drop.jpg", "image/jpeg", 1, null, null, null)
            ),
            selectedAssetIds = setOf(keep, drop),
            stagedEditsByAssetId = mapOf(
                keep to AssetEditPatch(description = FieldPatch.Set("k")),
                drop to AssetEditPatch(description = FieldPatch.Set("d"))
            ),
            batchFeedback = BatchFeedback(BatchFeedbackLevel.Success, "clear")
        )

        val next = reduceUploadPrepState(
            initial,
            UploadPrepAction.ReplaceAssets(
                listOf(
                    LocalAsset(keep, "keep-next.jpg", "image/jpeg", 2, null, null, null)
                )
            )
        )

        assertEquals(setOf(keep), next.assets.keys)
        assertEquals(setOf(keep), next.selectedAssetIds)
        assertEquals(setOf(keep), next.stagedEditsByAssetId.keys)
        assertNull(next.batchFeedback)
    }

    @Test
    fun selectAllSelectsLoadedAssetsOnly() {
        val a = LocalAssetId("a")
        val b = LocalAssetId("b")
        val initial = UploadPrepState(
            assets = mapOf(
                a to LocalAsset(a, "a.jpg", "image/jpeg", 1, null, null, null),
                b to LocalAsset(b, "b.jpg", "image/jpeg", 2, null, null, null)
            ),
            selectedAssetIds = setOf(a)
        )

        val next = reduceUploadPrepState(initial, UploadPrepAction.SelectAll)

        assertEquals(setOf(a, b), next.selectedAssetIds)
    }

    @Test
    fun applyBulkDraftStagesOnlyEditedFieldsForSelectedSubset() {
        val a = LocalAssetId("a")
        val b = LocalAssetId("b")
        val c = LocalAssetId("c")

        val initial = UploadPrepState(
            assets = mapOf(
                a to LocalAsset(a, "a.jpg", "image/jpeg", 1, null, null, null),
                b to LocalAsset(b, "b.jpg", "image/jpeg", 2, null, null, null),
                c to LocalAsset(c, "c.jpg", "image/jpeg", 3, null, null, null)
            ),
            selectedAssetIds = setOf(a, c),
            stagedEditsByAssetId = mapOf(
                a to AssetEditPatch(description = FieldPatch.Set("keep-me"))
            ),
            bulkEditDraft = BulkEditDraft(
                includeFavorite = true,
                isFavorite = true,
                addTagIds = "tag-a, tag-b"
            )
        )

        val next = reduceUploadPrepState(initial, UploadPrepAction.ApplyBulkEditDraftToSelected)

        assertEquals(setOf(a, c), next.stagedEditsByAssetId.keys)

        val patchA = next.stagedEditsByAssetId[a]
        assertNotNull(patchA)
        assertEquals("keep-me", (patchA.description as FieldPatch.Set<String?>).value)
        assertEquals(true, (patchA.isFavorite as FieldPatch.Set<Boolean>).value)
        assertEquals(setOf("tag-a", "tag-b"), patchA.addTagIds)

        val patchC = next.stagedEditsByAssetId[c]
        assertNotNull(patchC)
        assertTrue(patchC.description is FieldPatch.Unset)
        assertEquals(true, (patchC.isFavorite as FieldPatch.Set<Boolean>).value)
        assertEquals(setOf("tag-a", "tag-b"), patchC.addTagIds)

        assertNull(next.stagedEditsByAssetId[b])
    }

    @Test
    fun bulkDraftValidationBlocksApplyWhenSelectionMissing() {
        val next = reduceUploadPrepState(
            UploadPrepState(
                bulkEditDraft = BulkEditDraft(includeFavorite = true, isFavorite = true)
            ),
            UploadPrepAction.ApplyBulkEditDraftToSelected
        )

        assertTrue(next.stagedEditsByAssetId.isEmpty())
        assertEquals(BatchFeedbackLevel.Error, next.batchFeedback?.level)
        assertEquals(
            "Pick a batch with at least one non-duplicate asset before applying bulk edits.",
            next.batchFeedback?.message
        )
        assertFalse(canApplyBulkEdit(next))
    }

    @Test
    fun bulkDraftValidationBlocksMalformedDateTime() {
        val id = LocalAssetId("a")
        val next = reduceUploadPrepState(
            UploadPrepState(
                assets = mapOf(id to LocalAsset(id, "a.jpg", "image/jpeg", 1, null, null, null)),
                selectedAssetIds = setOf(id),
                bulkEditDraft = BulkEditDraft(
                    includeDateTimeOriginal = true,
                    dateTimeOriginal = "2026/01/01 00:00:00"
                )
            ),
            UploadPrepAction.ApplyBulkEditDraftToSelected
        )

        assertNull(next.stagedEditsByAssetId[id])
        assertEquals(BatchFeedbackLevel.Error, next.batchFeedback?.level)
        assertEquals(
            "Date/time must use ISO 8601 UTC format: YYYY-MM-DDTHH:MM:SSZ.",
            next.batchFeedback?.message
        )
    }

    @Test
    fun bulkDraftValidationBlocksConflictingTagOperations() {
        val id = LocalAssetId("a")
        val next = reduceUploadPrepState(
            UploadPrepState(
                assets = mapOf(id to LocalAsset(id, "a.jpg", "image/jpeg", 1, null, null, null)),
                selectedAssetIds = setOf(id),
                bulkEditDraft = BulkEditDraft(
                    addTagIds = "tag-1,tag-2",
                    removeTagIds = "tag-2"
                )
            ),
            UploadPrepAction.ApplyBulkEditDraftToSelected
        )

        assertNull(next.stagedEditsByAssetId[id])
        assertEquals(BatchFeedbackLevel.Error, next.batchFeedback?.level)
        assertEquals(
            "Tag IDs cannot be both added and removed: tag-2.",
            next.batchFeedback?.message
        )
    }

    @Test
    fun clearStagedForSelectedReportsWarningWhenNothingSelected() {
        val next = reduceUploadPrepState(
            UploadPrepState(
                stagedEditsByAssetId = mapOf(
                    LocalAssetId("a") to AssetEditPatch(description = FieldPatch.Set("staged"))
                )
            ),
            UploadPrepAction.ClearStagedForSelected
        )

        assertEquals(BatchFeedbackLevel.Warning, next.batchFeedback?.level)
        assertEquals("No editable assets in the batch to clear.", next.batchFeedback?.message)
        assertEquals(1, next.stagedEditsByAssetId.size)
    }

    @Test
    fun dryRunPreflightRequiresSelectionAndValidStagedDateTime() {
        val emptySelection = reduceUploadPrepState(
            UploadPrepState(),
            UploadPrepAction.GenerateDryRunPreview
        )
        assertEquals(BatchFeedbackLevel.Error, emptySelection.batchFeedback?.level)
        assertEquals(
            "Pick a batch with at least one non-duplicate asset before generating a request plan.",
            emptySelection.batchFeedback?.message
        )

        val id = LocalAssetId("a")
        val invalidDate = reduceUploadPrepState(
            UploadPrepState(
                assets = mapOf(id to LocalAsset(id, "a.jpg", "image/jpeg", 1, null, null, null)),
                selectedAssetIds = setOf(id),
                serverBaseUrl = testApiBaseUrl,
                stagedEditsByAssetId = mapOf(
                    id to AssetEditPatch(dateTimeOriginal = FieldPatch.Set("not-a-date"))
                )
            ),
            UploadPrepAction.GenerateDryRunPreview
        )

        assertEquals(BatchFeedbackLevel.Error, invalidDate.batchFeedback?.level)
        assertEquals(
            "One or more staged date/time values are invalid. Use YYYY-MM-DDTHH:MM:SSZ.",
            invalidDate.batchFeedback?.message
        )
        assertNull(invalidDate.dryRunPlan)
    }

    @Test
    fun dryRunGenerationSetsSuccessFeedback() {
        val id = LocalAssetId("a")
        val next = reduceUploadPrepState(
            UploadPrepState(
                assets = mapOf(id to LocalAsset(id, "a.jpg", "image/jpeg", 1, null, null, null)),
                selectedAssetIds = setOf(id),
                serverBaseUrl = testApiBaseUrl
            ),
            UploadPrepAction.GenerateDryRunPreview
        )

        assertNotNull(next.dryRunPlan)
        assertTrue(next.dryRunApiRequests.isNotEmpty())
        assertEquals(BatchFeedbackLevel.Success, next.batchFeedback?.level)
    }

    @Test
    fun dryRunGenerationWarnsWhenSelectedIdsDoNotResolveToAssets() {
        val selectedOnly = LocalAssetId("selected-only")
        val next = reduceUploadPrepState(
            UploadPrepState(
                selectedAssetIds = setOf(selectedOnly),
                serverBaseUrl = testApiBaseUrl,
                availableAlbums = listOf(UploadCatalogEntry("a1", "Family")),
                availableTags = listOf(UploadCatalogEntry("t1", "Trip"))
            ),
            UploadPrepAction.GenerateDryRunPreview
        )

        assertNotNull(next.dryRunPlan)
        assertTrue(next.dryRunApiRequests.isEmpty())
        assertEquals(BatchFeedbackLevel.Warning, next.batchFeedback?.level)
        assertEquals(
            "No operations planned. Pick a batch and/or stage edits first.",
            next.batchFeedback?.message
        )
    }

    @Test
    fun dryRunPreflightValidatesOnlySelectedAssets() {
        val selected = LocalAssetId("selected")
        val unselected = LocalAssetId("unselected")
        val next = reduceUploadPrepState(
            UploadPrepState(
                assets = mapOf(
                    selected to LocalAsset(selected, "a.jpg", "image/jpeg", 1, null, null, null),
                    unselected to LocalAsset(unselected, "b.jpg", "image/jpeg", 1, null, null, null)
                ),
                selectedAssetIds = setOf(selected),
                serverBaseUrl = testApiBaseUrl,
                stagedEditsByAssetId = mapOf(
                    selected to AssetEditPatch(dateTimeOriginal = FieldPatch.Set("2026-01-01T00:00:00Z")),
                    unselected to AssetEditPatch(dateTimeOriginal = FieldPatch.Set("invalid"))
                )
            ),
            UploadPrepAction.GenerateDryRunPreview
        )

        assertEquals(BatchFeedbackLevel.Success, next.batchFeedback?.level)
        assertNotNull(next.dryRunPlan)
    }

    @Test
    fun catalogReducerTracksLoadingBlockedAndReadyStates() {
        val loading = reduceUploadPrepState(
            UploadPrepState(),
            UploadPrepAction.CatalogRequestStarted
        )
        assertEquals(CatalogUiStatus.Loading, loading.catalogStatus)

        val blocked = reduceUploadPrepState(
            loading,
            UploadPrepAction.CatalogBlockedMissingApiKey("missing key")
        )
        assertEquals(CatalogUiStatus.BlockedMissingApiKey, blocked.catalogStatus)
        assertEquals("missing key", blocked.catalogMessage)

        val ready = reduceUploadPrepState(
            blocked,
            UploadPrepAction.CatalogAlbumsLoaded(
                albums = listOf(
                    UploadCatalogEntry("2", "Zoo"),
                    UploadCatalogEntry("1", "Family")
                ),
                message = "loaded"
            )
        )
        assertEquals(CatalogUiStatus.Ready, ready.catalogStatus)
        assertEquals(listOf("Family", "Zoo"), ready.availableAlbums.map { it.name })
        assertEquals("loaded", ready.catalogMessage)
    }

    @Test
    fun uploadExecutionReducerTracksStatusAndClearsOnApiKeyOrDryRunReset() {
        val started = reduceUploadPrepState(
            UploadPrepState(),
            UploadPrepAction.UploadExecutionStarted("running")
        )
        assertEquals(UploadExecutionStatus.Executing, started.executionStatus)
        assertEquals("running", started.executionMessage)
        assertNull(started.executionRequestCount)

        val submitted = reduceUploadPrepState(
            started,
            UploadPrepAction.UploadExecutionSubmitted(
                requestCount = 3,
                message = "submitted"
            )
        )
        assertEquals(UploadExecutionStatus.Submitted, submitted.executionStatus)
        assertEquals("submitted", submitted.executionMessage)
        assertEquals(3, submitted.executionRequestCount)

        val resetByApiKey = reduceUploadPrepState(
            submitted,
            UploadPrepAction.SetApiKey("new-key")
        )
        assertEquals(UploadExecutionStatus.Idle, resetByApiKey.executionStatus)
        assertNull(resetByApiKey.executionMessage)
        assertNull(resetByApiKey.executionRequestCount)

        val failed = reduceUploadPrepState(
            resetByApiKey,
            UploadPrepAction.UploadExecutionFailed("boom")
        )
        assertEquals(UploadExecutionStatus.Failed, failed.executionStatus)
        assertEquals("boom", failed.executionMessage)

        val resetByDryRunClear = reduceUploadPrepState(
            failed.copy(
                dryRunPlan = DomainUploadRequestPlan(),
                dryRunApiRequests = listOf(
                    DomainUploadApiRequest("GET", "$testApiBaseUrl/albums")
                )
            ),
            UploadPrepAction.ClearDryRunPreview
        )
        assertEquals(UploadExecutionStatus.Idle, resetByDryRunClear.executionStatus)
        assertNull(resetByDryRunClear.executionMessage)
        assertNull(resetByDryRunClear.executionRequestCount)
    }

    @Test
    fun createSessionTagForBulkAddsCatalogTagAndUpdatesBulkDraft() {
        val initial = UploadPrepState(
            bulkEditDraft = BulkEditDraft(
                addTagIds = "existing-tag",
                removeTagIds = "old-tag"
            ),
            availableTags = listOf(UploadCatalogEntry("existing-tag", "Existing"))
        )

        val next = reduceUploadPrepState(
            initial,
            UploadPrepAction.CreateSessionTagForBulk("Trip 2026")
        )

        assertEquals(2, next.availableTags.size)
        assertTrue(next.availableTags.any { it.name == "Trip 2026" })
        val sessionId = next.availableTags.first { it.name == "Trip 2026" }.id
        assertEquals("existing-tag,$sessionId", next.bulkEditDraft.addTagIds)
        assertEquals("old-tag", next.bulkEditDraft.removeTagIds)
        assertEquals("Trip 2026", next.sessionTagsById[sessionId])
    }

    @Test
    fun createSessionTagForAssetStagesTagPatchAndKeepsTagCatalogInSession() {
        val assetId = LocalAssetId("asset-1")
        val initial = UploadPrepState(
            assets = mapOf(assetId to LocalAsset(assetId, "a.jpg", "image/jpeg", 1, null, null, null)),
            selectedAssetIds = setOf(assetId)
        )

        val next = reduceUploadPrepState(
            initial,
            UploadPrepAction.CreateSessionTagForAsset(assetId, "Roadtrip")
        )

        val patch = next.stagedEditsByAssetId[assetId]
        assertNotNull(patch)
        assertEquals(1, patch.addTagIds.size)
        assertTrue(next.availableTags.any { it.name == "Roadtrip" })
        val sessionId = patch.addTagIds.first()
        assertEquals("Roadtrip", next.sessionTagsById[sessionId])
    }

    @Test
    fun replaceTagEditsForAssetAllowsDeselectingPreviouslyAddedTags() {
        val assetId = LocalAssetId("asset-1")
        val initial = UploadPrepState(
            assets = mapOf(assetId to LocalAsset(assetId, "a.jpg", "image/jpeg", 1, null, null, null)),
            stagedEditsByAssetId = mapOf(
                assetId to AssetEditPatch(addTagIds = setOf("session-tag:abc"))
            )
        )

        val next = reduceUploadPrepState(
            initial,
            UploadPrepAction.ReplaceTagEditsForAsset(
                assetId = assetId,
                addTagIds = emptySet(),
                removeTagIds = emptySet()
            )
        )

        assertNull(next.stagedEditsByAssetId[assetId])
    }

    @Test
    fun duplicateAssetsCannotBeSelected() {
        val a = LocalAssetId("a")
        val b = LocalAssetId("b")
        val state = UploadPrepState(
            assets = mapOf(
                a to LocalAsset(a, "a.jpg", "image/jpeg", 1, null, null, null),
                b to LocalAsset(b, "b.jpg", "image/jpeg", 1, null, null, null)
            ),
            duplicateAssetIds = setOf(a)
        )

        val selected = reduceUploadPrepState(state, UploadPrepAction.SelectAll)
        assertEquals(setOf(b), selected.selectedAssetIds)

        val toggledDup = reduceUploadPrepState(selected, UploadPrepAction.ToggleSelection(a))
        assertEquals(setOf(b), toggledDup.selectedAssetIds)
    }

    @Test
    fun duplicateCheckCompletionPrunesSelectionAndStagedEditsForDuplicates() {
        val duplicate = LocalAssetId("dup")
        val keep = LocalAssetId("keep")
        val state = UploadPrepState(
            assets = mapOf(
                duplicate to LocalAsset(duplicate, "dup.jpg", "image/jpeg", 1, null, null, null),
                keep to LocalAsset(keep, "keep.jpg", "image/jpeg", 1, null, null, null)
            ),
            selectedAssetIds = setOf(duplicate, keep),
            stagedEditsByAssetId = mapOf(
                duplicate to AssetEditPatch(description = FieldPatch.Set("drop")),
                keep to AssetEditPatch(description = FieldPatch.Set("keep"))
            )
        )

        val next = reduceUploadPrepState(
            state,
            UploadPrepAction.DuplicateCheckCompleted(
                duplicateAssetIds = setOf(duplicate),
                message = "found dup"
            )
        )

        assertEquals(setOf(keep), next.selectedAssetIds)
        assertEquals(setOf(keep), next.stagedEditsByAssetId.keys)
        assertEquals(setOf(duplicate), next.duplicateAssetIds)
    }
}
