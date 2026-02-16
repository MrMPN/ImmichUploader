package com.marcportabella.immichuploader

import com.marcportabella.immichuploader.data.ApiKeyGatedImmichCatalogTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichTransport
import com.marcportabella.immichuploader.data.DryRunImmichCatalogTransport
import com.marcportabella.immichuploader.data.DryRunImmichTransport
import com.marcportabella.immichuploader.data.IMMICH_API_BASE_URL
import com.marcportabella.immichuploader.data.ImmichApiRequest
import com.marcportabella.immichuploader.data.ImmichBulkMetadataRequest
import com.marcportabella.immichuploader.data.ImmichCatalogEntry
import com.marcportabella.immichuploader.data.ImmichCatalogRequestBuilder
import com.marcportabella.immichuploader.data.ImmichCatalogResult
import com.marcportabella.immichuploader.data.ImmichLookupHook
import com.marcportabella.immichuploader.data.ImmichRequestBuilder
import com.marcportabella.immichuploader.data.ImmichRequestPlan
import com.marcportabella.immichuploader.data.ImmichUploadRequest
import com.marcportabella.immichuploader.data.TransportGateStatus
import com.marcportabella.immichuploader.data.UploadExecutionPath
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.BatchFeedback
import com.marcportabella.immichuploader.domain.BatchFeedbackLevel
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.CatalogUiStatus
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.LocalIntakeFile
import com.marcportabella.immichuploader.domain.UploadExecutionStatus
import com.marcportabella.immichuploader.domain.UploadPrepAction
import com.marcportabella.immichuploader.domain.UploadPrepState
import com.marcportabella.immichuploader.domain.canApplyBulkEdit
import com.marcportabella.immichuploader.domain.mapLocalIntakeFilesToAssets
import com.marcportabella.immichuploader.domain.parseJpegExifMetadata
import com.marcportabella.immichuploader.domain.reduceUploadPrepState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComposeAppWebTest {

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
        assertEquals("Select at least one asset before applying bulk edits.", next.batchFeedback?.message)
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
        assertEquals("No selected assets to clear.", next.batchFeedback?.message)
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
            "Select at least one asset before generating a dry-run plan.",
            emptySelection.batchFeedback?.message
        )

        val id = LocalAssetId("a")
        val invalidDate = reduceUploadPrepState(
            UploadPrepState(
                assets = mapOf(id to LocalAsset(id, "a.jpg", "image/jpeg", 1, null, null, null)),
                selectedAssetIds = setOf(id),
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
                selectedAssetIds = setOf(id)
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
                availableAlbums = listOf(ImmichCatalogEntry("a1", "Family")),
                availableTags = listOf(ImmichCatalogEntry("t1", "Trip"))
            ),
            UploadPrepAction.GenerateDryRunPreview
        )

        assertNotNull(next.dryRunPlan)
        assertTrue(next.dryRunApiRequests.isEmpty())
        assertEquals(BatchFeedbackLevel.Warning, next.batchFeedback?.level)
        assertEquals(
            "No operations planned. Select assets and/or stage edits first.",
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
    fun bulkMetadataBuilderCreatesRequestWhenPatchContainsMetadataFields() {
        val request = ImmichRequestBuilder.buildBulkMetadataRequest(
            assetIds = setOf("2", "1"),
            patch = AssetEditPatch(
                description = FieldPatch.Set("caption"),
                isFavorite = FieldPatch.Set(true),
                dateTimeOriginal = FieldPatch.Set("2026-01-01T00:00:00Z")
            )
        )

        assertNotNull(request)
        assertEquals(listOf("1", "2"), request.ids)
        assertEquals("caption", request.description)
        assertEquals(true, request.isFavorite)
        assertEquals("2026-01-01T00:00:00Z", request.dateTimeOriginal)
    }

    @Test
    fun bulkMetadataBuilderReturnsNullForNonMetadataOnlyPatch() {
        val request = ImmichRequestBuilder.buildBulkMetadataRequest(
            assetIds = setOf("1"),
            patch = AssetEditPatch(addTagIds = setOf("tag-1"))
        )

        assertNull(request)
    }

    @Test
    fun albumAndTagBuildersRespectPatchAndSelection() {
        val patch = AssetEditPatch(
            albumId = FieldPatch.Set("album-1"),
            addTagIds = setOf("tag-b", "tag-a")
        )

        val albumRequest = ImmichRequestBuilder.buildAlbumAddRequest(setOf("a2", "a1"), patch)
        val tagRequest = ImmichRequestBuilder.buildTagAssignRequest(setOf("a2", "a1"), patch)

        assertNotNull(albumRequest)
        assertEquals("album-1", albumRequest.albumId)
        assertEquals(listOf("a1", "a2"), albumRequest.assetIds)

        assertNotNull(tagRequest)
        assertEquals(listOf("a1", "a2"), tagRequest.assetIds)
        assertEquals(listOf("tag-a", "tag-b"), tagRequest.tagIds)
    }

    @Test
    fun albumBuilderReturnsNullForBlankAlbumId() {
        val request = ImmichRequestBuilder.buildAlbumAddRequest(
            assetIds = setOf("a1"),
            patch = AssetEditPatch(albumId = FieldPatch.Set("   "))
        )

        assertNull(request)
    }

    @Test
    fun lookupHooksIncludeLookupsAndCreateHooksDeterministically() {
        val hooks = ImmichRequestBuilder.buildLookupHooks(
            shouldLookupAlbums = true,
            shouldLookupTags = true,
            albumsToCreate = setOf("Family", "  "),
            tagsToCreate = setOf("Trip")
        )

        assertTrue(hooks.first() is ImmichLookupHook.LookupAlbums)
        assertTrue(hooks[1] is ImmichLookupHook.LookupTags)
        assertTrue(hooks[2] is ImmichLookupHook.CreateAlbumIfMissing)
        assertTrue(hooks[3] is ImmichLookupHook.CreateTagIfMissing)
    }

    @Test
    fun catalogRequestBuilderUsesFixedBaseUrlContracts() {
        val albumsLookup = ImmichCatalogRequestBuilder.lookupAlbums()
        val tagsLookup = ImmichCatalogRequestBuilder.lookupTags()
        val createAlbum = ImmichCatalogRequestBuilder.createAlbum("Family")
        val createTag = ImmichCatalogRequestBuilder.createTag("Trip")

        assertEquals("GET", albumsLookup.method)
        assertEquals("$IMMICH_API_BASE_URL/albums", albumsLookup.url)
        assertNull(albumsLookup.body)

        assertEquals("GET", tagsLookup.method)
        assertEquals("$IMMICH_API_BASE_URL/tags", tagsLookup.url)
        assertNull(tagsLookup.body)

        assertEquals("POST", createAlbum.method)
        assertEquals("$IMMICH_API_BASE_URL/albums", createAlbum.url)
        assertEquals("""{"name":"Family"}""", createAlbum.body)

        assertEquals("POST", createTag.method)
        assertEquals("$IMMICH_API_BASE_URL/tags", createTag.url)
        assertEquals("""{"name":"Trip"}""", createTag.body)
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
                    ImmichCatalogEntry("2", "Zoo"),
                    ImmichCatalogEntry("1", "Family")
                ),
                message = "loaded"
            )
        )
        assertEquals(CatalogUiStatus.Ready, ready.catalogStatus)
        assertEquals(listOf("Family", "Zoo"), ready.availableAlbums.map { it.name })
        assertEquals("loaded", ready.catalogMessage)
    }

    @Test
    fun catalogTransportBlocksWhenApiKeyMissingAndSupportsDryRunCreation() {
        val transport = ApiKeyGatedImmichCatalogTransport(DryRunImmichCatalogTransport())

        val blocked = transport.lookupAlbums(apiKey = null)
        assertIs<ImmichCatalogResult.BlockedMissingApiKey>(blocked)

        val createdAlbum = transport.createAlbumIfMissing(apiKey = "k", name = "Family")
        assertIs<ImmichCatalogResult.DryRunSuccess>(createdAlbum)
        assertEquals(1, createdAlbum.entries.size)
        assertEquals("Family", createdAlbum.entries.first().name)

        val loadedAlbums = transport.lookupAlbums(apiKey = "k")
        assertIs<ImmichCatalogResult.DryRunSuccess>(loadedAlbums)
        assertEquals(1, loadedAlbums.entries.size)
    }

    @Test
    fun uploadTransportPathSelectionAndGateFollowApiKeyPresence() {
        val transport = ApiKeyGatedImmichTransport(DryRunImmichTransport())

        assertEquals(
            UploadExecutionPath.BlockedMissingApiKey,
            transport.selectExecutionPath(apiKey = null)
        )
        assertEquals(
            UploadExecutionPath.BlockedMissingApiKey,
            transport.selectExecutionPath(apiKey = "   ")
        )
        assertEquals(
            UploadExecutionPath.ApiExecution,
            transport.selectExecutionPath(apiKey = "key")
        )
        assertEquals(TransportGateStatus.MissingApiKey, transport.gateStatus(null))
        assertEquals(TransportGateStatus.Ready, transport.gateStatus("key"))
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
                dryRunPlan = ImmichRequestPlan(),
                dryRunApiRequests = listOf(
                    ImmichApiRequest("GET", "$IMMICH_API_BASE_URL/albums")
                )
            ),
            UploadPrepAction.ClearDryRunPreview
        )
        assertEquals(UploadExecutionStatus.Idle, resetByDryRunClear.executionStatus)
        assertNull(resetByDryRunClear.executionMessage)
        assertNull(resetByDryRunClear.executionRequestCount)
    }

    @Test
    fun localIntakeMapperBuildsAssetsWithPreviewMetadata() {
        val previewBytes = byteArrayOf(1, 2, 3)
        val assets = mapLocalIntakeFilesToAssets(
            listOf(
                LocalIntakeFile(
                    name = "photo.jpg",
                    type = "image/jpeg",
                    size = 123,
                    lastModifiedEpochMillis = 1_700_000_000_000,
                    previewUrl = "blob:preview-1",
                    previewBytes = previewBytes,
                    captureDateTime = "2026-01-02T03:04:05",
                    timeZone = "+01:00",
                    cameraMake = "Canon",
                    cameraModel = "R6",
                    exifMetadata = mapOf("iso" to "200")
                ),
                LocalIntakeFile(
                    name = "unknown.bin",
                    type = "",
                    size = 99,
                    lastModifiedEpochMillis = 1_700_000_000_100,
                    previewUrl = null
                )
            )
        )

        assertEquals(2, assets.size)
        assertEquals("photo.jpg", assets[0].fileName)
        assertEquals("image/jpeg", assets[0].mimeType)
        assertEquals("blob:preview-1", assets[0].previewUrl)
        assertEquals(previewBytes.toList(), assets[0].previewBytes?.toList())
        assertEquals("2026-01-02T03:04:05", assets[0].captureDateTime)
        assertEquals("+01:00", assets[0].timeZone)
        assertEquals("Canon", assets[0].cameraMake)
        assertEquals("R6", assets[0].cameraModel)
        assertEquals("200", assets[0].exifMetadata["iso"])
        assertEquals("application/octet-stream", assets[1].mimeType)
        assertNull(assets[1].previewUrl)
        assertTrue(assets[0].id.value.startsWith("local-photo.jpg-123-"))
    }

    @Test
    fun jpegExifParserExtractsDateTimeTimezoneAndCameraFields() {
        val jpegBytes = buildJpegWithExif(
            entries = listOf(
                exifAsciiEntry(MAKE_TAG, "Canon"),
                exifAsciiEntry(MODEL_TAG, "EOS R6"),
                exifLongEntry(EXIF_IFD_POINTER_TAG, 50)
            ),
            exifEntries = listOf(
                exifAsciiEntry(DATE_TIME_ORIGINAL_TAG, "2026:01:02 03:04:05"),
                exifAsciiEntry(OFFSET_TIME_ORIGINAL_TAG, "+0100"),
                exifShortEntry(ISO_SPEED_TAG, 320),
                exifRationalEntry(F_NUMBER_TAG, 28, 10)
            )
        )

        val parsed = parseJpegExifMetadata(jpegBytes)

        assertNotNull(parsed)
        assertEquals("2026-01-02T03:04:05", parsed.captureDateTime)
        assertEquals("+01:00", parsed.timeZone)
        assertEquals("Canon", parsed.cameraMake)
        assertEquals("EOS R6", parsed.cameraModel)
        assertEquals("320", parsed.metadata["iso"])
        assertEquals("2.8", parsed.metadata["fNumber"])
    }

    @Test
    fun jpegExifParserExtractsTimezoneFromDateTimeOriginalOffsetSuffix() {
        val jpegBytes = buildJpegWithExif(
            entries = listOf(
                exifAsciiEntry(MAKE_TAG, "Sony"),
                exifLongEntry(EXIF_IFD_POINTER_TAG, 50)
            ),
            exifEntries = listOf(
                exifAsciiEntry(DATE_TIME_ORIGINAL_TAG, "2026:01:02 03:04:05+0530")
            )
        )

        val parsed = parseJpegExifMetadata(jpegBytes)

        assertNotNull(parsed)
        assertEquals("2026-01-02T03:04:05", parsed.captureDateTime)
        assertEquals("+05:30", parsed.timeZone)
    }

    @Test
    fun jpegExifParserExtractsTimezoneFromTimeZoneOffsetTag() {
        val jpegBytes = buildJpegWithExif(
            entries = listOf(
                exifLongEntry(EXIF_IFD_POINTER_TAG, 50)
            ),
            exifEntries = listOf(
                exifAsciiEntry(DATE_TIME_ORIGINAL_TAG, "2026:01:02 03:04:05"),
                exifSignedShortEntry(TIME_ZONE_OFFSET_TAG, -5)
            )
        )

        val parsed = parseJpegExifMetadata(jpegBytes)

        assertNotNull(parsed)
        assertEquals("2026-01-02T03:04:05", parsed.captureDateTime)
        assertEquals("-05:00", parsed.timeZone)
    }

    @Test
    fun dryRunPlanComposesUploadMetadataTagAndAlbumRequestsForSelection() {
        val a = LocalAssetId("a")
        val b = LocalAssetId("b")
        val state = UploadPrepState(
            assets = mapOf(
                a to LocalAsset(a, "a.jpg", "image/jpeg", 1, null, null, null),
                b to LocalAsset(b, "b.jpg", "image/jpeg", 2, null, null, null)
            ),
            selectedAssetIds = setOf(a, b),
            stagedEditsByAssetId = mapOf(
                a to AssetEditPatch(
                    description = FieldPatch.Set("caption"),
                    isFavorite = FieldPatch.Set(true),
                    albumId = FieldPatch.Set("album-1"),
                    addTagIds = setOf("tag-1")
                )
            ),
            albumCreateDraft = "Family",
            tagCreateDraft = "Trip"
        )

        val plan = ImmichRequestBuilder.buildDryRunPlan(state)

        assertEquals(2, plan.uploadRequests.size)
        assertEquals(1, plan.bulkMetadataRequests.size)
        assertEquals(1, plan.tagAssignRequests.size)
        assertEquals(1, plan.albumAddRequests.size)
        assertEquals(4, plan.lookupHooks.size)
        assertEquals(listOf("remote-a"), plan.bulkMetadataRequests.first().ids)
        assertEquals(listOf("remote-a"), plan.tagAssignRequests.first().assetIds)
        assertEquals(listOf("remote-a"), plan.albumAddRequests.first().assetIds)
    }

    @Test
    fun dryRunPlanGroupsEqualPatchesIntoSingleBulkOperation() {
        val a = LocalAssetId("a")
        val b = LocalAssetId("b")
        val patchA = AssetEditPatch(description = FieldPatch.Set("shared"))
        val patchB = AssetEditPatch(description = FieldPatch.Set("shared"))
        val state = UploadPrepState(
            assets = mapOf(
                a to LocalAsset(a, "a.jpg", "image/jpeg", 1, null, null, null),
                b to LocalAsset(b, "b.jpg", "image/jpeg", 2, null, null, null)
            ),
            selectedAssetIds = setOf(a, b),
            stagedEditsByAssetId = mapOf(
                a to patchA,
                b to patchB
            )
        )

        val plan = ImmichRequestBuilder.buildDryRunPlan(state)

        assertEquals(1, plan.bulkMetadataRequests.size)
        assertEquals(listOf("remote-a", "remote-b"), plan.bulkMetadataRequests.first().ids)
    }

    @Test
    fun payloadInspectorBuildsEndpointsAndPayloadsFromPlan() {
        val state = UploadPrepState(
            assets = mapOf(
                LocalAssetId("a") to LocalAsset(
                    id = LocalAssetId("a"),
                    fileName = "a.jpg",
                    mimeType = "image/jpeg",
                    fileSizeBytes = 1,
                    previewUrl = null,
                    captureDateTime = "2026-02-01T00:00:00Z",
                    timeZone = null
                )
            ),
            selectedAssetIds = setOf(LocalAssetId("a")),
            stagedEditsByAssetId = mapOf(
                LocalAssetId("a") to AssetEditPatch(
                    description = FieldPatch.Set("caption"),
                    albumId = FieldPatch.Set("album-1"),
                    addTagIds = setOf("tag-1")
                )
            )
        )

        val plan = ImmichRequestBuilder.buildDryRunPlan(state)
        val requests = ImmichRequestBuilder.buildPayloadInspectorRequests(plan)

        assertTrue(requests.any { it.method == "POST" && it.url == "$IMMICH_API_BASE_URL/assets" })
        assertTrue(requests.any { it.method == "PUT" && it.url == "$IMMICH_API_BASE_URL/assets/updateAssets" })
        assertTrue(requests.any { it.method == "PUT" && it.url == "$IMMICH_API_BASE_URL/tags/assets" })
        assertTrue(requests.any { it.method == "PUT" && it.url == "$IMMICH_API_BASE_URL/albums/assets" })
        assertTrue(requests.any { it.body?.contains("\"assetData\":\"<binary:") == true })
        assertTrue(requests.any { it.body?.contains("\"description\":\"caption\"") == true })
    }

    @Test
    fun payloadInspectorEscapesJsonForLookupAndPayloadBodies() {
        val requestPlan = ImmichRequestPlan(
            uploadRequests = listOf(
                ImmichUploadRequest(
                    localAssetId = "a",
                    deviceAssetId = "dev\"1",
                    deviceId = "device\\1",
                    fileCreatedAt = "2026-02-01T00:00:00Z",
                    fileModifiedAt = "2026-02-01T00:00:00Z",
                    metadata = mapOf("fileName" to "line\nbreak", "mimeType" to "image/jpeg")
                )
            ),
            bulkMetadataRequests = listOf(
                ImmichBulkMetadataRequest(
                    ids = listOf("remote-1"),
                    description = "quote\"here"
                )
            ),
            lookupHooks = listOf(
                ImmichLookupHook.CreateAlbumIfMissing("Fam\"ily"),
                ImmichLookupHook.CreateTagIfMissing("Tri\\p")
            )
        )

        val requests = ImmichRequestBuilder.buildPayloadInspectorRequests(requestPlan)
        val createAlbum = requests.first { it.url == "$IMMICH_API_BASE_URL/albums" }
        val createTag = requests.first { it.url == "$IMMICH_API_BASE_URL/tags" }
        val upload = requests.first { it.url == "$IMMICH_API_BASE_URL/assets" }
        val metadata = requests.first { it.url == "$IMMICH_API_BASE_URL/assets/updateAssets" }
        val uploadBody = assertNotNull(upload.body)
        val metadataBody = assertNotNull(metadata.body)

        assertEquals("""{"name":"Fam\"ily"}""", createAlbum.body)
        assertEquals("""{"name":"Tri\\p"}""", createTag.body)
        assertTrue(uploadBody.contains("\"deviceAssetId\":\"dev\\\"1\""))
        assertTrue(uploadBody.contains("\"deviceId\":\"device\\\\1\""))
        assertTrue(uploadBody.contains("\"fileName\":\"line\\nbreak\""))
        assertTrue(metadataBody.contains("\"description\":\"quote\\\"here\""))
    }
}

