package com.marcportabella.immichuploader.data

import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadPrepState
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal val immichJson: Json = Json {
    encodeDefaults = false
    explicitNulls = false
    ignoreUnknownKeys = true
}

fun normalizeImmichApiBaseUrl(rawValue: String): String {
    val trimmed = rawValue.trim().trimEnd('/')
    if (trimmed.isEmpty()) return ""
    val baseUrl = if (trimmed.startsWith("/")) {
        trimmed
    } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
    return if (baseUrl.endsWith("/api")) baseUrl else "$baseUrl/api"
}

fun buildImmichApiUrl(apiBaseUrl: String, endpointPath: String): String {
    val normalizedBase = normalizeImmichApiBaseUrl(apiBaseUrl).trimEnd('/')
    val normalizedPath = endpointPath.trim().removePrefix("/")
    return "$normalizedBase/$normalizedPath"
}

data class ImmichUploadRequest(
    val localAssetId: String,
    val fileName: String,
    val mimeType: String,
    val sourceFile: PlatformFile?,
    val sidecarData: String? = null,
    val deviceAssetId: String,
    val deviceId: String,
    val fileCreatedAt: String,
    val fileModifiedAt: String
)

@Serializable
data class ImmichBulkMetadataRequest(
    val ids: List<String>,
    val dateTimeOriginal: String? = null,
    val timeZone: String? = null,
    val description: String? = null,
    val isFavorite: Boolean? = null
)

@Serializable
data class ImmichTagAssignRequest(
    val assetIds: List<String>,
    val tagIds: List<String>
)

@Serializable
data class ImmichTagAssetsRequest(
    val ids: List<String>
)

@Serializable
data class ImmichAlbumAddRequest(
    val albumId: String,
    val assetIds: List<String>
)

@Serializable
data class ImmichAlbumCreateRequest(val name: String)

@Serializable
data class ImmichTagCreateRequest(val name: String)

@Serializable
data class ImmichBulkUploadCheckItem(
    val id: String,
    val checksum: String,
    val originalPath: String? = null,
    val relativePath: String? = null
)

@Serializable
data class ImmichBulkUploadCheckRequest(
    val assets: List<ImmichBulkUploadCheckItem>
)

sealed interface ImmichApiBody

data class ImmichUploadBody(val payload: ImmichUploadPayload) : ImmichApiBody

data class ImmichBulkMetadataBody(val payload: ImmichBulkMetadataRequest) : ImmichApiBody

data class ImmichTagAssignBody(val payload: ImmichTagAssignRequest) : ImmichApiBody

data class ImmichTagAssetsBody(val payload: ImmichTagAssetsRequest) : ImmichApiBody

data class ImmichAlbumAddBody(val payload: ImmichAlbumAddRequest) : ImmichApiBody

data class ImmichAlbumCreateBody(val payload: ImmichAlbumCreateRequest) : ImmichApiBody

data class ImmichTagCreateBody(val payload: ImmichTagCreateRequest) : ImmichApiBody

data class ImmichBulkUploadCheckBody(val payload: ImmichBulkUploadCheckRequest) : ImmichApiBody

data class ImmichApiRequest(
    val method: String,
    val url: String,
    val body: ImmichApiBody? = null
)

object ImmichCatalogRequestBuilder {
    fun lookupAlbums(apiBaseUrl: String): ImmichApiRequest =
        ImmichApiRequest(
            method = "GET",
            url = buildImmichApiUrl(apiBaseUrl, "albums")
        )

    fun lookupTags(apiBaseUrl: String): ImmichApiRequest =
        ImmichApiRequest(
            method = "GET",
            url = buildImmichApiUrl(apiBaseUrl, "tags")
        )

    fun lookupCurrentUser(apiBaseUrl: String): ImmichApiRequest =
        ImmichApiRequest(
            method = "GET",
            url = buildImmichApiUrl(apiBaseUrl, "users/me")
        )

