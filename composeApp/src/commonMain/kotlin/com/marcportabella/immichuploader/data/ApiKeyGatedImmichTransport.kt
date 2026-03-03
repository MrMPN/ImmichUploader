package com.marcportabella.immichuploader.data

class ApiKeyGatedImmichTransport(
    private val onlineTransport: ImmichOnlineTransport
) : ImmichTransport {

    fun selectExecutionPath(apiKey: String?, serverBaseUrl: String?): UploadExecutionPath =
        when {
            apiKey.isNullOrBlank() -> UploadExecutionPath.BlockedMissingApiKey
            normalizeImmichApiBaseUrl(serverBaseUrl.orEmpty()).isBlank() -> UploadExecutionPath.BlockedMissingServerBaseUrl
            else -> UploadExecutionPath.ApiExecution
        }

    fun gateStatus(apiKey: String?, serverBaseUrl: String?): TransportGateStatus =
        when (selectExecutionPath(apiKey = apiKey, serverBaseUrl = serverBaseUrl)) {
            UploadExecutionPath.BlockedMissingApiKey -> TransportGateStatus.MissingApiKey
            UploadExecutionPath.BlockedMissingServerBaseUrl -> TransportGateStatus.MissingServerBaseUrl
            UploadExecutionPath.ApiExecution -> TransportGateStatus.Ready
        }

    override suspend fun submit(plan: ImmichRequestPlan, apiKey: String?, serverBaseUrl: String?): ImmichTransportResult {
        return when (selectExecutionPath(apiKey = apiKey, serverBaseUrl = serverBaseUrl)) {
            UploadExecutionPath.BlockedMissingApiKey -> ImmichTransportResult.BlockedMissingApiKey(plan)
            UploadExecutionPath.BlockedMissingServerBaseUrl -> ImmichTransportResult.BlockedMissingServerBaseUrl(plan)
            UploadExecutionPath.ApiExecution -> onlineTransport.submit(
                plan = plan,
                apiKey = apiKey.orEmpty(),
                serverBaseUrl = normalizeImmichApiBaseUrl(serverBaseUrl.orEmpty())
            )
        }
    }
}
