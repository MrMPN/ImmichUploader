package com.marcportabella.immichuploader.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

class ApiImmichOnlineTransport(
    private val executor: ImmichApiExecutor = defaultImmichApiExecutor()
) : ImmichOnlineTransport {
    override suspend fun submit(plan: ImmichRequestPlan, apiKey: String): ImmichTransportResult {
        val resolvedPlan = resolveSessionTagIds(plan = plan, apiKey = apiKey) ?: return ImmichTransportResult.Failed(
            "Failed to resolve one or more session tags before upload."
        )
        val requests = ImmichRequestBuilder.buildPayloadInspectorRequests(resolvedPlan)
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
}
