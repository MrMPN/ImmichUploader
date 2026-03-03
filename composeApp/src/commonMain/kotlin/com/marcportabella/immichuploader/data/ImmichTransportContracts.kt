package com.marcportabella.immichuploader.data

enum class UploadExecutionPath {
    BlockedMissingApiKey,
    BlockedMissingServerBaseUrl,
    ApiExecution
}

sealed interface TransportGateStatus {
    data object Ready : TransportGateStatus
    data object MissingApiKey : TransportGateStatus
    data object MissingServerBaseUrl : TransportGateStatus
}

data class ImmichCatalogEntry(
    val id: String,
    val name: String
)

sealed interface ImmichCatalogResult {
    data class Success(
        val request: ImmichApiRequest,
        val entries: List<ImmichCatalogEntry>,
        val message: String
    ) : ImmichCatalogResult

    data class BlockedMissingApiKey(
        val request: ImmichApiRequest,
        val message: String
    ) : ImmichCatalogResult

    data class BlockedMissingServerBaseUrl(
        val request: ImmichApiRequest,
        val message: String
    ) : ImmichCatalogResult
}

sealed interface ImmichBulkUploadCheckResult {
    data class Success(
        val request: ImmichApiRequest,
        val existingAssetIdByItemId: Map<String, String>,
        val message: String
    ) : ImmichBulkUploadCheckResult

    data class BlockedMissingApiKey(
        val request: ImmichApiRequest,
        val message: String
    ) : ImmichBulkUploadCheckResult

    data class BlockedMissingServerBaseUrl(
        val request: ImmichApiRequest,
        val message: String
    ) : ImmichBulkUploadCheckResult
}

sealed interface ImmichTransportResult {
    data class BlockedMissingApiKey(val plan: ImmichRequestPlan) : ImmichTransportResult
    data class BlockedMissingServerBaseUrl(val plan: ImmichRequestPlan) : ImmichTransportResult
    data class Submitted(val requestCount: Int) : ImmichTransportResult
    data class Failed(val message: String) : ImmichTransportResult
}

interface ImmichTransport {
    suspend fun submit(plan: ImmichRequestPlan, apiKey: String?, serverBaseUrl: String?): ImmichTransportResult
}

interface ImmichOnlineTransport {
    suspend fun submit(plan: ImmichRequestPlan, apiKey: String, serverBaseUrl: String): ImmichTransportResult
}

interface ImmichApiExecutor {
    suspend fun execute(request: ImmichApiRequest, apiKey: String): ImmichApiExecutorResult
}

data class ImmichApiExecutorResult(
    val statusCode: Int,
    val responseBody: String
)

interface ImmichOnlineCatalogTransport {
    suspend fun lookupAlbums(apiKey: String, serverBaseUrl: String): ImmichCatalogResult.Success
    suspend fun lookupTags(apiKey: String, serverBaseUrl: String): ImmichCatalogResult.Success
    suspend fun createAlbumIfMissing(apiKey: String, serverBaseUrl: String, name: String): ImmichCatalogResult.Success
    suspend fun createTagIfMissing(apiKey: String, serverBaseUrl: String, name: String): ImmichCatalogResult.Success
    suspend fun bulkUploadCheck(
        apiKey: String,
        serverBaseUrl: String,
        items: List<ImmichBulkUploadCheckItem>
    ): ImmichBulkUploadCheckResult.Success
}
