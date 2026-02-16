package com.marcportabella.immichuploader.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ApiImmichOnlineCatalogTransport(
    private val executor: ImmichApiExecutor = BrowserImmichApiExecutor()
) : ImmichOnlineCatalogTransport {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun lookupAlbums(apiKey: String): ImmichCatalogResult.Success {
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

        return ImmichCatalogResult.Success(request = request, entries = entries, message = message)
    }

    override suspend fun lookupTags(apiKey: String): ImmichCatalogResult.Success {
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

        return ImmichCatalogResult.Success(request = request, entries = entries, message = message)
    }

    override suspend fun createAlbumIfMissing(apiKey: String, name: String): ImmichCatalogResult.Success {
        val request = ImmichCatalogRequestBuilder.createAlbum(name)
        val createResult = execute(request, apiKey)
        if (createResult == null) {
            return ImmichCatalogResult.Success(
                request = request,
                entries = emptyList(),
                message = "Album create failed before response."
            )
        }
        return if (createResult.statusCode in 200..299 || createResult.statusCode == 409) {
            val refreshed = lookupAlbums(apiKey)
            refreshed.copy(message = "Album ensure requested for \"$name\". ${refreshed.message}")
        } else {
            ImmichCatalogResult.Success(
                request = request,
                entries = emptyList(),
                message = "Album create failed with HTTP ${createResult.statusCode}."
            )
        }
    }

    override suspend fun createTagIfMissing(apiKey: String, name: String): ImmichCatalogResult.Success {
        val request = ImmichCatalogRequestBuilder.createTag(name)
        val createResult = execute(request, apiKey)
        if (createResult == null) {
            return ImmichCatalogResult.Success(
                request = request,
                entries = emptyList(),
                message = "Tag create failed before response."
            )
        }
        return if (createResult.statusCode in 200..299 || createResult.statusCode == 409) {
            val refreshed = lookupTags(apiKey)
            refreshed.copy(message = "Tag ensure requested for \"$name\". ${refreshed.message}")
        } else {
            ImmichCatalogResult.Success(
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
            val name = nameKeys.firstNotNullOfOrNull { key -> obj[key]?.jsonPrimitive?.contentOrNull }
                ?: return@mapNotNull null
            ImmichCatalogEntry(id = id, name = name)
        }.distinctBy { it.id }.sortedBy { it.name.lowercase() }
    }
}
