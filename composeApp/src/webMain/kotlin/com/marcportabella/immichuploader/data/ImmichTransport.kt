package com.marcportabella.immichuploader.data

import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
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
    suspend fun lookupAlbums(apiKey: String): ImmichCatalogResult.DryRunSuccess
    suspend fun lookupTags(apiKey: String): ImmichCatalogResult.DryRunSuccess
    suspend fun createAlbumIfMissing(apiKey: String, name: String): ImmichCatalogResult.DryRunSuccess
    suspend fun createTagIfMissing(apiKey: String, name: String): ImmichCatalogResult.DryRunSuccess
}

class DryRunImmichCatalogTransport : ImmichOnlineCatalogTransport {
    private val albums = linkedMapOf<String, ImmichCatalogEntry>()
    private val tags = linkedMapOf<String, ImmichCatalogEntry>()

    override suspend fun lookupAlbums(apiKey: String): ImmichCatalogResult.DryRunSuccess =
        ImmichCatalogResult.DryRunSuccess(
            request = ImmichCatalogRequestBuilder.lookupAlbums(),
            entries = albums.values.sortedBy { it.name.lowercase() },
            message = "Dry-run albums loaded."
        )

    override suspend fun lookupTags(apiKey: String): ImmichCatalogResult.DryRunSuccess =
        ImmichCatalogResult.DryRunSuccess(
            request = ImmichCatalogRequestBuilder.lookupTags(),
            entries = tags.values.sortedBy { it.name.lowercase() },
            message = "Dry-run tags loaded."
        )

    override suspend fun createAlbumIfMissing(apiKey: String, name: String): ImmichCatalogResult.DryRunSuccess {
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

    override suspend fun createTagIfMissing(apiKey: String, name: String): ImmichCatalogResult.DryRunSuccess {
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

class ApiImmichOnlineCatalogTransport(
    private val executor: ImmichApiExecutor = BrowserImmichApiExecutor()
) : ImmichOnlineCatalogTransport {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun lookupAlbums(apiKey: String): ImmichCatalogResult.DryRunSuccess {
        val request = ImmichCatalogRequestBuilder.lookupAlbums()
        val result = execute(request, apiKey)
        val entries = if (result != null && result.statusCode in 200..299) {
            parseEntries(result.responseBody, nameKeys = listOf("albumName", "name"))
        } else {
            emptyList()
        }

        val message = when {
            result == null -> "Album lookup failed before response."
            result.statusCode !in 200..299 -> "Album lookup failed with HTTP ${result.statusCode}."
            else -> "Loaded ${entries.size} albums from server."
        }

        return ImmichCatalogResult.DryRunSuccess(request = request, entries = entries, message = message)
    }

    override suspend fun lookupTags(apiKey: String): ImmichCatalogResult.DryRunSuccess {
        val request = ImmichCatalogRequestBuilder.lookupTags()
        val result = execute(request, apiKey)
        val entries = if (result != null && result.statusCode in 200..299) {
            parseEntries(result.responseBody, nameKeys = listOf("value", "name"))
        } else {
            emptyList()
        }

        val message = when {
            result == null -> "Tag lookup failed before response."
            result.statusCode !in 200..299 -> "Tag lookup failed with HTTP ${result.statusCode}."
            else -> "Loaded ${entries.size} tags from server."
        }

        return ImmichCatalogResult.DryRunSuccess(request = request, entries = entries, message = message)
    }

    override suspend fun createAlbumIfMissing(apiKey: String, name: String): ImmichCatalogResult.DryRunSuccess {
        val request = ImmichCatalogRequestBuilder.createAlbum(name)
        val createResult = execute(request, apiKey)
        if (createResult == null) {
            return ImmichCatalogResult.DryRunSuccess(request = request, entries = emptyList(), message = "Album create failed before response.")
        }
        return if (createResult.statusCode in 200..299 || createResult.statusCode == 409) {
            val refreshed = lookupAlbums(apiKey)
            refreshed.copy(message = "Album ensure requested for \"$name\". ${refreshed.message}")
        } else {
            ImmichCatalogResult.DryRunSuccess(
                request = request,
                entries = emptyList(),
                message = "Album create failed with HTTP ${createResult.statusCode}."
            )
        }
    }

    override suspend fun createTagIfMissing(apiKey: String, name: String): ImmichCatalogResult.DryRunSuccess {
        val request = ImmichCatalogRequestBuilder.createTag(name)
        val createResult = execute(request, apiKey)
        if (createResult == null) {
            return ImmichCatalogResult.DryRunSuccess(request = request, entries = emptyList(), message = "Tag create failed before response.")
        }
        return if (createResult.statusCode in 200..299 || createResult.statusCode == 409) {
            val refreshed = lookupTags(apiKey)
            refreshed.copy(message = "Tag ensure requested for \"$name\". ${refreshed.message}")
        } else {
            ImmichCatalogResult.DryRunSuccess(
                request = request,
                entries = emptyList(),
                message = "Tag create failed with HTTP ${createResult.statusCode}."
            )
        }
    }

    private suspend fun execute(request: ImmichApiRequest, apiKey: String): ImmichApiExecutorResult? =
        runCatching { executor.execute(request = request, apiKey = apiKey) }.getOrNull()

    private fun parseEntries(responseBody: String, nameKeys: List<String>): List<ImmichCatalogEntry> {
        val root = runCatching { json.parseToJsonElement(responseBody) }.getOrNull() ?: return emptyList()
        val array = root as? JsonArray ?: return emptyList()
        return array.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = nameKeys.firstNotNullOfOrNull { key -> obj[key]?.jsonPrimitive?.contentOrNull } ?: return@mapNotNull null
            ImmichCatalogEntry(id = id, name = name)
        }.distinctBy { it.id }.sortedBy { it.name.lowercase() }
    }
}

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
            return ImmichCatalogResult.DryRunSuccess(
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
