package com.marcportabella.immichuploader.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private const val IMMICH_API_BASE_URL: String = "https://fotos.marcportabella.com/api"
private const val FALLBACK_TIMESTAMP = "1970-01-01T00:00:00Z"
private const val DEFAULT_DEVICE_ID = "web-local-device"

private val uploadRequestJson: Json = Json {
    encodeDefaults = false
    explicitNulls = false
    ignoreUnknownKeys = true
}

@Serializable
data class UploadCatalogEntry(
    val id: String,
    val name: String
)

@Serializable
data class UploadApiRequest(
    val method: String,
    val url: String,
    val body: String? = null
)

@Serializable
data class UploadUploadRequest(
    val localAssetId: String,
    val fileName: String,
    val mimeType: String,
    val sidecarData: String? = null,
    val deviceAssetId: String,
    val deviceId: String,
    val fileCreatedAt: String,
    val fileModifiedAt: String,
    val metadata: Map<String, String>
)

@Serializable
data class UploadBulkMetadataRequest(
    val ids: List<String>,
    val dateTimeOriginal: String? = null,
    val timeZone: String? = null,
    val description: String? = null,
    val isFavorite: Boolean? = null
)

@Serializable
data class UploadTagAssignRequest(
    val assetIds: List<String>,
    val tagIds: List<String>
)

@Serializable
data class UploadAlbumAddRequest(
    val albumId: String,
    val assetIds: List<String>
)

sealed interface UploadLookupHook {
    data object LookupAlbums : UploadLookupHook
    data object LookupTags : UploadLookupHook
    data class CreateAlbumIfMissing(val name: String) : UploadLookupHook
    data class CreateTagIfMissing(val name: String) : UploadLookupHook
}

data class UploadRequestPlan(
    val uploadRequests: List<UploadUploadRequest> = emptyList(),
    val bulkMetadataRequests: List<UploadBulkMetadataRequest> = emptyList(),
    val tagAssignRequests: List<UploadTagAssignRequest> = emptyList(),
    val albumAddRequests: List<UploadAlbumAddRequest> = emptyList(),
    val lookupHooks: List<UploadLookupHook> = emptyList(),
    val sessionAlbumsById: Map<String, String> = emptyMap(),
    val sessionTagsById: Map<String, String> = emptyMap()
)

object UploadRequestPlanner {
    fun buildDryRunPlan(
        state: UploadPrepState,
        deviceId: String = DEFAULT_DEVICE_ID
    ): UploadRequestPlan {
        val selectedIds = state.selectedAssetIds.toList().sortedBy { it.value }
        val uploadRequests = selectedIds.mapNotNull { assetId ->
            val asset = state.assets[assetId] ?: return@mapNotNull null
            val patch = state.stagedEditsByAssetId[assetId]
            buildUploadRequest(asset = asset, deviceId = deviceId, patch = patch)
        }

        val remoteIdsByPatch = linkedMapOf<AssetEditPatch, MutableSet<String>>()
        selectedIds.forEach { assetId ->
            val patch = state.stagedEditsByAssetId[assetId] ?: return@forEach
            val remoteIds = remoteIdsByPatch.getOrPut(patch) { linkedSetOf() }
            remoteIds += "remote-${assetId.value}"
        }

        val tagAssignRequests = mutableListOf<UploadTagAssignRequest>()
        val albumAddRequests = mutableListOf<UploadAlbumAddRequest>()

        remoteIdsByPatch.forEach { (patch, remoteIds) ->
            buildTagAssignRequest(remoteIds, patch)?.let { tagAssignRequests += it }
            buildAlbumAddRequest(remoteIds, patch)?.let { albumAddRequests += it }
        }

        val lookupHooks = buildLookupHooks(
            albumsToCreate = collectSessionAlbumNamesForSelection(state) + setOf(state.albumCreateDraft),
            tagsToCreate = collectSessionTagNamesForSelection(state) + setOf(state.tagCreateDraft)
        )

        return UploadRequestPlan(
            uploadRequests = uploadRequests,
            tagAssignRequests = tagAssignRequests,
            albumAddRequests = albumAddRequests,
            lookupHooks = lookupHooks,
            sessionAlbumsById = state.sessionAlbumsById,
            sessionTagsById = state.sessionTagsById
        )
    }

