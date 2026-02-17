package com.marcportabella.immichuploader

import com.marcportabella.immichuploader.data.IMMICH_API_BASE_URL
import com.marcportabella.immichuploader.data.ImmichAlbumCreateBody
import com.marcportabella.immichuploader.data.ImmichApiRequest
import com.marcportabella.immichuploader.data.ImmichBulkMetadataBody
import com.marcportabella.immichuploader.data.ImmichBulkMetadataRequest
import com.marcportabella.immichuploader.data.ImmichLookupHook
import com.marcportabella.immichuploader.data.ImmichRequestBuilder
import com.marcportabella.immichuploader.data.ImmichRequestPlan
import com.marcportabella.immichuploader.data.ImmichTagCreateBody
import com.marcportabella.immichuploader.data.ImmichUploadRequest
import com.marcportabella.immichuploader.data.ImmichUploadBody
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadPrepState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UploadRequestPlannerSliceWebTest {

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
        assertTrue(
            requests.any {
                val payload = (it.body as? ImmichUploadBody)?.payload
                payload?.assetData?.startsWith("<binary:") == true
            }
        )
        assertTrue(
            requests.any {
                val payload = (it.body as? ImmichBulkMetadataBody)?.payload
                payload?.description == "caption"
            }
        )
    }

    @Test
    fun payloadInspectorIncludesTimezoneInBulkMetadataPayload() {
        val id = LocalAssetId("a")
        val state = UploadPrepState(
            assets = mapOf(
                id to LocalAsset(id, "a.jpg", "image/jpeg", 1, null, null, null)
            ),
            selectedAssetIds = setOf(id),
            stagedEditsByAssetId = mapOf(
                id to AssetEditPatch(
                    timeZone = FieldPatch.Set("+02:00")
                )
            )
        )

        val plan = ImmichRequestBuilder.buildDryRunPlan(state)
        val requests = ImmichRequestBuilder.buildPayloadInspectorRequests(plan)
        val metadataRequest = requests.first { it.url == "$IMMICH_API_BASE_URL/assets/updateAssets" }
        val body = (metadataRequest.body as? ImmichBulkMetadataBody)?.payload
        assertNotNull(body)

        assertEquals("+02:00", body.timeZone)
    }

    @Test
    fun payloadInspectorPreservesLookupAndPayloadValues() {
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
        val createAlbumBody = (createAlbum.body as? ImmichAlbumCreateBody)?.payload
        val createTagBody = (createTag.body as? ImmichTagCreateBody)?.payload
        val uploadBody = (upload.body as? ImmichUploadBody)?.payload
        val metadataBody = (metadata.body as? ImmichBulkMetadataBody)?.payload

        assertNotNull(createAlbumBody)
        assertNotNull(createTagBody)
        assertNotNull(uploadBody)
        assertNotNull(metadataBody)
        assertEquals("Fam\"ily", createAlbumBody.name)
        assertEquals("Tri\\p", createTagBody.name)
        assertEquals("dev\"1", uploadBody.deviceAssetId)
        assertEquals("device\\1", uploadBody.deviceId)
        assertEquals("line\nbreak", uploadBody.metadata["fileName"])
        assertEquals("quote\"here", metadataBody.description)
    }

    @Test
    fun catalogRequestBuilderUsesFixedBaseUrlContracts() {
        val albumsLookup = com.marcportabella.immichuploader.data.ImmichCatalogRequestBuilder.lookupAlbums()
        val tagsLookup = com.marcportabella.immichuploader.data.ImmichCatalogRequestBuilder.lookupTags()
        val createAlbum = com.marcportabella.immichuploader.data.ImmichCatalogRequestBuilder.createAlbum("Family")
        val createTag = com.marcportabella.immichuploader.data.ImmichCatalogRequestBuilder.createTag("Trip")

        assertEquals("GET", albumsLookup.method)
        assertEquals("$IMMICH_API_BASE_URL/albums", albumsLookup.url)
        assertNull(albumsLookup.body)

        assertEquals("GET", tagsLookup.method)
        assertEquals("$IMMICH_API_BASE_URL/tags", tagsLookup.url)
        assertNull(tagsLookup.body)

        assertEquals("POST", createAlbum.method)
        assertEquals("$IMMICH_API_BASE_URL/albums", createAlbum.url)
        val createAlbumBody = (createAlbum.body as? ImmichAlbumCreateBody)?.payload
        assertNotNull(createAlbumBody)
        assertEquals("Family", createAlbumBody.name)

        assertEquals("POST", createTag.method)
        assertEquals("$IMMICH_API_BASE_URL/tags", createTag.url)
        val createTagBody = (createTag.body as? ImmichTagCreateBody)?.payload
        assertNotNull(createTagBody)
        assertEquals("Trip", createTagBody.name)
    }

    @Test
    fun payloadInspectorResultShapeIsStable() {
        val request = ImmichApiRequest("GET", "$IMMICH_API_BASE_URL/albums", null)
        assertEquals("GET", request.method)
        assertEquals("$IMMICH_API_BASE_URL/albums", request.url)
        assertNull(request.body)
    }
}
