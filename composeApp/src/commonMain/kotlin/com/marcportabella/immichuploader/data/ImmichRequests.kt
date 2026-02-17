package com.marcportabella.immichuploader.data

import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.UploadPrepState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val IMMICH_API_BASE_URL: String = "https://fotos.marcportabella.com/api"
internal val immichJson: Json = Json {
    encodeDefaults = false
    explicitNulls = false
    ignoreUnknownKeys = true
}

data class ImmichUploadRequest(
    val localAssetId: String,
    val deviceAssetId: String,
    val deviceId: String,
    val fileCreatedAt: String,
    val fileModifiedAt: String,
    val metadata: Map<String, String>
)

@Serializable
data class ImmichBulkMetadataRequest(
    val ids: List<String>,
    val dateTimeOriginal: String? = null,
    val timeZone: String? = null,
    val description: String? = null,
    val isFavorite: Boolean? = null
)

@Serializable
data class ImmichTagAssignRequest(
    val assetIds: List<String>,
    val tagIds: List<String>
)

@Serializable
data class ImmichAlbumAddRequest(
    val albumId: String,
    val assetIds: List<String>
)

@Serializable
data class ImmichAlbumCreateRequest(val name: String)

@Serializable
data class ImmichTagCreateRequest(val name: String)

@Serializable
data class ImmichBulkUploadCheckItem(
    val id: String,
    val checksum: String,
    val originalPath: String? = null,
    val relativePath: String? = null
)

@Serializable
data class ImmichBulkUploadCheckRequest(
    val assets: List<ImmichBulkUploadCheckItem>
)

sealed interface ImmichApiBody

data class ImmichUploadBody(val payload: ImmichUploadPayload) : ImmichApiBody

data class ImmichBulkMetadataBody(val payload: ImmichBulkMetadataRequest) : ImmichApiBody

data class ImmichTagAssignBody(val payload: ImmichTagAssignRequest) : ImmichApiBody

data class ImmichAlbumAddBody(val payload: ImmichAlbumAddRequest) : ImmichApiBody

data class ImmichAlbumCreateBody(val payload: ImmichAlbumCreateRequest) : ImmichApiBody

data class ImmichTagCreateBody(val payload: ImmichTagCreateRequest) : ImmichApiBody

data class ImmichBulkUploadCheckBody(val payload: ImmichBulkUploadCheckRequest) : ImmichApiBody

data class ImmichApiRequest(
    val method: String,
    val url: String,
    val body: ImmichApiBody? = null
)

object ImmichCatalogRequestBuilder {
    fun lookupAlbums(): ImmichApiRequest =
        ImmichApiRequest(
            method = "GET",
            url = "$IMMICH_API_BASE_URL/albums"
        )

    fun lookupTags(): ImmichApiRequest =
        ImmichApiRequest(
            method = "GET",
            url = "$IMMICH_API_BASE_URL/tags"
        )

    fun createAlbum(name: String): ImmichApiRequest =
        ImmichApiRequest(
            method = "POST",
            url = "$IMMICH_API_BASE_URL/albums",
            body = ImmichAlbumCreateBody(ImmichAlbumCreateRequest(name.trim()))
        )

    fun createTag(name: String): ImmichApiRequest =
        ImmichApiRequest(
            method = "POST",
            url = "$IMMICH_API_BASE_URL/tags",
            body = ImmichTagCreateBody(ImmichTagCreateRequest(name.trim()))
        )

    fun bulkUploadCheck(items: List<ImmichBulkUploadCheckItem>): ImmichApiRequest =
        ImmichApiRequest(
            method = "POST",
            url = "$IMMICH_API_BASE_URL/assets/bulk-upload-check",
            body = ImmichBulkUploadCheckBody(
                ImmichBulkUploadCheckRequest(
                    assets = items.distinctBy { it.id }.sortedBy { it.id }
                )
            )
        )
}

sealed interface ImmichLookupHook {
    data object LookupAlbums : ImmichLookupHook
    data object LookupTags : ImmichLookupHook
    data class CreateAlbumIfMissing(val name: String) : ImmichLookupHook
    data class CreateTagIfMissing(val name: String) : ImmichLookupHook
}

data class ImmichRequestPlan(
    val uploadRequests: List<ImmichUploadRequest> = emptyList(),
    val bulkMetadataRequests: List<ImmichBulkMetadataRequest> = emptyList(),
    val tagAssignRequests: List<ImmichTagAssignRequest> = emptyList(),
    val albumAddRequests: List<ImmichAlbumAddRequest> = emptyList(),
    val lookupHooks: List<ImmichLookupHook> = emptyList(),
    val sessionTagsById: Map<String, String> = emptyMap()
)

object ImmichRequestBuilder {
    private const val FALLBACK_TIMESTAMP = "1970-01-01T00:00:00Z"
    private const val DEFAULT_DEVICE_ID = "web-local-device"

