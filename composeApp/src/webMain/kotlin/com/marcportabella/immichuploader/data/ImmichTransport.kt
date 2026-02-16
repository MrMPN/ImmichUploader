package com.marcportabella.immichuploader.data

import kotlinx.coroutines.await
import kotlin.js.JsAny
import kotlin.js.Promise

enum class UploadExecutionPath {
    BlockedMissingApiKey,
    ApiExecution
}

sealed interface TransportGateStatus {
    data object Ready : TransportGateStatus
    data object MissingApiKey : TransportGateStatus
}

data class ImmichCatalogEntry(
    val id: String,
    val name: String
)

sealed interface ImmichCatalogResult {
    data class DryRunSuccess(
        val request: ImmichApiRequest,
        val entries: List<ImmichCatalogEntry>,
        val message: String
    ) : ImmichCatalogResult

    data class BlockedMissingApiKey(
        val request: ImmichApiRequest,
        val message: String
    ) : ImmichCatalogResult
}

sealed interface ImmichTransportResult {
    data class DryRun(val plan: ImmichRequestPlan) : ImmichTransportResult
    data class BlockedMissingApiKey(val plan: ImmichRequestPlan) : ImmichTransportResult
    data class Submitted(val requestCount: Int) : ImmichTransportResult
    data class Failed(val message: String) : ImmichTransportResult
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

interface ImmichApiExecutor {
    suspend fun execute(request: ImmichApiRequest, apiKey: String): ImmichApiExecutorResult
}

data class ImmichApiExecutorResult(
    val statusCode: Int,
    val responseBody: String
)

class BrowserImmichApiExecutor : ImmichApiExecutor {
    override suspend fun execute(request: ImmichApiRequest, apiKey: String): ImmichApiExecutorResult {
        val headers = Headers().apply {
            append("x-api-key", apiKey)
            append("Accept", "application/json")
            if (request.body != null) {
                append("Content-Type", "application/json")
            }
        }

        val init = RequestInit(
            method = request.method,
            headers = headers
        ).apply {
            if (request.body != null) {
                body = request.body
            }
        }

        val response = fetch(
            input = request.url,
            init = init
        ).await<FetchResponse>()
        val responseBody = response.text().await<String>()

        return ImmichApiExecutorResult(
            statusCode = response.status.toInt(),
            responseBody = responseBody
        )
    }
}

private external class Headers {
    fun append(name: String, value: String)
}

private external class RequestInit(
    method: String? = definedExternally,
    headers: Headers? = definedExternally,
    body: String? = definedExternally
) {
    var body: String?
}

private external interface FetchResponse : JsAny {
    val status: Short
    fun text(): Promise<JsAny?>
}

private external fun fetch(input: String, init: RequestInit): Promise<JsAny?>

class ApiImmichOnlineTransport(
    private val executor: ImmichApiExecutor = BrowserImmichApiExecutor()
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

interface ImmichOnlineCatalogTransport {
    fun lookupAlbums(apiKey: String): ImmichCatalogResult.DryRunSuccess
    fun lookupTags(apiKey: String): ImmichCatalogResult.DryRunSuccess
    fun createAlbumIfMissing(apiKey: String, name: String): ImmichCatalogResult.DryRunSuccess
    fun createTagIfMissing(apiKey: String, name: String): ImmichCatalogResult.DryRunSuccess
}

class DryRunImmichCatalogTransport : ImmichOnlineCatalogTransport {
    private val albums = linkedMapOf<String, ImmichCatalogEntry>()
    private val tags = linkedMapOf<String, ImmichCatalogEntry>()

    override fun lookupAlbums(apiKey: String): ImmichCatalogResult.DryRunSuccess =
        ImmichCatalogResult.DryRunSuccess(
            request = ImmichCatalogRequestBuilder.lookupAlbums(),
            entries = albums.values.sortedBy { it.name.lowercase() },
            message = "Dry-run albums loaded."
        )

    override fun lookupTags(apiKey: String): ImmichCatalogResult.DryRunSuccess =
        ImmichCatalogResult.DryRunSuccess(
            request = ImmichCatalogRequestBuilder.lookupTags(),
            entries = tags.values.sortedBy { it.name.lowercase() },
            message = "Dry-run tags loaded."
        )

    override fun createAlbumIfMissing(apiKey: String, name: String): ImmichCatalogResult.DryRunSuccess {
        val normalized = name.trim()
        val existing = albums.values.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
        if (existing == null) {
            val created = ImmichCatalogEntry(
                id = "dryrun-album-${slugify(normalized)}",
                name = normalized
            )
            albums[created.id] = created
        }
        return ImmichCatalogResult.DryRunSuccess(
            request = ImmichCatalogRequestBuilder.createAlbum(normalized),
            entries = albums.values.sortedBy { it.name.lowercase() },
            message = "Dry-run album ensured: $normalized"
        )
    }

    override fun createTagIfMissing(apiKey: String, name: String): ImmichCatalogResult.DryRunSuccess {
        val normalized = name.trim()
        val existing = tags.values.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
        if (existing == null) {
            val created = ImmichCatalogEntry(
                id = "dryrun-tag-${slugify(normalized)}",
                name = normalized
            )
            tags[created.id] = created
        }
        return ImmichCatalogResult.DryRunSuccess(
            request = ImmichCatalogRequestBuilder.createTag(normalized),
            entries = tags.values.sortedBy { it.name.lowercase() },
            message = "Dry-run tag ensured: $normalized"
        )
    }
}

class ApiKeyGatedImmichCatalogTransport(
    private val onlineTransport: ImmichOnlineCatalogTransport
) {
    private val blockedMessage = "API key required for Immich catalog lookup/create."

    fun gateStatus(apiKey: String?): TransportGateStatus =
        if (apiKey.isNullOrBlank()) TransportGateStatus.MissingApiKey else TransportGateStatus.Ready

    fun lookupAlbums(apiKey: String?): ImmichCatalogResult {
        val request = ImmichCatalogRequestBuilder.lookupAlbums()
        if (gateStatus(apiKey) == TransportGateStatus.MissingApiKey) {
            return ImmichCatalogResult.BlockedMissingApiKey(request, blockedMessage)
        }
        return onlineTransport.lookupAlbums(apiKey.orEmpty())
    }

    fun lookupTags(apiKey: String?): ImmichCatalogResult {
        val request = ImmichCatalogRequestBuilder.lookupTags()
        if (gateStatus(apiKey) == TransportGateStatus.MissingApiKey) {
            return ImmichCatalogResult.BlockedMissingApiKey(request, blockedMessage)
        }
        return onlineTransport.lookupTags(apiKey.orEmpty())
    }

    fun createAlbumIfMissing(apiKey: String?, name: String): ImmichCatalogResult {
        val normalized = name.trim()
        val request = ImmichCatalogRequestBuilder.createAlbum(normalized)
        if (gateStatus(apiKey) == TransportGateStatus.MissingApiKey) {
            return ImmichCatalogResult.BlockedMissingApiKey(request, blockedMessage)
        }
        if (normalized.isEmpty()) {
            return ImmichCatalogResult.DryRunSuccess(
                request = request,
                entries = emptyList(),
                message = "Album name is empty."
            )
        }
        return onlineTransport.createAlbumIfMissing(apiKey.orEmpty(), normalized)
    }

    fun createTagIfMissing(apiKey: String?, name: String): ImmichCatalogResult {
        val normalized = name.trim()
        val request = ImmichCatalogRequestBuilder.createTag(normalized)
        if (gateStatus(apiKey) == TransportGateStatus.MissingApiKey) {
            return ImmichCatalogResult.BlockedMissingApiKey(request, blockedMessage)
        }
        if (normalized.isEmpty()) {
            return ImmichCatalogResult.DryRunSuccess(
                request = request,
                entries = emptyList(),
                message = "Tag name is empty."
            )
        }
        return onlineTransport.createTagIfMissing(apiKey.orEmpty(), normalized)
    }
}

private fun slugify(value: String): String =
    value.lowercase()
        .map { char -> if (char.isLetterOrDigit()) char else '-' }
        .joinToString("")
        .trim('-')
        .ifBlank { "item" }
