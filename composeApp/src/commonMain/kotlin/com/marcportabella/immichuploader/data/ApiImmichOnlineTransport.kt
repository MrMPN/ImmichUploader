package com.marcportabella.immichuploader.data

class ApiImmichOnlineTransport(
    private val executor: ImmichApiExecutor = defaultImmichApiExecutor()
) : ImmichOnlineTransport {
    override suspend fun submit(plan: ImmichRequestPlan, apiKey: String): ImmichTransportResult {
        val requests = ImmichRequestBuilder.buildPayloadInspectorRequests(plan)
        requests.forEachIndexed { index, request ->
            val result = runCatching {
                executor.execute(request = request, apiKey = apiKey)
            }.getOrElse { throwable ->
                val message = throwable.message ?: "Unknown transport failure"
                return ImmichTransportResult.Failed(
                    "Request ${index + 1} failed before response: $message"
                )
            }

            if (result.statusCode !in 200..299) {
                return ImmichTransportResult.Failed(
                    "Request ${index + 1} failed with HTTP ${result.statusCode} (${request.method} ${request.url})."
                )
            }
        }

        return ImmichTransportResult.Submitted(requests.size)
    }
}