    fun createAlbum(apiBaseUrl: String, name: String): ImmichApiRequest =
        ImmichApiRequest(
            method = "POST",
            url = buildImmichApiUrl(apiBaseUrl, "albums"),
            body = ImmichAlbumCreateBody(ImmichAlbumCreateRequest(name.trim()))
        )

    fun createTag(apiBaseUrl: String, name: String): ImmichApiRequest =
        ImmichApiRequest(
            method = "POST",
            url = buildImmichApiUrl(apiBaseUrl, "tags"),
            body = ImmichTagCreateBody(ImmichTagCreateRequest(name.trim()))
        )

    fun bulkUploadCheck(apiBaseUrl: String, items: List<ImmichBulkUploadCheckItem>): ImmichApiRequest =
        ImmichApiRequest(
            method = "POST",
            url = buildImmichApiUrl(apiBaseUrl, "assets/bulk-upload-check"),
            body = ImmichBulkUploadCheckBody(
                ImmichBulkUploadCheckRequest(
                    assets = items.distinctBy { it.id }.sortedBy { it.id }
                )
            )
        )
}

sealed interface ImmichLookupHook {
    data object LookupAlbums : ImmichLookupHook
    data object LookupTags : ImmichLookupHook
    data class CreateAlbumIfMissing(val name: String) : ImmichLookupHook
    data class CreateTagIfMissing(val name: String) : ImmichLookupHook
}

data class ImmichRequestPlan(
    val uploadRequests: List<ImmichUploadRequest> = emptyList(),
    val bulkMetadataRequests: List<ImmichBulkMetadataRequest> = emptyList(),
    val tagAssignRequests: List<ImmichTagAssignRequest> = emptyList(),
    val albumAddRequests: List<ImmichAlbumAddRequest> = emptyList(),
    val lookupHooks: List<ImmichLookupHook> = emptyList(),
    val sessionAlbumsById: Map<String, String> = emptyMap(),
    val sessionTagsById: Map<String, String> = emptyMap()
)

object ImmichRequestBuilder {
    private const val FALLBACK_TIMESTAMP = "1970-01-01T00:00:00Z"
    private const val DEFAULT_DEVICE_ID = "web-local-device"

    fun buildUploadRequest(
        asset: LocalAsset,
        deviceId: String,
        patch: AssetEditPatch? = null
    ): ImmichUploadRequest {
        val patchDateTime = (patch?.dateTimeOriginal as? FieldPatch.Set<String>)?.value
        val patchTimeZone = (patch?.timeZone as? FieldPatch.Set<String>)?.value
        val timestampBase = patchDateTime ?: asset.captureDateTime ?: FALLBACK_TIMESTAMP
        val timestamp = withTimezoneOffsetIfMissing(timestampBase, patchTimeZone)
        return ImmichUploadRequest(
            localAssetId = asset.id.value,
            fileName = asset.fileName,
            mimeType = asset.mimeType,
            sourceFile = asset.sourceFile,
            sidecarData = buildXmpSidecarData(timestamp, patchDateTime != null || patchTimeZone != null),
            deviceAssetId = asset.id.value,
            deviceId = deviceId,
            fileCreatedAt = timestamp,
            fileModifiedAt = timestamp
        )
    }

    fun buildBulkMetadataRequest(assetIds: Set<String>, patch: AssetEditPatch): ImmichBulkMetadataRequest? {
        if (assetIds.isEmpty()) return null

        val description = (patch.description as? FieldPatch.Set<String?>)?.value
        val isFavorite = (patch.isFavorite as? FieldPatch.Set<Boolean>)?.value
        val dateTimeOriginal = (patch.dateTimeOriginal as? FieldPatch.Set<String>)?.value
        val timeZone = (patch.timeZone as? FieldPatch.Set<String>)?.value

        if (description == null && isFavorite == null && dateTimeOriginal == null && timeZone == null) {
            return null
        }

        return ImmichBulkMetadataRequest(
            ids = assetIds.toList().sorted(),
            dateTimeOriginal = dateTimeOriginal,
            timeZone = timeZone,
            description = description,
            isFavorite = isFavorite
        )
    }