    fun buildPayloadInspectorRequests(plan: UploadRequestPlan): List<UploadApiRequest> {
        val requests = mutableListOf<UploadApiRequest>()

        plan.lookupHooks.forEach { hook ->
            requests += when (hook) {
                UploadLookupHook.LookupAlbums ->
                    UploadApiRequest(method = "GET", url = "$IMMICH_API_BASE_URL/albums")

                UploadLookupHook.LookupTags ->
                    UploadApiRequest(method = "GET", url = "$IMMICH_API_BASE_URL/tags")

                is UploadLookupHook.CreateAlbumIfMissing ->
                    UploadApiRequest(
                        method = "POST",
                        url = "$IMMICH_API_BASE_URL/albums",
                        body = uploadRequestJson.encodeToString(UploadNameRequest(hook.name.trim()))
                    )

                is UploadLookupHook.CreateTagIfMissing ->
                    UploadApiRequest(
                        method = "POST",
                        url = "$IMMICH_API_BASE_URL/tags",
                        body = uploadRequestJson.encodeToString(UploadNameRequest(hook.name.trim()))
                    )
            }
        }

        plan.uploadRequests.forEach { request ->
            requests += UploadApiRequest(
                method = "POST",
                url = "$IMMICH_API_BASE_URL/assets",
                body = request.toPayloadJson()
            )
        }

        plan.tagAssignRequests.forEach { request ->
            request.tagIds.sorted().forEach { tagId ->
                requests += UploadApiRequest(
                    method = "PUT",
                    url = "$IMMICH_API_BASE_URL/tags/$tagId/assets",
                    body = uploadRequestJson.encodeToString(UploadTagAssetsPayload(request.assetIds.sorted()))
                )
            }
        }

        plan.albumAddRequests.forEach { request ->
            requests += UploadApiRequest(
                method = "PUT",
                url = "$IMMICH_API_BASE_URL/albums/assets",
                body = request.toPayloadJson()
            )
        }

        return requests
    }

    private fun buildUploadRequest(
        asset: LocalAsset,
        deviceId: String,
        patch: AssetEditPatch? = null
    ): UploadUploadRequest {
        val patchDateTime = (patch?.dateTimeOriginal as? FieldPatch.Set<String>)?.value
        val patchTimeZone = (patch?.timeZone as? FieldPatch.Set<String>)?.value
        val timestampBase = patchDateTime ?: asset.captureDateTime ?: FALLBACK_TIMESTAMP
        val timestamp = withTimezoneOffsetIfMissing(timestampBase, patchTimeZone)
        return UploadUploadRequest(
            localAssetId = asset.id.value,
            fileName = asset.fileName,
            mimeType = asset.mimeType,
            sidecarData = buildXmpSidecarData(timestamp, patchDateTime != null || patchTimeZone != null),
            deviceAssetId = asset.id.value,
            deviceId = deviceId,
            fileCreatedAt = timestamp,
            fileModifiedAt = timestamp,
            metadata = mapOf(
                "fileName" to asset.fileName,
                "mimeType" to asset.mimeType
            )
        )
    }

    private fun buildBulkMetadataRequest(assetIds: Set<String>, patch: AssetEditPatch): UploadBulkMetadataRequest? {
        if (assetIds.isEmpty()) return null

        val description = (patch.description as? FieldPatch.Set<String?>)?.value
        val isFavorite = (patch.isFavorite as? FieldPatch.Set<Boolean>)?.value
        val dateTimeOriginal = (patch.dateTimeOriginal as? FieldPatch.Set<String>)?.value
        val timeZone = (patch.timeZone as? FieldPatch.Set<String>)?.value

        if (description == null && isFavorite == null && dateTimeOriginal == null && timeZone == null) {
            return null
        }

        return UploadBulkMetadataRequest(
            ids = assetIds.toList().sorted(),
            dateTimeOriginal = dateTimeOriginal,
            timeZone = timeZone,
            description = description,
            isFavorite = isFavorite
        )
    }

    private fun buildTagAssignRequest(assetIds: Set<String>, patch: AssetEditPatch): UploadTagAssignRequest? {
        if (assetIds.isEmpty() || patch.addTagIds.isEmpty()) return null
        return UploadTagAssignRequest(
            assetIds = assetIds.toList().sorted(),
            tagIds = patch.addTagIds.toList().sorted()
        )
    }

    private fun buildAlbumAddRequest(assetIds: Set<String>, patch: AssetEditPatch): UploadAlbumAddRequest? {
        if (assetIds.isEmpty()) return null
        val albumId = (patch.albumId as? FieldPatch.Set<String?>)?.value ?: return null
        if (albumId.isBlank()) return null

        return UploadAlbumAddRequest(
            albumId = albumId,
            assetIds = assetIds.toList().sorted()
        )
    }

