package com.marcportabella.immichuploader.data

class ApiKeyGatedImmichTransport(
    private val onlineTransport: ImmichOnlineTransport
) : ImmichTransport {

    fun selectExecutionPath(apiKey: String?): UploadExecutionPath =
        if (apiKey.isNullOrBlank()) UploadExecutionPath.BlockedMissingApiKey else UploadExecutionPath.ApiExecution

    fun gateStatus(apiKey: String?): TransportGateStatus =
        if (apiKey.isNullOrBlank()) TransportGateStatus.MissingApiKey else TransportGateStatus.Ready

    override suspend fun submit(plan: ImmichRequestPlan, apiKey: String?): ImmichTransportResult {
        if (selectExecutionPath(apiKey) == UploadExecutionPath.BlockedMissingApiKey) {
            return ImmichTransportResult.BlockedMissingApiKey(plan)
        }
        return onlineTransport.submit(plan, apiKey.orEmpty())
    }
}