    fun buildTagAssignRequest(assetIds: Set<String>, patch: AssetEditPatch): ImmichTagAssignRequest? {
        if (assetIds.isEmpty() || patch.addTagIds.isEmpty()) return null
        return ImmichTagAssignRequest(
            assetIds = assetIds.toList().sorted(),
            tagIds = patch.addTagIds.toList().sorted()
        )
    }

    fun buildAlbumAddRequest(assetIds: Set<String>, patch: AssetEditPatch): ImmichAlbumAddRequest? {
        if (assetIds.isEmpty()) return null
        val albumId = (patch.albumId as? FieldPatch.Set<String?>)?.value ?: return null
        if (albumId.isBlank()) return null

        return ImmichAlbumAddRequest(
            albumId = albumId,
            assetIds = assetIds.toList().sorted()
        )
    }

    fun buildLookupHooks(
        albumsToCreate: Set<String>,
        tagsToCreate: Set<String>
    ): List<ImmichLookupHook> {
        val hooks = mutableListOf<ImmichLookupHook>()
        albumsToCreate.map { it.trim() }.filter { it.isNotEmpty() }.sorted().forEach {
            hooks += ImmichLookupHook.CreateAlbumIfMissing(it)
        }
        tagsToCreate.map { it.trim() }.filter { it.isNotEmpty() }.sorted().forEach {
            hooks += ImmichLookupHook.CreateTagIfMissing(it)
        }
        return hooks
    }

    fun buildDryRunPlan(
        state: UploadPrepState,
        deviceId: String = DEFAULT_DEVICE_ID
    ): ImmichRequestPlan {
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

        val tagAssignRequests = mutableListOf<ImmichTagAssignRequest>()
        val albumAddRequests = mutableListOf<ImmichAlbumAddRequest>()
        val bulkMetadataRequests = mutableListOf<ImmichBulkMetadataRequest>()

        remoteIdsByPatch.forEach { (patch, remoteIds) ->
            buildBulkMetadataRequest(remoteIds, patch)?.let { bulkMetadataRequests += it }
            buildTagAssignRequest(remoteIds, patch)?.let { tagAssignRequests += it }
            buildAlbumAddRequest(remoteIds, patch)?.let { albumAddRequests += it }
        }

        val lookupHooks = buildLookupHooks(
            albumsToCreate = collectSessionAlbumNamesForSelection(state) + setOf(state.albumCreateDraft),
            tagsToCreate = collectSessionTagNamesForSelection(state) + setOf(state.tagCreateDraft)
        )

        return ImmichRequestPlan(
            uploadRequests = uploadRequests,
            bulkMetadataRequests = bulkMetadataRequests,
            tagAssignRequests = tagAssignRequests,
            albumAddRequests = albumAddRequests,
            lookupHooks = lookupHooks,
            sessionAlbumsById = state.sessionAlbumsById,
            sessionTagsById = state.sessionTagsById
        )
    }

