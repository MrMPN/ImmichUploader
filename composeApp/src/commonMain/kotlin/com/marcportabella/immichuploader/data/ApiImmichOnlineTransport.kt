package com.marcportabella.immichuploader.data

import com.marcportabella.immichuploader.platform.platformLogInfo
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class ApiImmichOnlineTransport(
    private val executor: ImmichApiExecutor = defaultImmichApiExecutor()
) : ImmichOnlineTransport {
    override suspend fun submit(plan: ImmichRequestPlan, apiKey: String): ImmichTransportResult {
        val resolvedPlan = resolveSessionCatalogIds(plan = plan, apiKey = apiKey) ?: return ImmichTransportResult.Failed(
            "Failed to resolve one or more session tags/albums before upload."
        )
        val missingSource = resolvedPlan.uploadRequests.firstOrNull { it.sourceFile == null }
        if (missingSource != null) {
            return ImmichTransportResult.Failed(
                "Upload request for local asset ${missingSource.localAssetId} has no source file attached."
            )
        }
        var executedRequestCount = 0

        resolvedPlan.lookupHooks.forEach { hook ->
            val request = when (hook) {
                ImmichLookupHook.LookupAlbums -> ImmichCatalogRequestBuilder.lookupAlbums()
                ImmichLookupHook.LookupTags -> ImmichCatalogRequestBuilder.lookupTags()
                is ImmichLookupHook.CreateAlbumIfMissing -> ImmichCatalogRequestBuilder.createAlbum(hook.name)
                is ImmichLookupHook.CreateTagIfMissing -> ImmichCatalogRequestBuilder.createTag(hook.name)
            }
            val result = runRequestOrFail(request = request, requestOrdinal = executedRequestCount + 1, apiKey = apiKey)
            executedRequestCount++
            if (result.statusCode !in 200..299) {
                return ImmichTransportResult.Failed(result.responseBody)
            }
        }

        val remoteAssetIdByPlaceholder = mutableMapOf<String, String>()
        resolvedPlan.uploadRequests.forEach { upload ->
            val request = ImmichApiRequest(
                method = "POST",
                url = "$IMMICH_API_BASE_URL/assets",
                body = ImmichUploadBody(
                    payload = ImmichUploadPayload(
                        localAssetId = upload.localAssetId,
                        fileName = upload.fileName,
                        mimeType = upload.mimeType,
                        sourceFile = upload.sourceFile,
                        sidecarData = upload.sidecarData,
                        deviceAssetId = upload.deviceAssetId,
                        deviceId = upload.deviceId,
                        fileCreatedAt = upload.fileCreatedAt,
                        fileModifiedAt = upload.fileModifiedAt
                    )
                )
            )
            val result = runRequestOrFail(request = request, requestOrdinal = executedRequestCount + 1, apiKey = apiKey)
            executedRequestCount++
            if (result.statusCode !in 200..299) {
                return ImmichTransportResult.Failed(result.responseBody)
            }

            val remoteAssetId = extractUploadedAssetId(result.responseBody)
            if (remoteAssetId.isNullOrBlank()) {
                return ImmichTransportResult.Failed(
                    "Upload response missing asset id for local asset ${upload.localAssetId}."
                )
            }
            remoteAssetIdByPlaceholder["remote-${upload.localAssetId}"] = remoteAssetId
            platformLogInfo(
                "[immichuploader][exec] uploaded local=${upload.localAssetId} -> remote=$remoteAssetId"
            )
        }

        val resolvedBulkMetadataRequests = resolvedPlan.bulkMetadataRequests.map { request ->
            val resolvedIds = request.ids.map { id -> remoteAssetIdByPlaceholder[id] ?: id }
            request.copy(ids = resolvedIds)
        }
        val resolvedTagAssignRequests = resolvedPlan.tagAssignRequests.map { request ->
            val resolvedAssetIds = request.assetIds.map { id -> remoteAssetIdByPlaceholder[id] ?: id }
            request.copy(assetIds = resolvedAssetIds)
        }
        val resolvedAlbumAddRequests = resolvedPlan.albumAddRequests.map { request ->
            val resolvedAssetIds = request.assetIds.map { id -> remoteAssetIdByPlaceholder[id] ?: id }
            request.copy(assetIds = resolvedAssetIds)
        }

        if (containsPlaceholderMetadataId(resolvedBulkMetadataRequests) ||
            containsPlaceholderTagAssetId(resolvedTagAssignRequests) ||
            containsPlaceholderAlbumAssetId(resolvedAlbumAddRequests)
        ) {
            return ImmichTransportResult.Failed(
                "Failed to resolve one or more uploaded asset IDs before metadata/tag/album requests."
            )
        }

        if (resolvedBulkMetadataRequests.isNotEmpty()) {
            platformLogInfo(
                "[immichuploader][exec] skipping metadata PUT updates; metadata is applied via upload timestamps"
            )
        }

        resolvedTagAssignRequests.forEach { request ->
            platformLogInfo(
                "[immichuploader][exec] tagAssets assetIds=${request.assetIds.joinToString(",")} tagIds=${request.tagIds.joinToString(",")}"
            )
            val ids = request.assetIds.sorted()
            request.tagIds.sorted().forEach { tagId ->
                val apiRequest = ImmichApiRequest(
                    method = "PUT",
                    url = "$IMMICH_API_BASE_URL/tags/$tagId/assets",
                    body = ImmichTagAssetsBody(ImmichTagAssetsRequest(ids = ids))
                )
                val result = runRequestOrFail(
                    request = apiRequest,
                    requestOrdinal = executedRequestCount + 1,
                    apiKey = apiKey
                )
                executedRequestCount++
                if (result.statusCode !in 200..299) {
                    return ImmichTransportResult.Failed(result.responseBody)
                }
                platformLogInfo("[immichuploader][exec] tagAssets response=${result.responseBody}")
            }
        }

        resolvedAlbumAddRequests.forEach { request ->
            val apiRequest = ImmichApiRequest(
                method = "PUT",
                url = "$IMMICH_API_BASE_URL/albums/assets",
                body = ImmichAlbumAddBody(request)
            )
            val result = runRequestOrFail(request = apiRequest, requestOrdinal = executedRequestCount + 1, apiKey = apiKey)
            executedRequestCount++
            if (result.statusCode !in 200..299) {
                return ImmichTransportResult.Failed(result.responseBody)
            }
        }

        return ImmichTransportResult.Submitted(executedRequestCount)
    }

    private suspend fun resolveSessionCatalogIds(plan: ImmichRequestPlan, apiKey: String): ImmichRequestPlan? {
        val tagResolved = resolveSessionTagIds(plan = plan, apiKey = apiKey) ?: return null
        return resolveSessionAlbumIds(plan = tagResolved, apiKey = apiKey)
    }

    private suspend fun resolveSessionTagIds(plan: ImmichRequestPlan, apiKey: String): ImmichRequestPlan? {
        if (plan.sessionTagsById.isEmpty()) return plan

        val usedSessionTagIds = plan.tagAssignRequests
            .asSequence()
            .flatMap { it.tagIds.asSequence() }
            .filter { it in plan.sessionTagsById }
            .toSet()
        if (usedSessionTagIds.isEmpty()) return plan

        val resolvedIdsBySessionId = mutableMapOf<String, String>()
        usedSessionTagIds.forEach { sessionTagId ->
            val tagName = plan.sessionTagsById[sessionTagId] ?: return@forEach
            val resolvedTagId = resolveTagIdByName(name = tagName, apiKey = apiKey) ?: return null
            resolvedIdsBySessionId[sessionTagId] = resolvedTagId
        }

        val resolvedTagAssign = plan.tagAssignRequests.map { request ->
            val resolvedTagIds = request.tagIds
                .map { tagId -> resolvedIdsBySessionId[tagId] ?: tagId }
                .distinct()
                .sorted()
            request.copy(tagIds = resolvedTagIds)
        }

        return plan.copy(tagAssignRequests = resolvedTagAssign)
    }

    private suspend fun resolveSessionAlbumIds(plan: ImmichRequestPlan, apiKey: String): ImmichRequestPlan? {
        if (plan.sessionAlbumsById.isEmpty()) return plan

        val usedSessionAlbumIds = plan.albumAddRequests
            .asSequence()
            .map { it.albumId }
            .filter { it in plan.sessionAlbumsById }
            .toSet()
        if (usedSessionAlbumIds.isEmpty()) return plan

        val resolvedIdsBySessionId = mutableMapOf<String, String>()
        usedSessionAlbumIds.forEach { sessionAlbumId ->
            val albumName = plan.sessionAlbumsById[sessionAlbumId] ?: return@forEach
            val resolvedAlbumId = resolveAlbumIdByName(name = albumName, apiKey = apiKey) ?: return null
            resolvedIdsBySessionId[sessionAlbumId] = resolvedAlbumId
        }

        val resolvedAlbumAdd = plan.albumAddRequests.map { request ->
            request.copy(albumId = resolvedIdsBySessionId[request.albumId] ?: request.albumId)
        }

        return plan.copy(albumAddRequests = resolvedAlbumAdd)
    }

    private suspend fun resolveTagIdByName(name: String, apiKey: String): String? {
        lookupTagIdByName(name = name, apiKey = apiKey)?.let { return it }

        val createResult = runCatching {
            executor.execute(
                request = ImmichCatalogRequestBuilder.createTag(name),
                apiKey = apiKey
            )
        }.getOrNull() ?: return null

        if (createResult.statusCode !in 200..299) {
            return lookupTagIdByName(name = name, apiKey = apiKey)
        }

        extractTagIdFromPayload(createResult.responseBody)?.let { return it }
        return lookupTagIdByName(name = name, apiKey = apiKey)
    }

    private suspend fun lookupTagIdByName(name: String, apiKey: String): String? {
        val response = runCatching {
            executor.execute(
                request = ImmichCatalogRequestBuilder.lookupTags(),
                apiKey = apiKey
            )
        }.getOrNull() ?: return null
        if (response.statusCode !in 200..299) return null
        return extractTagIdByName(responseBody = response.responseBody, name = name)
    }

    private fun extractTagIdByName(responseBody: String, name: String): String? {
        val root = runCatching { immichJson.parseToJsonElement(responseBody) }.getOrNull() ?: return null
        val entries = when (root) {
            is JsonArray -> root
            is JsonObject -> root["tags"]?.jsonArray ?: return null
            else -> return null
        }

        entries.forEach { element ->
            val obj = element as? JsonObject ?: return@forEach
            val tagName = (obj["name"] as? JsonPrimitive)?.content
                ?: (obj["value"] as? JsonPrimitive)?.content
                ?: return@forEach
            if (!tagName.equals(name, ignoreCase = true)) return@forEach
            val id = (obj["id"] as? JsonPrimitive)?.content
            if (!id.isNullOrBlank()) return id
        }
        return null
    }

    private fun extractTagIdFromPayload(responseBody: String): String? {
        val root = runCatching { immichJson.parseToJsonElement(responseBody) }.getOrNull() as? JsonObject ?: return null
        return (root["id"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
    }

    private suspend fun resolveAlbumIdByName(name: String, apiKey: String): String? {
        lookupAlbumIdByName(name = name, apiKey = apiKey)?.let { return it }

        val createResult = runCatching {
            executor.execute(
                request = ImmichCatalogRequestBuilder.createAlbum(name),
                apiKey = apiKey
            )
        }.getOrNull() ?: return null

        if (createResult.statusCode !in 200..299) {
            return lookupAlbumIdByName(name = name, apiKey = apiKey)
        }

        extractAlbumIdFromPayload(createResult.responseBody)?.let { return it }
        return lookupAlbumIdByName(name = name, apiKey = apiKey)
    }

    private suspend fun lookupAlbumIdByName(name: String, apiKey: String): String? {
        val response = runCatching {
            executor.execute(
                request = ImmichCatalogRequestBuilder.lookupAlbums(),
                apiKey = apiKey
            )
        }.getOrNull() ?: return null
        if (response.statusCode !in 200..299) return null
        return extractAlbumIdByName(responseBody = response.responseBody, name = name)
    }

    private fun extractAlbumIdByName(responseBody: String, name: String): String? {
        val root = runCatching { immichJson.parseToJsonElement(responseBody) }.getOrNull() ?: return null
        val entries = when (root) {
            is JsonArray -> root
            is JsonObject -> root["albums"]?.jsonArray ?: return null
            else -> return null
        }

        entries.forEach { element ->
            val obj = element as? JsonObject ?: return@forEach
            val albumName = (obj["albumName"] as? JsonPrimitive)?.content
                ?: (obj["name"] as? JsonPrimitive)?.content
                ?: return@forEach
            if (!albumName.equals(name, ignoreCase = true)) return@forEach
            val id = (obj["id"] as? JsonPrimitive)?.content
            if (!id.isNullOrBlank()) return id
        }
        return null
    }

    private fun extractAlbumIdFromPayload(responseBody: String): String? {
        val root = runCatching { immichJson.parseToJsonElement(responseBody) }.getOrNull() as? JsonObject ?: return null
        return (root["id"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
    }

    private suspend fun runRequestOrFail(
        request: ImmichApiRequest,
        requestOrdinal: Int,
        apiKey: String
    ): ImmichApiExecutorResult {
        val result = runCatching {
            executor.execute(request = request, apiKey = apiKey)
        }.getOrElse { throwable ->
            val message = throwable.message ?: "Unknown transport failure"
            return ImmichApiExecutorResult(
                statusCode = 0,
                responseBody = "Request $requestOrdinal failed before response: $message"
            )
        }
        if (result.statusCode !in 200..299) {
            return ImmichApiExecutorResult(
                statusCode = result.statusCode,
                responseBody = "Request $requestOrdinal failed with HTTP ${result.statusCode} (${request.method} ${request.url})."
            )
        }
        return result
    }

    private fun extractUploadedAssetId(responseBody: String): String? {
        val root = runCatching { immichJson.parseToJsonElement(responseBody) }.getOrNull() as? JsonObject ?: return null
        return root["id"]?.let { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
            ?: root["asset"]?.let { asset ->
                (asset as? JsonObject)?.get("id")?.let { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
            }
    }

    private fun containsPlaceholderMetadataId(requests: List<ImmichBulkMetadataRequest>): Boolean =
        requests.any { req -> req.ids.any { id -> id.startsWith("remote-") } }

    private fun containsPlaceholderTagAssetId(requests: List<ImmichTagAssignRequest>): Boolean =
        requests.any { req -> req.assetIds.any { id -> id.startsWith("remote-") } }

    private fun containsPlaceholderAlbumAssetId(requests: List<ImmichAlbumAddRequest>): Boolean =
        requests.any { req -> req.assetIds.any { id -> id.startsWith("remote-") } }

}
