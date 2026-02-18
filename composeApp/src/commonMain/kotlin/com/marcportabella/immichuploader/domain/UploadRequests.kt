package com.marcportabella.immichuploader.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val IMMICH_API_BASE_URL: String = "https://fotos.marcportabella.com/api"
private const val FALLBACK_TIMESTAMP = "1970-01-01T00:00:00Z"
private const val DEFAULT_DEVICE_ID = "web-local-device"

private val uploadRequestJson: Json = Json {
    encodeDefaults = false
    explicitNulls = false
    ignoreUnknownKeys = true
}

@Serializable
data class UploadCatalogEntry(
    val id: String,
    val name: String
)

@Serializable
data class UploadApiRequest(
    val method: String,
    val url: String,
    val body: String? = null
)

@Serializable
data class UploadUploadRequest(
    val localAssetId: String,
    val deviceAssetId: String,
    val deviceId: String,
    val fileCreatedAt: String,
    val fileModifiedAt: String,
    val metadata: Map<String, String>
)

@Serializable
data class UploadBulkMetadataRequest(
    val ids: List<String>,
    val dateTimeOriginal: String? = null,
    val timeZone: String? = null,
    val description: String? = null,
    val isFavorite: Boolean? = null
)

@Serializable
data class UploadTagAssignRequest(
    val assetIds: List<String>,
    val tagIds: List<String>
)

@Serializable
data class UploadAlbumAddRequest(
    val albumId: String,
    val assetIds: List<String>
)

sealed interface UploadLookupHook {
    data object LookupAlbums : UploadLookupHook
    data object LookupTags : UploadLookupHook
    data class CreateAlbumIfMissing(val name: String) : UploadLookupHook
    data class CreateTagIfMissing(val name: String) : UploadLookupHook
}

data class UploadRequestPlan(
    val uploadRequests: List<UploadUploadRequest> = emptyList(),
    val bulkMetadataRequests: List<UploadBulkMetadataRequest> = emptyList(),
    val tagAssignRequests: List<UploadTagAssignRequest> = emptyList(),
    val albumAddRequests: List<UploadAlbumAddRequest> = emptyList(),
    val lookupHooks: List<UploadLookupHook> = emptyList(),
    val sessionTagsById: Map<String, String> = emptyMap()
)

object UploadRequestPlanner {
    fun buildDryRunPlan(
        state: UploadPrepState,
        deviceId: String = DEFAULT_DEVICE_ID
    ): UploadRequestPlan {
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

        val bulkMetadataRequests = mutableListOf<UploadBulkMetadataRequest>()
        val tagAssignRequests = mutableListOf<UploadTagAssignRequest>()
        val albumAddRequests = mutableListOf<UploadAlbumAddRequest>()

        remoteIdsByPatch.forEach { (patch, remoteIds) ->
            buildBulkMetadataRequest(remoteIds, patch)?.let { bulkMetadataRequests += it }
            buildTagAssignRequest(remoteIds, patch)?.let { tagAssignRequests += it }
            buildAlbumAddRequest(remoteIds, patch)?.let { albumAddRequests += it }
        }

        val lookupHooks = buildLookupHooks(
            albumsToCreate = setOf(state.albumCreateDraft),
            tagsToCreate = collectSessionTagNamesForSelection(state) + setOf(state.tagCreateDraft)
        )

        return UploadRequestPlan(
            uploadRequests = uploadRequests,
            bulkMetadataRequests = bulkMetadataRequests,
            tagAssignRequests = tagAssignRequests,
            albumAddRequests = albumAddRequests,
            lookupHooks = lookupHooks,
            sessionTagsById = state.sessionTagsById
        )
    }

    fun buildPayloadInspectorRequests(plan: UploadRequestPlan): List<UploadApiRequest> {
        val requests = mutableListOf<UploadApiRequest>()

        plan.lookupHooks.forEach { hook ->
            requests += when (hook) {
                UploadLookupHook.LookupAlbums ->
                    UploadApiRequest(method = "GET", url = "$IMMICH_API_BASE_URL/albums")

                UploadLookupHook.LookupTags ->
                    UploadApiRequest(method = "GET", url = "$IMMICH_API_BASE_URL/tags")

                is UploadLookupHook.CreateAlbumIfMissing ->
                    UploadApiRequest(
                        method = "POST",
                        url = "$IMMICH_API_BASE_URL/albums",
                        body = uploadRequestJson.encodeToString(UploadNameRequest(hook.name.trim()))
                    )

                is UploadLookupHook.CreateTagIfMissing ->
                    UploadApiRequest(
                        method = "POST",
                        url = "$IMMICH_API_BASE_URL/tags",
                        body = uploadRequestJson.encodeToString(UploadNameRequest(hook.name.trim()))
                    )
            }
        }

        plan.uploadRequests.forEach { request ->
            requests += UploadApiRequest(
                method = "POST",
                url = "$IMMICH_API_BASE_URL/assets",
                body = request.toPayloadJson()
            )
        }

        plan.bulkMetadataRequests.forEach { request ->
            val requestsToSend = listOf(request.copy(ids = request.ids.sorted()))
            requestsToSend.forEach { requestItem ->
                requests += UploadApiRequest(
                    method = "PUT",
                    url = "$IMMICH_API_BASE_URL/assets",
                    body = requestItem.toPayloadJson()
                )
            }
        }

        plan.tagAssignRequests.forEach { request ->
            request.tagIds.sorted().forEach { tagId ->
                requests += UploadApiRequest(
                    method = "PUT",
                    url = "$IMMICH_API_BASE_URL/tags/$tagId/assets",
                    body = uploadRequestJson.encodeToString(UploadTagAssetsPayload(request.assetIds.sorted()))
                )
            }
        }

        plan.albumAddRequests.forEach { request ->
            requests += UploadApiRequest(
                method = "PUT",
                url = "$IMMICH_API_BASE_URL/albums/assets",
                body = request.toPayloadJson()
            )
        }

        return requests
    }

