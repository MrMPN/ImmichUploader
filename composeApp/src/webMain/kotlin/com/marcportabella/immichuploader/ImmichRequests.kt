package com.marcportabella.immichuploader

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

        if (description == null && isFavorite == null && dateTimeOriginal == null) {
            return null
        }

        return ImmichBulkMetadataRequest(
            ids = assetIds.toList().sorted(),
            dateTimeOriginal = dateTimeOriginal,
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
}
