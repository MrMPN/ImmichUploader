package com.marcportabella.immichuploader.data

import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.UploadPrepState

const val IMMICH_API_BASE_URL: String = "https://fotos.marcportabella.com/api"

data class ImmichUploadRequest(
    val localAssetId: String,
    val deviceAssetId: String,
    val deviceId: String,
    val fileCreatedAt: String,
    val fileModifiedAt: String,
    val metadata: Map<String, String>
)

data class ImmichBulkMetadataRequest(
    val ids: List<String>,
    val dateTimeOriginal: String? = null,
    val timeZone: String? = null,
    val description: String? = null,
    val isFavorite: Boolean? = null
)

data class ImmichTagAssignRequest(
    val assetIds: List<String>,
    val tagIds: List<String>
)

data class ImmichAlbumAddRequest(
    val albumId: String,
    val assetIds: List<String>
)

data class ImmichAlbumCreateRequest(val name: String)

data class ImmichTagCreateRequest(val name: String)

data class ImmichApiRequest(
    val method: String,
    val url: String,
    val body: String? = null
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
            body = """{"name":"${name.trim().escapeJson()}"}"""
        )

    fun createTag(name: String): ImmichApiRequest =
        ImmichApiRequest(
            method = "POST",
            url = "$IMMICH_API_BASE_URL/tags",
            body = """{"name":"${name.trim().escapeJson()}"}"""
        )
}

sealed interface ImmichLookupHook {
    data object LookupAlbums : ImmichLookupHook
    data object LookupTags : ImmichLookupHook
    data class CreateAlbumIfMissing(val name: String) : ImmichLookupHook
    data class CreateTagIfMissing(val name: String) : ImmichLookupHook
}

private fun String.escapeJson(): String =
    buildString(length) {
        for (char in this@escapeJson) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }

data class ImmichRequestPlan(
    val uploadRequests: List<ImmichUploadRequest> = emptyList(),
    val bulkMetadataRequests: List<ImmichBulkMetadataRequest> = emptyList(),
    val tagAssignRequests: List<ImmichTagAssignRequest> = emptyList(),
    val albumAddRequests: List<ImmichAlbumAddRequest> = emptyList(),
    val lookupHooks: List<ImmichLookupHook> = emptyList()
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
            tagsToCreate = setOf(state.tagCreateDraft)
        )

        return ImmichRequestPlan(
            uploadRequests = uploadRequests,
            bulkMetadataRequests = bulkMetadataRequests,
            tagAssignRequests = tagAssignRequests,
            albumAddRequests = albumAddRequests,
            lookupHooks = lookupHooks
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
                body = request.toPayloadJson()
            )
        }

        plan.bulkMetadataRequests.forEach { request ->
            requests += ImmichApiRequest(
                method = "PUT",
                url = "$IMMICH_API_BASE_URL/assets/updateAssets",
                body = request.toPayloadJson()
            )
        }

        plan.tagAssignRequests.forEach { request ->
            requests += ImmichApiRequest(
                method = "PUT",
                url = "$IMMICH_API_BASE_URL/tags/assets",
                body = request.toPayloadJson()
            )
        }

        plan.albumAddRequests.forEach { request ->
            requests += ImmichApiRequest(
                method = "PUT",
                url = "$IMMICH_API_BASE_URL/albums/assets",
                body = request.toPayloadJson()
            )
        }

        return requests
    }
}

private fun ImmichUploadRequest.toPayloadJson(): String {
    val metadataJson = metadata.entries
        .sortedBy { it.key }
        .joinToString(",") { (key, value) -> """"${key.escapeJson()}":"${value.escapeJson()}"""" }
    return """{"assetData":"<binary:$localAssetId>","deviceAssetId":"${deviceAssetId.escapeJson()}","deviceId":"${deviceId.escapeJson()}","fileCreatedAt":"${fileCreatedAt.escapeJson()}","fileModifiedAt":"${fileModifiedAt.escapeJson()}","metadata":{$metadataJson}}"""
}

private fun ImmichBulkMetadataRequest.toPayloadJson(): String {
    val fields = mutableListOf<String>()
    fields += """"ids":[${ids.sorted().joinToString(",") { """"${it.escapeJson()}"""" }}]"""
    dateTimeOriginal?.let { fields += """"dateTimeOriginal":"${it.escapeJson()}"""" }
    timeZone?.let { fields += """"timeZone":"${it.escapeJson()}"""" }
    description?.let { fields += """"description":"${it.escapeJson()}"""" }
    isFavorite?.let { fields += """"isFavorite":$it""" }
    return "{${fields.joinToString(",")}}"
}

private fun ImmichTagAssignRequest.toPayloadJson(): String =
    """{"assetIds":[${assetIds.sorted().joinToString(",") { """"${it.escapeJson()}"""" }}],"tagIds":[${tagIds.sorted().joinToString(",") { """"${it.escapeJson()}"""" }}]}"""

private fun ImmichAlbumAddRequest.toPayloadJson(): String =
    """{"albumId":"${albumId.escapeJson()}","assetIds":[${assetIds.sorted().joinToString(",") { """"${it.escapeJson()}"""" }}]}"""