    private fun buildLookupHooks(
        albumsToCreate: Set<String>,
        tagsToCreate: Set<String>
    ): List<UploadLookupHook> {
        val hooks = mutableListOf<UploadLookupHook>()
        albumsToCreate.map { it.trim() }.filter { it.isNotEmpty() }.sorted().forEach {
            hooks += UploadLookupHook.CreateAlbumIfMissing(it)
        }
        tagsToCreate.map { it.trim() }.filter { it.isNotEmpty() }.sorted().forEach {
            hooks += UploadLookupHook.CreateTagIfMissing(it)
        }
        return hooks
    }

    private fun collectSessionTagNamesForSelection(state: UploadPrepState): Set<String> {
        if (state.selectedAssetIds.isEmpty() || state.sessionTagsById.isEmpty()) return emptySet()
        return state.selectedAssetIds
            .mapNotNull { state.stagedEditsByAssetId[it] }
            .flatMap { patch -> patch.addTagIds }
            .mapNotNull { sessionTagId -> state.sessionTagsById[sessionTagId] }
            .toSet()
    }

    private fun collectSessionAlbumNamesForSelection(state: UploadPrepState): Set<String> {
        if (state.selectedAssetIds.isEmpty() || state.sessionAlbumsById.isEmpty()) return emptySet()
        return state.selectedAssetIds
            .mapNotNull { state.stagedEditsByAssetId[it] }
            .mapNotNull { patch -> (patch.albumId as? FieldPatch.Set<String?>)?.value }
            .mapNotNull { sessionAlbumId -> state.sessionAlbumsById[sessionAlbumId] }
            .toSet()
    }
}

private fun UploadUploadRequest.toPayloadJson(): String {
    val payload = UploadAssetPayload(
        assetData = "<binary:$localAssetId>",
        filename = fileName,
        mimeType = mimeType,
        sidecarData = sidecarData?.let { "<sidecar:${it.length}chars>" },
        deviceAssetId = deviceAssetId,
        deviceId = deviceId,
        fileCreatedAt = fileCreatedAt,
        fileModifiedAt = fileModifiedAt,
        metadata = uploadRequestJson.encodeToString(metadata)
    )
    return uploadRequestJson.encodeToString(payload)
}

private fun UploadBulkMetadataRequest.toPayloadJson(): String =
    uploadRequestJson.encodeToString(copy(ids = ids.sorted()))

private fun UploadTagAssignRequest.toPayloadJson(): String =
    uploadRequestJson.encodeToString(copy(assetIds = assetIds.sorted(), tagIds = tagIds.sorted()))

private fun UploadAlbumAddRequest.toPayloadJson(): String =
    uploadRequestJson.encodeToString(copy(assetIds = assetIds.sorted()))

private fun buildXmpSidecarData(timestamp: String, enabled: Boolean): String? {
    if (!enabled) return null
    val xmpTimestamp = toXmpDateTime(timestamp) ?: return null
    val xmpSubSecTimestamp = toXmpSubSecDateTime(xmpTimestamp)
    val exifTimestamp = toExifDateTime(xmpTimestamp) ?: return null
    val offset = extractOffsetFromXmpDateTime(xmpTimestamp)
    return """
        <?xpacket begin='﻿' id='W5M0MpCehiHzreSzNTczkc9d'?>
        <x:xmpmeta xmlns:x="adobe:ns:meta/">
          <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
            <rdf:Description rdf:about=""
              xmlns:xmp="http://ns.adobe.com/xap/1.0/"
              xmlns:exif="http://ns.adobe.com/exif/1.0/"
              xmlns:photoshop="http://ns.adobe.com/photoshop/1.0/">
              <xmp:CreateDate>$xmpTimestamp</xmp:CreateDate>
              <xmp:SubSecCreateDate>$xmpSubSecTimestamp</xmp:SubSecCreateDate>
              <xmp:DateTimeCreated>$xmpTimestamp</xmp:DateTimeCreated>
              <xmp:ModifyDate>$xmpTimestamp</xmp:ModifyDate>
              <photoshop:DateCreated>$xmpTimestamp</photoshop:DateCreated>
              <exif:DateTimeOriginal>$exifTimestamp</exif:DateTimeOriginal>
              ${if (offset != null) "<exif:OffsetTimeOriginal>$offset</exif:OffsetTimeOriginal>" else ""}
            </rdf:Description>
          </rdf:RDF>
        </x:xmpmeta>
        <?xpacket end='w'?>
    """.trimIndent()
}

private fun toXmpDateTime(isoTimestamp: String): String? {
    val value = isoTimestamp.trim()
    val match = ISO_TIMESTAMP_REGEX.matchEntire(value) ?: return null
    val year = match.groupValues[1]
    val month = match.groupValues[2]
    val day = match.groupValues[3]
    val hour = match.groupValues[4]
    val minute = match.groupValues[5]
    val second = match.groupValues[6]
    val zoneRaw = match.groupValues[7]
    val zone = when {
        zoneRaw == "Z" -> "+00:00"
        zoneRaw.isNotBlank() -> zoneRaw
        else -> return null
    }
    return "$year-$month-$day" + "T" + "$hour:$minute:$second$zone"
}

