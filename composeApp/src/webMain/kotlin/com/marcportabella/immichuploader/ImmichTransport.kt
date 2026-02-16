package com.marcportabella.immichuploader

sealed interface TransportGateStatus {
    data object Ready : TransportGateStatus
    data object MissingApiKey : TransportGateStatus
}

sealed interface ImmichTransportResult {
    data class DryRun(val plan: ImmichRequestPlan) : ImmichTransportResult
    data class BlockedMissingApiKey(val plan: ImmichRequestPlan) : ImmichTransportResult
    data class Submitted(val requestCount: Int) : ImmichTransportResult
}

interface ImmichTransport {
    suspend fun submit(plan: ImmichRequestPlan, apiKey: String?): ImmichTransportResult
}

interface ImmichOnlineTransport {
    suspend fun submit(plan: ImmichRequestPlan, apiKey: String): ImmichTransportResult
}

class DryRunImmichTransport : ImmichOnlineTransport {
    override suspend fun submit(plan: ImmichRequestPlan, apiKey: String): ImmichTransportResult =
        ImmichTransportResult.DryRun(plan)
}

class ApiKeyGatedImmichTransport(
    private val onlineTransport: ImmichOnlineTransport
) : ImmichTransport {

    fun gateStatus(apiKey: String?): TransportGateStatus =
        if (apiKey.isNullOrBlank()) TransportGateStatus.MissingApiKey else TransportGateStatus.Ready

    override suspend fun submit(plan: ImmichRequestPlan, apiKey: String?): ImmichTransportResult {
        if (gateStatus(apiKey) == TransportGateStatus.MissingApiKey) {
            return ImmichTransportResult.BlockedMissingApiKey(plan)
        }
        return onlineTransport.submit(plan, apiKey.orEmpty())
    }
}