    fun buildUploadRequest(asset: LocalAsset, deviceId: String): ImmichUploadRequest {
        val timestamp = asset.captureDateTime ?: FALLBACK_TIMESTAMP
        return ImmichUploadRequest(
            localAssetId = asset.id.value,
            deviceAssetId = asset.id.value,
            deviceId = deviceId,
            fileCreatedAt = timestamp,
            fileModifiedAt = timestamp,
            metadata = mapOf(
                "fileName" to asset.fileName,
                "mimeType" to asset.mimeType
            )
        )
    }

    fun buildBulkMetadataRequest(assetIds: Set<String>, patch: AssetEditPatch): ImmichBulkMetadataRequest? {
        if (assetIds.isEmpty()) return null

        val description = (patch.description as? FieldPatch.Set<String?>)?.value
        val isFavorite = (patch.isFavorite as? FieldPatch.Set<Boolean>)?.value
        val dateTimeOriginal = (patch.dateTimeOriginal as? FieldPatch.Set<String>)?.value
        val timeZone = (patch.timeZone as? FieldPatch.Set<String>)?.value

        if (description == null && isFavorite == null && dateTimeOriginal == null && timeZone == null) {
            return null
        }

        return ImmichBulkMetadataRequest(
            ids = assetIds.toList().sorted(),
            dateTimeOriginal = dateTimeOriginal,
            timeZone = timeZone,
            description = description,
            isFavorite = isFavorite
        )
    }

    fun buildTagAssignRequest(assetIds: Set<String>, patch: AssetEditPatch): ImmichTagAssignRequest? {
        if (assetIds.isEmpty() || patch.addTagIds.isEmpty()) return null
        return ImmichTagAssignRequest(
            assetIds = assetIds.toList().sorted(),
            tagIds = patch.addTagIds.toList().sorted()
        )
    }

    fun buildAlbumAddRequest(assetIds: Set<String>, patch: AssetEditPatch): ImmichAlbumAddRequest? {
        if (assetIds.isEmpty()) return null
        val albumId = (patch.albumId as? FieldPatch.Set<String?>)?.value ?: return null
        if (albumId.isBlank()) return null

        return ImmichAlbumAddRequest(
            albumId = albumId,
            assetIds = assetIds.toList().sorted()
        )
    }

    fun buildLookupHooks(
        shouldLookupAlbums: Boolean,
        shouldLookupTags: Boolean,
        albumsToCreate: Set<String>,
        tagsToCreate: Set<String>
    ): List<ImmichLookupHook> {
        val hooks = mutableListOf<ImmichLookupHook>()
        if (shouldLookupAlbums) hooks += ImmichLookupHook.LookupAlbums
        if (shouldLookupTags) hooks += ImmichLookupHook.LookupTags
        albumsToCreate.map { it.trim() }.filter { it.isNotEmpty() }.sorted().forEach {
            hooks += ImmichLookupHook.CreateAlbumIfMissing(it)
        }
        tagsToCreate.map { it.trim() }.filter { it.isNotEmpty() }.sorted().forEach {
            hooks += ImmichLookupHook.CreateTagIfMissing(it)
        }
        return hooks
    }

    fun buildDryRunPlan(
        state: UploadPrepState,
        deviceId: String = DEFAULT_DEVICE_ID
    ): ImmichRequestPlan {
        val selectedIds = state.selectedAssetIds.toList().sortedBy { it.value }
        val selectedAssets = selectedIds.mapNotNull { state.assets[it] }

        val uploadRequests = selectedAssets.map { asset ->
            buildUploadRequest(asset, deviceId)
        }

        val remoteIdsByPatch = linkedMapOf<AssetEditPatch, MutableSet<String>>()
        selectedIds.forEach { assetId ->
            val patch = state.stagedEditsByAssetId[assetId] ?: return@forEach
            val remoteIds = remoteIdsByPatch.getOrPut(patch) { linkedSetOf() }
            remoteIds += "remote-${assetId.value}"
        }

        val bulkMetadataRequests = mutableListOf<ImmichBulkMetadataRequest>()
        val tagAssignRequests = mutableListOf<ImmichTagAssignRequest>()
        val albumAddRequests = mutableListOf<ImmichAlbumAddRequest>()

        remoteIdsByPatch.forEach { (patch, remoteIds) ->
            buildBulkMetadataRequest(remoteIds, patch)?.let { bulkMetadataRequests += it }
            buildTagAssignRequest(remoteIds, patch)?.let { tagAssignRequests += it }
            buildAlbumAddRequest(remoteIds, patch)?.let { albumAddRequests += it }
        }

        val lookupHooks = buildLookupHooks(
            shouldLookupAlbums = state.availableAlbums.isEmpty(),
            shouldLookupTags = state.availableTags.isEmpty(),
            albumsToCreate = setOf(state.albumCreateDraft),
            tagsToCreate = collectSessionTagNamesForSelection(state) + setOf(state.tagCreateDraft)
        )

        return ImmichRequestPlan(
            uploadRequests = uploadRequests,
            bulkMetadataRequests = bulkMetadataRequests,
            tagAssignRequests = tagAssignRequests,
            albumAddRequests = albumAddRequests,
            lookupHooks = lookupHooks,
            sessionTagsById = state.sessionTagsById
        )
    }