private fun toXmpSubSecDateTime(xmpTimestamp: String): String {
    if (xmpTimestamp.contains('.')) return xmpTimestamp
    val zoneStart = xmpTimestamp.length - 6
    if (zoneStart <= 0) return xmpTimestamp
    return xmpTimestamp.substring(0, zoneStart) + ".000" + xmpTimestamp.substring(zoneStart)
}

private fun toExifDateTime(xmpTimestamp: String): String? {
    val match = XMP_TIMESTAMP_REGEX.matchEntire(xmpTimestamp) ?: return null
    val year = match.groupValues[1]
    val month = match.groupValues[2]
    val day = match.groupValues[3]
    val hour = match.groupValues[4]
    val minute = match.groupValues[5]
    val second = match.groupValues[6]
    val offset = match.groupValues[7].replace(":", "")
    return "$year:$month:$day $hour:$minute:$second$offset"
}

private fun withTimezoneOffsetIfMissing(dateTime: String, timeZone: String?): String {
    val trimmedDateTime = normalizeIsoDateTime(dateTime.trim())
    if (trimmedDateTime.endsWith("Z") || OFFSET_SUFFIX_REGEX.containsMatchIn(trimmedDateTime)) return trimmedDateTime
    val normalizedTimeZone = timeZone?.trim()?.takeIf { it.isNotEmpty() } ?: return trimmedDateTime
    if (normalizedTimeZone == "Z" || OFFSET_ONLY_REGEX.matches(normalizedTimeZone)) {
        return "$trimmedDateTime$normalizedTimeZone"
    }
    if (!normalizedTimeZone.contains('/')) return trimmedDateTime
    val localDateTime = runCatching { LocalDateTime.parse(trimmedDateTime.substringBefore('.')) }.getOrNull() ?: return trimmedDateTime
    val zone = runCatching { TimeZone.of(normalizedTimeZone) }.getOrNull() ?: return trimmedDateTime
    val instantAtUtc = localDateTime.toInstant(TimeZone.UTC)
    val instantAtZone = localDateTime.toInstant(zone)
    val offsetMinutes = ((instantAtUtc.toEpochMilliseconds() - instantAtZone.toEpochMilliseconds()) / 60_000L).toInt()
    val sign = if (offsetMinutes < 0) "-" else "+"
    val absolute = kotlin.math.abs(offsetMinutes)
    val hours = absolute / 60
    val minutes = absolute % 60
    return "$trimmedDateTime$sign${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

private val OFFSET_SUFFIX_REGEX = Regex("[+-](?:[01]\\d|2[0-3]):[0-5]\\d$")
private val OFFSET_ONLY_REGEX = Regex("^[+-](?:[01]\\d|2[0-3]):[0-5]\\d$")
private val ISO_MINUTE_PRECISION_REGEX =
    Regex("^(\\d{4}-\\d{2}-\\d{2})T(\\d{2}):(\\d{2})(?::(\\d{2}))?(Z|[+-]\\d{2}:\\d{2})?$")
private val ISO_TIMESTAMP_REGEX =
    Regex("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})(Z|[+-]\\d{2}:\\d{2})?$")
private val XMP_TIMESTAMP_REGEX =
    Regex("^(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})([+-]\\d{2}:\\d{2})$")

private fun extractOffsetFromXmpDateTime(value: String): String? {
    val zone = value.takeLast(6)
    return if (OFFSET_ONLY_REGEX.matches(zone)) zone else null
}

private fun normalizeIsoDateTime(value: String): String {
    val match = ISO_MINUTE_PRECISION_REGEX.matchEntire(value) ?: return value
    val datePart = match.groupValues[1]
    val hour = match.groupValues[2]
    val minute = match.groupValues[3]
    val second = match.groupValues[4].ifBlank { "00" }
    val zone = match.groupValues[5]
    return "$datePart" + "T" + "$hour:$minute:$second$zone"
}

@Serializable
private data class UploadAssetPayload(
    val assetData: String,
    val filename: String,
    val mimeType: String,
    val sidecarData: String? = null,
    val deviceAssetId: String,
    val deviceId: String,
    val fileCreatedAt: String,
    val fileModifiedAt: String,
    val metadata: String
)

@Serializable
private data class UploadNameRequest(val name: String)

@Serializable
private data class UploadTagAssetsPayload(val ids: List<String>)