    fun buildPayloadInspectorRequests(plan: ImmichRequestPlan, apiBaseUrl: String): List<ImmichApiRequest> {
        val requests = mutableListOf<ImmichApiRequest>()

        plan.lookupHooks.forEach { hook ->
            requests += when (hook) {
                ImmichLookupHook.LookupAlbums -> ImmichCatalogRequestBuilder.lookupAlbums(apiBaseUrl)
                ImmichLookupHook.LookupTags -> ImmichCatalogRequestBuilder.lookupTags(apiBaseUrl)
                is ImmichLookupHook.CreateAlbumIfMissing -> ImmichCatalogRequestBuilder.createAlbum(apiBaseUrl, hook.name)
                is ImmichLookupHook.CreateTagIfMissing -> ImmichCatalogRequestBuilder.createTag(apiBaseUrl, hook.name)
            }
        }

        plan.uploadRequests.forEach { request ->
            requests += ImmichApiRequest(
                method = "POST",
                url = buildImmichApiUrl(apiBaseUrl, "assets"),
                body = request.toApiBody()
            )
        }

        plan.bulkMetadataRequests.forEach { request ->
            requests += ImmichApiRequest(
                method = "PUT",
                url = buildImmichApiUrl(apiBaseUrl, "assets/updateAssets"),
                body = request.toApiBody()
            )
        }

        plan.tagAssignRequests.forEach { request ->
            val assetIds = request.assetIds.sorted()
            request.tagIds.sorted().forEach { tagId ->
                requests += ImmichApiRequest(
                    method = "PUT",
                    url = buildImmichApiUrl(apiBaseUrl, "tags/$tagId/assets"),
                    body = ImmichTagAssetsBody(ImmichTagAssetsRequest(ids = assetIds))
                )
            }
        }

        plan.albumAddRequests.forEach { request ->
            requests += ImmichApiRequest(
                method = "PUT",
                url = buildImmichApiUrl(apiBaseUrl, "albums/assets"),
                body = request.toApiBody()
            )
        }

        return requests
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

private fun ImmichUploadRequest.toApiBody(): ImmichUploadBody =
    ImmichUploadBody(
        payload = ImmichUploadPayload(
            localAssetId = localAssetId,
            fileName = fileName,
            mimeType = mimeType,
            sourceFile = sourceFile,
            sidecarData = sidecarData,
            deviceAssetId = deviceAssetId,
            deviceId = deviceId,
            fileCreatedAt = fileCreatedAt,
            fileModifiedAt = fileModifiedAt
        )
    )

private fun ImmichBulkMetadataRequest.toApiBody(): ImmichBulkMetadataBody =
    ImmichBulkMetadataBody(payload = copy(ids = ids.sorted()))

private fun ImmichTagAssignRequest.toApiBody(): ImmichTagAssignBody =
    ImmichTagAssignBody(payload = copy(assetIds = assetIds.sorted(), tagIds = tagIds.sorted()))

private fun ImmichAlbumAddRequest.toApiBody(): ImmichAlbumAddBody =
    ImmichAlbumAddBody(payload = copy(assetIds = assetIds.sorted()))

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

fun parseExistingAssetsByItemId(
    responseBody: String,
    requestedItems: List<ImmichBulkUploadCheckItem>
): Map<String, String> {
    val root = runCatching { immichJson.parseToJsonElement(responseBody) }.getOrNull() ?: return emptyMap()

    val existingAssets = (root as? JsonObject)
        ?.get("existingAssets")
        ?.jsonObject
        ?.mapNotNull { (checksum, value) ->
            val assetId = value.jsonPrimitive.contentOrNull?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            checksum to assetId
        }
        ?.toMap()
    if (!existingAssets.isNullOrEmpty()) {
        val idByChecksum = requestedItems.associate { it.checksum to it.id }
        return existingAssets.mapNotNull { (checksum, assetId) ->
            val itemId = idByChecksum[checksum] ?: return@mapNotNull null
            itemId to assetId
        }.toMap()
    }

    val results = (root as? JsonObject)?.get("results") as? JsonArray ?: return emptyMap()
    return results.mapNotNull { element ->
        val obj = element as? JsonObject ?: return@mapNotNull null
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val assetId = obj["assetId"]?.jsonPrimitive?.contentOrNull
            ?: return@mapNotNull null
        id to assetId
    }.toMap()
}

data class ImmichUploadPayload(
    val localAssetId: String,
    val fileName: String,
    val mimeType: String,
    val sourceFile: PlatformFile?,
    val sidecarData: String? = null,
    val deviceAssetId: String,
    val deviceId: String,
    val fileCreatedAt: String,
    val fileModifiedAt: String
) {
    override fun toString(): String =
        "ImmichUploadPayload(" +
            "assetData=<binary:$localAssetId>, " +
            "fileName=$fileName, " +
            "mimeType=$mimeType, " +
            "sourceFilePresent=${sourceFile != null}, " +
            "sidecarDataPresent=${sidecarData != null}, " +
            "deviceAssetId=$deviceAssetId, " +
            "deviceId=$deviceId, " +
            "fileCreatedAt=$fileCreatedAt, " +
            "fileModifiedAt=$fileModifiedAt" +
            ")"
}

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