    private fun buildUploadRequest(asset: LocalAsset, deviceId: String): UploadUploadRequest {
        val timestamp = asset.captureDateTime ?: FALLBACK_TIMESTAMP
        return UploadUploadRequest(
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

    private fun buildBulkMetadataRequest(assetIds: Set<String>, patch: AssetEditPatch): UploadBulkMetadataRequest? {
        if (assetIds.isEmpty()) return null

        val description = (patch.description as? FieldPatch.Set<String?>)?.value
        val isFavorite = (patch.isFavorite as? FieldPatch.Set<Boolean>)?.value
        val dateTimeOriginal = (patch.dateTimeOriginal as? FieldPatch.Set<String>)?.value
        val timeZone = (patch.timeZone as? FieldPatch.Set<String>)?.value

        if (description == null && isFavorite == null && dateTimeOriginal == null && timeZone == null) {
            return null
        }

        return UploadBulkMetadataRequest(
            ids = assetIds.toList().sorted(),
            dateTimeOriginal = dateTimeOriginal,
            timeZone = timeZone,
            description = description,
            isFavorite = isFavorite
        )
    }

    private fun buildTagAssignRequest(assetIds: Set<String>, patch: AssetEditPatch): UploadTagAssignRequest? {
        if (assetIds.isEmpty() || patch.addTagIds.isEmpty()) return null
        return UploadTagAssignRequest(
            assetIds = assetIds.toList().sorted(),
            tagIds = patch.addTagIds.toList().sorted()
        )
    }

    private fun buildAlbumAddRequest(assetIds: Set<String>, patch: AssetEditPatch): UploadAlbumAddRequest? {
        if (assetIds.isEmpty()) return null
        val albumId = (patch.albumId as? FieldPatch.Set<String?>)?.value ?: return null
        if (albumId.isBlank()) return null

        return UploadAlbumAddRequest(
            albumId = albumId,
            assetIds = assetIds.toList().sorted()
        )
    }

    private fun buildLookupHooks(
        albumsToCreate: Set<String>,
        tagsToCreate: Set<String>
    ): List<UploadLookupHook> {
        val hooks = mutableListOf<UploadLookupHook>()
        albumsToCreate.map { it.trim() }.filter { it.isNotEmpty() }.sorted().forEach {
            hooks += UploadLookupHook.CreateAlbumIfMissing(it)
        }
        tagsToCreate.map { it.trim() }.filter { it.isNotEmpty() }.sorted().forEach {
            hooks += UploadLookupHook.CreateTagIfMissing(it)
        }
        return hooks
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

private fun UploadUploadRequest.toPayloadJson(): String {
    val payload = UploadAssetPayload(
        assetData = "<binary:$localAssetId>",
        deviceAssetId = deviceAssetId,
        deviceId = deviceId,
        fileCreatedAt = fileCreatedAt,
        fileModifiedAt = fileModifiedAt,
        metadata = uploadRequestJson.encodeToString(metadata)
    )
    return uploadRequestJson.encodeToString(payload)
}

private fun UploadBulkMetadataRequest.toPayloadJson(): String =
    uploadRequestJson.encodeToString(copy(ids = ids.sorted()))

private fun UploadTagAssignRequest.toPayloadJson(): String =
    uploadRequestJson.encodeToString(copy(assetIds = assetIds.sorted(), tagIds = tagIds.sorted()))

private fun UploadAlbumAddRequest.toPayloadJson(): String =
    uploadRequestJson.encodeToString(copy(assetIds = assetIds.sorted()))

@Serializable
private data class UploadAssetPayload(
    val assetData: String,
    val deviceAssetId: String,
    val deviceId: String,
    val fileCreatedAt: String,
    val fileModifiedAt: String,
    val metadata: String
)

@Serializable
private data class UploadNameRequest(val name: String)

@Serializable
private data class UploadTagAssetsPayload(val ids: List<String>)