private data class ExifEntry(
    val tag: Int,
    val type: Int,
    val count: Int,
    val valueBytes: ByteArray
)

private fun exifAsciiEntry(tag: Int, value: String): ExifEntry {
    val bytes = (value + '\u0000').encodeToByteArray()
    return ExifEntry(tag = tag, type = 2, count = bytes.size, valueBytes = bytes)
}

private fun exifShortEntry(tag: Int, value: Int): ExifEntry =
    ExifEntry(
        tag = tag,
        type = 3,
        count = 1,
        valueBytes = byteArrayOf((value and 0xFF).toByte(), ((value shr 8) and 0xFF).toByte())
    )

private fun exifSignedShortEntry(tag: Int, value: Int): ExifEntry {
    val raw = value and 0xFFFF
    return ExifEntry(
        tag = tag,
        type = 8,
        count = 1,
        valueBytes = byteArrayOf((raw and 0xFF).toByte(), ((raw shr 8) and 0xFF).toByte())
    )
}

private fun exifLongEntry(tag: Int, value: Int): ExifEntry =
    ExifEntry(
        tag = tag,
        type = 4,
        count = 1,
        valueBytes = byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    )

private fun exifRationalEntry(tag: Int, numerator: Int, denominator: Int): ExifEntry =
    ExifEntry(
        tag = tag,
        type = 5,
        count = 1,
        valueBytes = byteArrayOf(
            (numerator and 0xFF).toByte(),
            ((numerator shr 8) and 0xFF).toByte(),
            ((numerator shr 16) and 0xFF).toByte(),
            ((numerator shr 24) and 0xFF).toByte(),
            (denominator and 0xFF).toByte(),
            ((denominator shr 8) and 0xFF).toByte(),
            ((denominator shr 16) and 0xFF).toByte(),
            ((denominator shr 24) and 0xFF).toByte()
        )
    )