    fun buildPayloadInspectorRequests(plan: ImmichRequestPlan): List<ImmichApiRequest> {
        val requests = mutableListOf<ImmichApiRequest>()

        plan.lookupHooks.forEach { hook ->
            requests += when (hook) {
                ImmichLookupHook.LookupAlbums -> ImmichCatalogRequestBuilder.lookupAlbums()
                ImmichLookupHook.LookupTags -> ImmichCatalogRequestBuilder.lookupTags()
                is ImmichLookupHook.CreateAlbumIfMissing -> ImmichCatalogRequestBuilder.createAlbum(hook.name)
                is ImmichLookupHook.CreateTagIfMissing -> ImmichCatalogRequestBuilder.createTag(hook.name)
            }
        }

        plan.uploadRequests.forEach { request ->
            requests += ImmichApiRequest(
                method = "POST",
                url = "$IMMICH_API_BASE_URL/assets",
                body = request.toApiBody()
            )
        }

        plan.bulkMetadataRequests.forEach { request ->
            requests += ImmichApiRequest(
                method = "PUT",
                url = "$IMMICH_API_BASE_URL/assets/updateAssets",
                body = request.toApiBody()
            )
        }

        plan.tagAssignRequests.forEach { request ->
            requests += ImmichApiRequest(
                method = "PUT",
                url = "$IMMICH_API_BASE_URL/tags/assets",
                body = request.toApiBody()
            )
        }

        plan.albumAddRequests.forEach { request ->
            requests += ImmichApiRequest(
                method = "PUT",
                url = "$IMMICH_API_BASE_URL/albums/assets",
                body = request.toApiBody()
            )
        }

        return requests
    }

    private fun collectSessionTagNamesForSelection(state: UploadPrepState): Set<String> {
        if (state.selectedAssetIds.isEmpty() || state.sessionTagsById.isEmpty()) return emptySet()
        return state.selectedAssetIds
            .mapNotNull { state.stagedEditsByAssetId[it] }
            .flatMap { patch -> patch.addTagIds }
            .mapNotNull { sessionTagId -> state.sessionTagsById[sessionTagId] }
            .toSet()
    }
}

private fun ImmichUploadRequest.toApiBody(): ImmichUploadBody =
    ImmichUploadBody(
        payload = ImmichUploadPayload(
            assetData = "<binary:$localAssetId>",
            deviceAssetId = deviceAssetId,
            deviceId = deviceId,
            fileCreatedAt = fileCreatedAt,
            fileModifiedAt = fileModifiedAt,
            metadata = metadata
        )
    )

private fun ImmichBulkMetadataRequest.toApiBody(): ImmichBulkMetadataBody =
    ImmichBulkMetadataBody(payload = copy(ids = ids.sorted()))

private fun ImmichTagAssignRequest.toApiBody(): ImmichTagAssignBody =
    ImmichTagAssignBody(payload = copy(assetIds = assetIds.sorted(), tagIds = tagIds.sorted()))

private fun ImmichAlbumAddRequest.toApiBody(): ImmichAlbumAddBody =
    ImmichAlbumAddBody(payload = copy(assetIds = assetIds.sorted()))

fun parseExistingAssetsByItemId(
    responseBody: String,
    requestedItems: List<ImmichBulkUploadCheckItem>
): Map<String, String> {
    val root = runCatching { immichJson.parseToJsonElement(responseBody) }.getOrNull() ?: return emptyMap()

    val existingAssets = (root as? JsonObject)
        ?.get("existingAssets")
        ?.jsonObject
        ?.mapNotNull { (checksum, value) ->
            val assetId = value.jsonPrimitive.contentOrNull?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            checksum to assetId
        }
        ?.toMap()
    if (!existingAssets.isNullOrEmpty()) {
        val idByChecksum = requestedItems.associate { it.checksum to it.id }
        return existingAssets.mapNotNull { (checksum, assetId) ->
            val itemId = idByChecksum[checksum] ?: return@mapNotNull null
            itemId to assetId
        }.toMap()
    }

    val results = (root as? JsonObject)?.get("results") as? JsonArray ?: return emptyMap()
    return results.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val assetId = obj["assetId"]?.jsonPrimitive?.contentOrNull
            ?: return@mapNotNull null
        id to assetId
    }.toMap()
}

@Serializable
data class ImmichUploadPayload(
    val assetData: String,
    val deviceAssetId: String,
    val deviceId: String,
    val fileCreatedAt: String,
    val fileModifiedAt: String,
    val metadata: Map<String, String>
)
