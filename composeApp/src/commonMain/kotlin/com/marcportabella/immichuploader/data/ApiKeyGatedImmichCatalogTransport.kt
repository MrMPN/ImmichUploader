package com.marcportabella.immichuploader.data

class ApiKeyGatedImmichCatalogTransport(
    private val onlineTransport: ImmichOnlineCatalogTransport
) {
    private val blockedApiKeyMessage = "API key required for Immich catalog lookup/create."
    private val blockedServerBaseUrlMessage = "Immich server URL required for catalog lookup/create."

    fun gateStatus(apiKey: String?, serverBaseUrl: String?): TransportGateStatus =
        when {
            apiKey.isNullOrBlank() -> TransportGateStatus.MissingApiKey
            normalizeImmichApiBaseUrl(serverBaseUrl.orEmpty()).isBlank() -> TransportGateStatus.MissingServerBaseUrl
            else -> TransportGateStatus.Ready
        }

    suspend fun lookupAlbums(apiKey: String?, serverBaseUrl: String?): ImmichCatalogResult {
        val normalizedServerBaseUrl = normalizeImmichApiBaseUrl(serverBaseUrl.orEmpty())
        val request = ImmichCatalogRequestBuilder.lookupAlbums(normalizedServerBaseUrl)
        return when (gateStatus(apiKey = apiKey, serverBaseUrl = normalizedServerBaseUrl)) {
            TransportGateStatus.MissingApiKey ->
                ImmichCatalogResult.BlockedMissingApiKey(request, blockedApiKeyMessage)

            TransportGateStatus.MissingServerBaseUrl ->
                ImmichCatalogResult.BlockedMissingServerBaseUrl(request, blockedServerBaseUrlMessage)

            TransportGateStatus.Ready ->
                onlineTransport.lookupAlbums(apiKey = apiKey.orEmpty(), serverBaseUrl = normalizedServerBaseUrl)
        }
    }

    suspend fun lookupTags(apiKey: String?, serverBaseUrl: String?): ImmichCatalogResult {
        val normalizedServerBaseUrl = normalizeImmichApiBaseUrl(serverBaseUrl.orEmpty())
        val request = ImmichCatalogRequestBuilder.lookupTags(normalizedServerBaseUrl)
        return when (gateStatus(apiKey = apiKey, serverBaseUrl = normalizedServerBaseUrl)) {
            TransportGateStatus.MissingApiKey ->
                ImmichCatalogResult.BlockedMissingApiKey(request, blockedApiKeyMessage)

            TransportGateStatus.MissingServerBaseUrl ->
                ImmichCatalogResult.BlockedMissingServerBaseUrl(request, blockedServerBaseUrlMessage)

            TransportGateStatus.Ready ->
                onlineTransport.lookupTags(apiKey = apiKey.orEmpty(), serverBaseUrl = normalizedServerBaseUrl)
        }
    }

    suspend fun createAlbumIfMissing(apiKey: String?, serverBaseUrl: String?, name: String): ImmichCatalogResult {
        val normalized = name.trim()
        val normalizedServerBaseUrl = normalizeImmichApiBaseUrl(serverBaseUrl.orEmpty())
        val request = ImmichCatalogRequestBuilder.createAlbum(normalizedServerBaseUrl, normalized)
        when (gateStatus(apiKey = apiKey, serverBaseUrl = normalizedServerBaseUrl)) {
            TransportGateStatus.MissingApiKey ->
                return ImmichCatalogResult.BlockedMissingApiKey(request, blockedApiKeyMessage)

            TransportGateStatus.MissingServerBaseUrl ->
                return ImmichCatalogResult.BlockedMissingServerBaseUrl(request, blockedServerBaseUrlMessage)

            TransportGateStatus.Ready -> Unit
        }
        if (normalized.isEmpty()) {
            return ImmichCatalogResult.Success(
                request = request,
                entries = emptyList(),
                message = "Album name is empty."
            )
        }
        return onlineTransport.createAlbumIfMissing(
            apiKey = apiKey.orEmpty(),
            serverBaseUrl = normalizedServerBaseUrl,
            name = normalized
        )
    }

    suspend fun createTagIfMissing(apiKey: String?, serverBaseUrl: String?, name: String): ImmichCatalogResult {
        val normalized = name.trim()
        val normalizedServerBaseUrl = normalizeImmichApiBaseUrl(serverBaseUrl.orEmpty())
        val request = ImmichCatalogRequestBuilder.createTag(normalizedServerBaseUrl, normalized)
        when (gateStatus(apiKey = apiKey, serverBaseUrl = normalizedServerBaseUrl)) {
            TransportGateStatus.MissingApiKey ->
                return ImmichCatalogResult.BlockedMissingApiKey(request, blockedApiKeyMessage)

            TransportGateStatus.MissingServerBaseUrl ->
                return ImmichCatalogResult.BlockedMissingServerBaseUrl(request, blockedServerBaseUrlMessage)

            TransportGateStatus.Ready -> Unit
        }
        if (normalized.isEmpty()) {
            return ImmichCatalogResult.Success(
                request = request,
                entries = emptyList(),
                message = "Tag name is empty."
            )
        }
        return onlineTransport.createTagIfMissing(
            apiKey = apiKey.orEmpty(),
            serverBaseUrl = normalizedServerBaseUrl,
            name = normalized
        )
    }

    suspend fun bulkUploadCheck(
        apiKey: String?,
        serverBaseUrl: String?,
        items: List<ImmichBulkUploadCheckItem>
    ): ImmichBulkUploadCheckResult {
        val normalized = items
            .map { it.copy(id = it.id.trim(), checksum = it.checksum.trim()) }
            .filter { it.id.isNotEmpty() && it.checksum.isNotEmpty() }
            .distinctBy { it.id }
        val normalizedServerBaseUrl = normalizeImmichApiBaseUrl(serverBaseUrl.orEmpty())
        val request = ImmichCatalogRequestBuilder.bulkUploadCheck(normalizedServerBaseUrl, normalized)
        when (gateStatus(apiKey = apiKey, serverBaseUrl = normalizedServerBaseUrl)) {
            TransportGateStatus.MissingApiKey ->
                return ImmichBulkUploadCheckResult.BlockedMissingApiKey(request, blockedApiKeyMessage)

            TransportGateStatus.MissingServerBaseUrl ->
                return ImmichBulkUploadCheckResult.BlockedMissingServerBaseUrl(request, blockedServerBaseUrlMessage)

            TransportGateStatus.Ready -> Unit
        }
        if (normalized.isEmpty()) {
            return ImmichBulkUploadCheckResult.Success(
                request = request,
                existingAssetIdByItemId = emptyMap(),
                message = "No checksums available for duplicate check."
            )
        }
        return onlineTransport.bulkUploadCheck(
            apiKey = apiKey.orEmpty(),
            serverBaseUrl = normalizedServerBaseUrl,
            items = normalized
        )
    }
}
