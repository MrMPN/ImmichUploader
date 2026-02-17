package com.marcportabella.immichuploader.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import com.marcportabella.immichuploader.platform.diagnosticMessage
import com.marcportabella.immichuploader.platform.platformLogError
import com.marcportabella.immichuploader.platform.platformLogInfo

class ApiImmichOnlineCatalogTransport(
    private val executor: ImmichApiExecutor = defaultImmichApiExecutor()
) : ImmichOnlineCatalogTransport {
    override suspend fun lookupAlbums(apiKey: String): ImmichCatalogResult.Success {
        val request = ImmichCatalogRequestBuilder.lookupAlbums()
        val execution = execute(request, apiKey)
        val result = execution.result
        val entries = if (result != null && result.statusCode in 200..299) {
            parseAlbumEntries(result.responseBody)
        } else {
            emptyList()
        }

        val message = when {
            result == null -> "Album lookup failed before response: ${execution.errorMessage ?: "unknown network error"}"
            result.statusCode !in 200..299 -> "Album lookup failed with HTTP ${result.statusCode}."
            else -> "Loaded ${entries.size} albums from server."
        }

        return ImmichCatalogResult.Success(request = request, entries = entries, message = message)
    }

    override suspend fun lookupTags(apiKey: String): ImmichCatalogResult.Success {
        val request = ImmichCatalogRequestBuilder.lookupTags()
        val execution = execute(request, apiKey)
        val result = execution.result
        val entries = if (result != null && result.statusCode in 200..299) {
            parseTagEntries(result.responseBody)
        } else {
            emptyList()
        }

        val message = when {
            result == null -> "Tag lookup failed before response: ${execution.errorMessage ?: "unknown network error"}"
            result.statusCode !in 200..299 -> "Tag lookup failed with HTTP ${result.statusCode}."
            else -> "Loaded ${entries.size} tags from server."
        }

        return ImmichCatalogResult.Success(request = request, entries = entries, message = message)
    }

    override suspend fun createAlbumIfMissing(apiKey: String, name: String): ImmichCatalogResult.Success {
        val request = ImmichCatalogRequestBuilder.createAlbum(name)
        val createExecution = execute(request, apiKey)
        val createResult = createExecution.result ?: return ImmichCatalogResult.Success(
            request = request,
            entries = emptyList(),
            message = "Album create failed before response: ${createExecution.errorMessage ?: "unknown network error"}"
        )
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
        val createExecution = execute(request, apiKey)
        val createResult = createExecution.result ?: return ImmichCatalogResult.Success(
            request = request,
            entries = emptyList(),
            message = "Tag create failed before response: ${createExecution.errorMessage ?: "unknown network error"}"
        )
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

    override suspend fun bulkUploadCheck(
        apiKey: String,
        items: List<ImmichBulkUploadCheckItem>
    ): ImmichBulkUploadCheckResult.Success {
        platformLogInfo("[immichuploader][dedup] request items=${items.size}")
        val request = ImmichCatalogRequestBuilder.bulkUploadCheck(items)
        val execution = execute(request, apiKey)
        val result = execution.result
        val existingAssets = if (result != null && result.statusCode in 200..299) {
            parseExistingAssetsByItemId(result.responseBody, requestedItems = items)
        } else {
            emptyMap()
        }

        val message = when {
            result == null -> "Duplicate check failed before response: ${execution.errorMessage ?: "unknown network error"}"
            result.statusCode !in 200..299 -> "Duplicate check failed with HTTP ${result.statusCode}."
            existingAssets.isEmpty() -> "Duplicate check completed. No existing server assets detected."
            else -> "Duplicate check completed. Found ${existingAssets.size} existing server assets."
        }

        return ImmichBulkUploadCheckResult.Success(
            request = request,
            existingAssetIdByItemId = existingAssets,
            message = message
        )
            .also {
                platformLogInfo(
                    "[immichuploader][dedup] response status=${result?.statusCode ?: -1} matches=${existingAssets.size}"
                )
            }
    }

    private suspend fun execute(request: ImmichApiRequest, apiKey: String): ExecutionAttempt =
        runCatching { executor.execute(request = request, apiKey = apiKey) }
            .fold(
                onSuccess = { ExecutionAttempt(result = it, errorMessage = null) },
                onFailure = { throwable ->
                    platformLogError(
                        "[immichuploader][catalog] request failed: ${request.method} ${request.url} error=${throwable.diagnosticMessage()}"
                    )
                    ExecutionAttempt(
                        result = null,
                        errorMessage = throwable.diagnosticMessage()
                    )
                }
            )

    private fun parseAlbumEntries(responseBody: String): List<ImmichCatalogEntry> =
        runCatching { immichJson.decodeFromString<List<ImmichAlbumResponse>>(responseBody) }
            .onFailure { throwable ->
                platformLogError("[immichuploader][catalog] album decode failed: ${throwable.diagnosticMessage()}")
            }
            .getOrDefault(emptyList())
            .mapNotNull { album ->
                val id = album.id ?: return@mapNotNull null
                val name = album.albumName ?: album.name ?: return@mapNotNull null
                ImmichCatalogEntry(id = id, name = name)
            }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }

    private fun parseTagEntries(responseBody: String): List<ImmichCatalogEntry> =
        runCatching { immichJson.decodeFromString<List<ImmichTagResponse>>(responseBody) }
            .onFailure { throwable ->
                platformLogError("[immichuploader][catalog] tag decode failed: ${throwable.diagnosticMessage()}")
            }
            .getOrDefault(emptyList())
            .mapNotNull { tag ->
                val id = tag.id ?: return@mapNotNull null
                val name = tag.value ?: tag.name ?: return@mapNotNull null
                ImmichCatalogEntry(id = id, name = name)
            }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }
}

private data class ExecutionAttempt(
    val result: ImmichApiExecutorResult?,
    val errorMessage: String?
)

@Serializable
private data class ImmichAlbumResponse(
    val id: String? = null,
    val albumName: String? = null,
    val name: String? = null
)

@Serializable
private data class ImmichTagResponse(
    val id: String? = null,
    val name: String? = null,
    val value: String? = null
)