private fun buildJpegWithExif(
    entries: List<ExifEntry>,
    exifEntries: List<ExifEntry>
): ByteArray {
    val tiff = buildLittleEndianTiff(entries, exifEntries)
    val app1Length = tiff.size + 8
    return byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(),
        0xFF.toByte(), 0xE1.toByte(),
        ((app1Length shr 8) and 0xFF).toByte(), (app1Length and 0xFF).toByte(),
        0x45, 0x78, 0x69, 0x66, 0x00, 0x00
    ) + tiff + byteArrayOf(0xFF.toByte(), 0xD9.toByte())
}

private fun buildLittleEndianTiff(entries: List<ExifEntry>, exifEntries: List<ExifEntry>): ByteArray {
    val ifd0Offset = 8
    val ifd0Size = 2 + entries.size * 12 + 4
    val exifIfdOffset = ifd0Offset + ifd0Size
    val exifIfdSize = 2 + exifEntries.size * 12 + 4
    val dataStart = exifIfdOffset + exifIfdSize

    val total = ByteArray(dataStart + entries.sumOf { paddedSize(it.valueBytes.size) } + exifEntries.sumOf { paddedSize(it.valueBytes.size) })
    total[0] = 0x49
    total[1] = 0x49
    total[2] = 0x2A
    total[3] = 0x00
    writeInt32LE(total, 4, ifd0Offset)

    var dataCursor = dataStart
    writeIfd(total, ifd0Offset, entries, dataCursor).also { dataCursor = it }
    writeIfd(total, exifIfdOffset, exifEntries, dataCursor)

    return total
}

