package com.marcportabella.immichuploader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComposeAppWebTest {

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
    fun localIntakeMapperBuildsAssetsWithPreviewMetadata() {
        val assets = mapLocalIntakeFilesToAssets(
            listOf(
                LocalIntakeFile(
                    name = "photo.jpg",
                    type = "image/jpeg",
                    size = 123,
                    lastModifiedEpochMillis = 1_700_000_000_000,
                    previewUrl = "blob:preview-1"
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
        assertEquals("application/octet-stream", assets[1].mimeType)
        assertNull(assets[1].previewUrl)
        assertTrue(assets[0].id.value.startsWith("local-photo.jpg-123-"))
    }
}
