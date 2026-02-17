package com.marcportabella.immichuploader.data

class ApiKeyGatedImmichCatalogTransport(
    private val onlineTransport: ImmichOnlineCatalogTransport
) {
    private val blockedMessage = "API key required for Immich catalog lookup/create."

    fun gateStatus(apiKey: String?): TransportGateStatus =
        if (apiKey.isNullOrBlank()) TransportGateStatus.MissingApiKey else TransportGateStatus.Ready

    suspend fun lookupAlbums(apiKey: String?): ImmichCatalogResult {
        val request = ImmichCatalogRequestBuilder.lookupAlbums()
        if (gateStatus(apiKey) == TransportGateStatus.MissingApiKey) {
            return ImmichCatalogResult.BlockedMissingApiKey(request, blockedMessage)
        }
        return onlineTransport.lookupAlbums(apiKey.orEmpty())
    }

    suspend fun lookupTags(apiKey: String?): ImmichCatalogResult {
        val request = ImmichCatalogRequestBuilder.lookupTags()
        if (gateStatus(apiKey) == TransportGateStatus.MissingApiKey) {
            return ImmichCatalogResult.BlockedMissingApiKey(request, blockedMessage)
        }
        return onlineTransport.lookupTags(apiKey.orEmpty())
    }

    suspend fun createAlbumIfMissing(apiKey: String?, name: String): ImmichCatalogResult {
        val normalized = name.trim()
        val request = ImmichCatalogRequestBuilder.createAlbum(normalized)
        if (gateStatus(apiKey) == TransportGateStatus.MissingApiKey) {
            return ImmichCatalogResult.BlockedMissingApiKey(request, blockedMessage)
        }
        if (normalized.isEmpty()) {
            return ImmichCatalogResult.Success(
                request = request,
                entries = emptyList(),
                message = "Album name is empty."
            )
        }
        return onlineTransport.createAlbumIfMissing(apiKey.orEmpty(), normalized)
    }

    suspend fun createTagIfMissing(apiKey: String?, name: String): ImmichCatalogResult {
        val normalized = name.trim()
        val request = ImmichCatalogRequestBuilder.createTag(normalized)
        if (gateStatus(apiKey) == TransportGateStatus.MissingApiKey) {
            return ImmichCatalogResult.BlockedMissingApiKey(request, blockedMessage)
        }
        if (normalized.isEmpty()) {
            return ImmichCatalogResult.Success(
                request = request,
                entries = emptyList(),
                message = "Tag name is empty."
            )
        }
        return onlineTransport.createTagIfMissing(apiKey.orEmpty(), normalized)
    }

    suspend fun bulkUploadCheck(apiKey: String?, items: List<ImmichBulkUploadCheckItem>): ImmichBulkUploadCheckResult {
        val normalized = items
            .map { it.copy(id = it.id.trim(), checksum = it.checksum.trim()) }
            .filter { it.id.isNotEmpty() && it.checksum.isNotEmpty() }
            .distinctBy { it.id }
        val request = ImmichCatalogRequestBuilder.bulkUploadCheck(normalized)
        if (gateStatus(apiKey) == TransportGateStatus.MissingApiKey) {
            return ImmichBulkUploadCheckResult.BlockedMissingApiKey(request, blockedMessage)
        }
        if (normalized.isEmpty()) {
            return ImmichBulkUploadCheckResult.Success(
                request = request,
                existingAssetIdByItemId = emptyMap(),
                message = "No checksums available for duplicate check."
            )
        }
        return onlineTransport.bulkUploadCheck(apiKey.orEmpty(), normalized)
    }
}