private fun writeIfd(buffer: ByteArray, ifdOffset: Int, entries: List<ExifEntry>, dataCursorStart: Int): Int {
    writeInt16LE(buffer, ifdOffset, entries.size)
    var dataCursor = dataCursorStart
    entries.forEachIndexed { index, entry ->
        val entryOffset = ifdOffset + 2 + (index * 12)
        writeInt16LE(buffer, entryOffset, entry.tag)
        writeInt16LE(buffer, entryOffset + 2, entry.type)
        writeInt32LE(buffer, entryOffset + 4, entry.count)

        if (entry.valueBytes.size <= 4) {
            entry.valueBytes.forEachIndexed { byteIndex, value ->
                buffer[entryOffset + 8 + byteIndex] = value
            }
        } else {
            writeInt32LE(buffer, entryOffset + 8, dataCursor)
            entry.valueBytes.copyInto(buffer, destinationOffset = dataCursor)
            dataCursor += paddedSize(entry.valueBytes.size)
        }
    }
    writeInt32LE(buffer, ifdOffset + 2 + (entries.size * 12), 0)
    return dataCursor
}

private fun paddedSize(size: Int): Int = if (size % 2 == 0) size else size + 1

private fun writeInt16LE(buffer: ByteArray, offset: Int, value: Int) {
    buffer[offset] = (value and 0xFF).toByte()
    buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
}

private fun writeInt32LE(buffer: ByteArray, offset: Int, value: Int) {
    buffer[offset] = (value and 0xFF).toByte()
    buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
    buffer[offset + 2] = ((value shr 16) and 0xFF).toByte()
    buffer[offset + 3] = ((value shr 24) and 0xFF).toByte()
}

private const val MAKE_TAG = 0x010F
private const val MODEL_TAG = 0x0110
private const val EXIF_IFD_POINTER_TAG = 0x8769
private const val DATE_TIME_ORIGINAL_TAG = 0x9003
private const val OFFSET_TIME_ORIGINAL_TAG = 0x9011
private const val TIME_ZONE_OFFSET_TAG = 0x882A
private const val ISO_SPEED_TAG = 0x8827
private const val F_NUMBER_TAG = 0x829D
