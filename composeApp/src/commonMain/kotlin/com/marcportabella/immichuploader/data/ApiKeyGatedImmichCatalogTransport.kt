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
}
