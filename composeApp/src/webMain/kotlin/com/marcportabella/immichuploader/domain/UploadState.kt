package com.marcportabella.immichuploader.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.marcportabella.immichuploader.data.ImmichApiRequest
import com.marcportabella.immichuploader.data.ImmichCatalogEntry
import com.marcportabella.immichuploader.data.ImmichRequestBuilder
import com.marcportabella.immichuploader.data.ImmichRequestPlan

data class BulkEditDraft(
    val includeDescription: Boolean = false,
    val description: String = "",
    val includeFavorite: Boolean = false,
    val isFavorite: Boolean = false,
    val includeDateTimeOriginal: Boolean = false,
    val dateTimeOriginal: String = "",
    val includeAlbumId: Boolean = false,
    val albumId: String = "",
    val addTagIds: String = "",
    val removeTagIds: String = ""
)

data class UploadPrepState(
    val assets: Map<LocalAssetId, LocalAsset> = emptyMap(),
    val selectedAssetIds: Set<LocalAssetId> = emptySet(),
    val stagedEditsByAssetId: Map<LocalAssetId, AssetEditPatch> = emptyMap(),
    val bulkEditDraft: BulkEditDraft = BulkEditDraft(),
    val apiKey: String = "",
    val albumCreateDraft: String = "",
    val tagCreateDraft: String = "",
    val availableAlbums: List<ImmichCatalogEntry> = emptyList(),
    val availableTags: List<ImmichCatalogEntry> = emptyList(),
    val catalogStatus: CatalogUiStatus = CatalogUiStatus.Idle,
    val catalogMessage: String? = null,
    val dryRunPlan: ImmichRequestPlan? = null,
    val dryRunApiRequests: List<ImmichApiRequest> = emptyList(),
    val dryRunMessage: String? = null,
    val executionStatus: UploadExecutionStatus = UploadExecutionStatus.Idle,
    val executionMessage: String? = null,
    val executionRequestCount: Int? = null,
    val batchFeedback: BatchFeedback? = null
)

data class BatchFeedback(
    val level: BatchFeedbackLevel,
    val message: String
)

enum class BatchFeedbackLevel {
    Error,
    Warning,
    Success
}

enum class CatalogUiStatus {
    Idle,
    Loading,
    Ready,
    BlockedMissingApiKey
}

enum class UploadExecutionStatus {
    Idle,
    Executing,
    BlockedMissingApiKey,
    Submitted,
    Failed
}

sealed interface UploadPrepAction {
    data class ReplaceAssets(val assets: List<LocalAsset>) : UploadPrepAction
    data class ToggleSelection(val assetId: LocalAssetId) : UploadPrepAction
    data class SetSelection(val assetIds: Set<LocalAssetId>) : UploadPrepAction
    data object SelectAll : UploadPrepAction
    data object ClearSelection : UploadPrepAction
    data class StageEditForSelected(val patch: AssetEditPatch) : UploadPrepAction
    data class StageEditForAsset(val assetId: LocalAssetId, val patch: AssetEditPatch) : UploadPrepAction
    data object ClearStagedForSelected : UploadPrepAction
    data class SetBulkEditDraft(val draft: BulkEditDraft) : UploadPrepAction
    data object ApplyBulkEditDraftToSelected : UploadPrepAction
    data object ClearBulkEditDraft : UploadPrepAction
    data class SetApiKey(val value: String) : UploadPrepAction
    data class SetAlbumCreateDraft(val value: String) : UploadPrepAction
    data class SetTagCreateDraft(val value: String) : UploadPrepAction
    data object CatalogRequestStarted : UploadPrepAction
    data class CatalogAlbumsLoaded(val albums: List<ImmichCatalogEntry>, val message: String) : UploadPrepAction
    data class CatalogTagsLoaded(val tags: List<ImmichCatalogEntry>, val message: String) : UploadPrepAction
    data class CatalogBlockedMissingApiKey(val message: String) : UploadPrepAction
    data object ClearCatalogMessage : UploadPrepAction
    data class DryRunPreviewGenerated(
        val plan: ImmichRequestPlan,
        val requests: List<ImmichApiRequest>,
        val message: String
    ) : UploadPrepAction
    data object GenerateDryRunPreview : UploadPrepAction
    data object ClearDryRunPreview : UploadPrepAction
    data class UploadExecutionStarted(val message: String) : UploadPrepAction
    data class UploadExecutionBlocked(val message: String) : UploadPrepAction
    data class UploadExecutionSubmitted(val requestCount: Int, val message: String) : UploadPrepAction
    data class UploadExecutionFailed(val message: String) : UploadPrepAction
    data object ClearUploadExecutionStatus : UploadPrepAction
    data object ClearBatchFeedback : UploadPrepAction
}

fun reduceUploadPrepState(state: UploadPrepState, action: UploadPrepAction): UploadPrepState =
    when (action) {
        is UploadPrepAction.ReplaceAssets -> {
            val nextAssets = action.assets.associateBy { it.id }
            val validIds = nextAssets.keys
            state.copy(
                assets = nextAssets,
                selectedAssetIds = state.selectedAssetIds.intersect(validIds),
                stagedEditsByAssetId = state.stagedEditsByAssetId.filterKeys { it in validIds },
                executionStatus = UploadExecutionStatus.Idle,
                executionMessage = null,
                executionRequestCount = null,
                batchFeedback = null
            )
        }

        is UploadPrepAction.ToggleSelection -> {
            if (action.assetId !in state.assets) return state
            val nextSelection = state.selectedAssetIds.toMutableSet()
            if (!nextSelection.add(action.assetId)) {
                nextSelection.remove(action.assetId)
            }
            state.copy(selectedAssetIds = nextSelection, batchFeedback = null)
        }

        is UploadPrepAction.SetSelection -> state.copy(
            selectedAssetIds = action.assetIds.intersect(state.assets.keys),
            batchFeedback = null
        )

        UploadPrepAction.SelectAll -> state.copy(selectedAssetIds = state.assets.keys, batchFeedback = null)

        UploadPrepAction.ClearSelection -> state.copy(selectedAssetIds = emptySet(), batchFeedback = null)

        is UploadPrepAction.StageEditForSelected ->
            stagePatchForIds(state, state.selectedAssetIds, action.patch)

        is UploadPrepAction.StageEditForAsset -> {
            if (action.assetId !in state.assets) return state
            stagePatchForIds(state, setOf(action.assetId), action.patch)
        }

        UploadPrepAction.ClearStagedForSelected -> {
            if (state.selectedAssetIds.isEmpty()) {
                state.copy(
                    batchFeedback = BatchFeedback(
                        level = BatchFeedbackLevel.Warning,
                        message = "No selected assets to clear."
                    )
                )
            } else {
                state.copy(
                    stagedEditsByAssetId = state.stagedEditsByAssetId.filterKeys { it !in state.selectedAssetIds },
                    batchFeedback = BatchFeedback(
                        level = BatchFeedbackLevel.Success,
                        message = "Cleared staged edits for ${state.selectedAssetIds.size} selected assets."
                    )
                )
            }
        }

        is UploadPrepAction.SetBulkEditDraft -> state.copy(
            bulkEditDraft = action.draft,
            batchFeedback = null
        )

        UploadPrepAction.ApplyBulkEditDraftToSelected -> {
            val preflightFeedback = preflightBulkEditDraft(state)
            if (preflightFeedback != null) {
                state.copy(batchFeedback = preflightFeedback)
            } else {
                val patch = state.bulkEditDraft.toPatch()
                    ?: return state.copy(
                        batchFeedback = BatchFeedback(
                            level = BatchFeedbackLevel.Warning,
                            message = "No bulk fields selected to apply."
                        )
                    )

                stagePatchForIds(state, state.selectedAssetIds, patch).copy(
                    batchFeedback = BatchFeedback(
                        level = BatchFeedbackLevel.Success,
                        message = "Applied bulk edits to ${state.selectedAssetIds.size} selected assets."
                    )
                )
            }
        }

        UploadPrepAction.ClearBulkEditDraft -> state.copy(
            bulkEditDraft = BulkEditDraft(),
            batchFeedback = null
        )

        is UploadPrepAction.SetApiKey -> state.copy(
            apiKey = action.value,
            executionStatus = UploadExecutionStatus.Idle,
            executionMessage = null,
            executionRequestCount = null
        )

        is UploadPrepAction.SetAlbumCreateDraft -> state.copy(albumCreateDraft = action.value)

        is UploadPrepAction.SetTagCreateDraft -> state.copy(tagCreateDraft = action.value)

        UploadPrepAction.CatalogRequestStarted -> state.copy(
            catalogStatus = CatalogUiStatus.Loading,
            catalogMessage = null
        )

        is UploadPrepAction.CatalogAlbumsLoaded -> state.copy(
            availableAlbums = action.albums.sortedBy { it.name.lowercase() },
            catalogStatus = CatalogUiStatus.Ready,
            catalogMessage = action.message
        )

        is UploadPrepAction.CatalogTagsLoaded -> state.copy(
            availableTags = action.tags.sortedBy { it.name.lowercase() },
            catalogStatus = CatalogUiStatus.Ready,
            catalogMessage = action.message
        )

        is UploadPrepAction.CatalogBlockedMissingApiKey -> state.copy(
            catalogStatus = CatalogUiStatus.BlockedMissingApiKey,
            catalogMessage = action.message
        )

        UploadPrepAction.ClearCatalogMessage -> state.copy(catalogMessage = null)

        is UploadPrepAction.DryRunPreviewGenerated -> state.copy(
            dryRunPlan = action.plan,
            dryRunApiRequests = action.requests,
            dryRunMessage = action.message,
            executionStatus = UploadExecutionStatus.Idle,
            executionMessage = null,
            executionRequestCount = null,
            batchFeedback = BatchFeedback(
                level = if (action.requests.isEmpty()) BatchFeedbackLevel.Warning else BatchFeedbackLevel.Success,
                message = action.message
            )
        )

        UploadPrepAction.GenerateDryRunPreview -> {
            val preflightFeedback = preflightDryRun(state)
            if (preflightFeedback != null) {
                state.copy(
                    dryRunPlan = null,
                    dryRunApiRequests = emptyList(),
                    dryRunMessage = preflightFeedback.message,
                    executionStatus = UploadExecutionStatus.Idle,
                    executionMessage = null,
                    executionRequestCount = null,
                    batchFeedback = preflightFeedback
                )
            } else {
                val plan = ImmichRequestBuilder.buildDryRunPlan(state)
                val requests = ImmichRequestBuilder.buildPayloadInspectorRequests(plan)
                val feedback = if (requests.isEmpty()) {
                    BatchFeedback(
                        level = BatchFeedbackLevel.Warning,
                        message = "No operations planned. Select assets and/or stage edits first."
                    )
                } else {
                    BatchFeedback(
                        level = BatchFeedbackLevel.Success,
                        message = "Dry-run generated ${requests.size} operations."
                    )
                }
                state.copy(
                    dryRunPlan = plan,
                    dryRunApiRequests = requests,
                    dryRunMessage = feedback.message,
                    executionStatus = UploadExecutionStatus.Idle,
                    executionMessage = null,
                    executionRequestCount = null,
                    batchFeedback = feedback
                )
            }
        }

        UploadPrepAction.ClearDryRunPreview -> state.copy(
            dryRunPlan = null,
            dryRunApiRequests = emptyList(),
            dryRunMessage = null,
            executionStatus = UploadExecutionStatus.Idle,
            executionMessage = null,
            executionRequestCount = null,
            batchFeedback = null
        )

        is UploadPrepAction.UploadExecutionStarted -> state.copy(
            executionStatus = UploadExecutionStatus.Executing,
            executionMessage = action.message,
            executionRequestCount = null
        )

        is UploadPrepAction.UploadExecutionBlocked -> state.copy(
            executionStatus = UploadExecutionStatus.BlockedMissingApiKey,
            executionMessage = action.message,
            executionRequestCount = null
        )

        is UploadPrepAction.UploadExecutionSubmitted -> state.copy(
            executionStatus = UploadExecutionStatus.Submitted,
            executionMessage = action.message,
            executionRequestCount = action.requestCount
        )

        is UploadPrepAction.UploadExecutionFailed -> state.copy(
            executionStatus = UploadExecutionStatus.Failed,
            executionMessage = action.message,
            executionRequestCount = null
        )

        UploadPrepAction.ClearUploadExecutionStatus -> state.copy(
            executionStatus = UploadExecutionStatus.Idle,
            executionMessage = null,
            executionRequestCount = null
        )

        UploadPrepAction.ClearBatchFeedback -> state.copy(batchFeedback = null)
    }

private fun stagePatchForIds(
    state: UploadPrepState,
    targetIds: Set<LocalAssetId>,
    patch: AssetEditPatch
): UploadPrepState {
    if (targetIds.isEmpty()) return state

    val nextStaged = state.stagedEditsByAssetId.toMutableMap()
    targetIds.forEach { assetId ->
        val current = nextStaged[assetId]
        nextStaged[assetId] = if (current == null) patch else current.merge(patch)
    }

    return state.copy(stagedEditsByAssetId = nextStaged)
}

private fun BulkEditDraft.toPatch(): AssetEditPatch? {
    val addTags = parseTagList(addTagIds)
    val removeTags = parseTagList(removeTagIds)

    val patch = AssetEditPatch(
        description = if (includeDescription) FieldPatch.Set(description.ifBlank { null }) else FieldPatch.Unset,
        isFavorite = if (includeFavorite) FieldPatch.Set(isFavorite) else FieldPatch.Unset,
        dateTimeOriginal = if (includeDateTimeOriginal && dateTimeOriginal.isNotBlank()) {
            FieldPatch.Set(dateTimeOriginal)
        } else {
            FieldPatch.Unset
        },
        albumId = if (includeAlbumId) FieldPatch.Set(albumId.ifBlank { null }) else FieldPatch.Unset,
        addTagIds = addTags,
        removeTagIds = removeTags
    )

    return if (
        patch.description is FieldPatch.Unset &&
        patch.isFavorite is FieldPatch.Unset &&
        patch.dateTimeOriginal is FieldPatch.Unset &&
        patch.albumId is FieldPatch.Unset &&
        patch.addTagIds.isEmpty() &&
        patch.removeTagIds.isEmpty()
    ) {
        null
    } else {
        patch
    }
}

fun preflightBulkEditDraft(state: UploadPrepState): BatchFeedback? {
    if (state.selectedAssetIds.isEmpty()) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Error,
            message = "Select at least one asset before applying bulk edits."
        )
    }

    val draft = state.bulkEditDraft
    if (draft.includeDateTimeOriginal && draft.dateTimeOriginal.isBlank()) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Error,
            message = "Date/time is required when date/time edit is enabled."
        )
    }

    if (draft.includeDateTimeOriginal && !isIsoUtcDateTime(draft.dateTimeOriginal)) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Error,
            message = "Date/time must use ISO 8601 UTC format: YYYY-MM-DDTHH:MM:SSZ."
        )
    }

    val addTags = parseTagList(draft.addTagIds)
    val removeTags = parseTagList(draft.removeTagIds)
    val conflictingTags = addTags.intersect(removeTags)
    if (conflictingTags.isNotEmpty()) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Error,
            message = "Tag IDs cannot be both added and removed: ${conflictingTags.sorted().joinToString(", ")}."
        )
    }

    if (draft.toPatch() == null) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Warning,
            message = "No bulk fields selected to apply."
        )
    }

    return null
}

fun canApplyBulkEdit(state: UploadPrepState): Boolean =
    preflightBulkEditDraft(state) == null

private fun preflightDryRun(state: UploadPrepState): BatchFeedback? {
    if (state.selectedAssetIds.isEmpty()) {
        return BatchFeedback(
            level = BatchFeedbackLevel.Error,
            message = "Select at least one asset before generating a dry-run plan."
        )
    }

    state.selectedAssetIds.forEach { assetId ->
        val patch = state.stagedEditsByAssetId[assetId] ?: return@forEach
        val dateTime = (patch.dateTimeOriginal as? FieldPatch.Set<String>)?.value ?: return@forEach
        if (!isIsoUtcDateTime(dateTime)) {
            return BatchFeedback(
                level = BatchFeedbackLevel.Error,
                message = "One or more staged date/time values are invalid. Use YYYY-MM-DDTHH:MM:SSZ."
            )
        }
    }

    return null
}

private fun parseTagList(value: String): Set<String> =
    value.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

private fun isIsoUtcDateTime(value: String): Boolean {
    val isoUtcRegex = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$""")
    if (!isoUtcRegex.matches(value)) return false

    val year = value.substring(0, 4).toIntOrNull() ?: return false
    val month = value.substring(5, 7).toIntOrNull() ?: return false
    val day = value.substring(8, 10).toIntOrNull() ?: return false
    val hour = value.substring(11, 13).toIntOrNull() ?: return false
    val minute = value.substring(14, 16).toIntOrNull() ?: return false
    val second = value.substring(17, 19).toIntOrNull() ?: return false

    return year >= 1900 &&
        month in 1..12 &&
        day in 1..31 &&
        hour in 0..23 &&
        minute in 0..59 &&
        second in 0..59
}

class UploadPrepStore(initialState: UploadPrepState = UploadPrepState()) {
    var state by mutableStateOf(initialState)
        private set

    fun dispatch(action: UploadPrepAction) {
        state = reduceUploadPrepState(state, action)
    }
}

data class LocalIntakeFile(
    val name: String,
    val type: String,
    val size: Long,
    val lastModifiedEpochMillis: Long,
    val previewUrl: String?,
    val previewBytes: ByteArray? = null,
    val captureDateTime: String? = null,
    val timeZone: String? = null,
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val exifMetadata: Map<String, String> = emptyMap(),
    val exifSummary: String? = null
)

data class ParsedExifMetadata(
    val captureDateTime: String?,
    val timeZone: String?,
    val cameraMake: String?,
    val cameraModel: String?,
    val metadata: Map<String, String>
)

fun mapLocalIntakeFilesToAssets(files: List<LocalIntakeFile>): List<LocalAsset> =
    files.mapIndexed { index, file ->
        val normalizedType = file.type.ifBlank { "application/octet-stream" }
        LocalAsset(
            id = LocalAssetId(
                "local-${file.name}-${file.size}-${file.lastModifiedEpochMillis}-$index"
            ),
            fileName = file.name,
            mimeType = normalizedType,
            fileSizeBytes = file.size,
            previewUrl = file.previewUrl,
            captureDateTime = file.captureDateTime,
            timeZone = file.timeZone,
            previewBytes = file.previewBytes,
            cameraMake = file.cameraMake,
            cameraModel = file.cameraModel,
            exifMetadata = file.exifMetadata,
            exifSummary = file.exifSummary
        )
    }

fun parseJpegExifMetadata(bytes: ByteArray): ParsedExifMetadata? {
    if (bytes.size < 4 || bytes[0].u8() != 0xFF || bytes[1].u8() != 0xD8) return null

    var offset = 2
    while (offset + 4 <= bytes.size) {
        if (bytes[offset].u8() != 0xFF) {
            offset += 1
            continue
        }

        val marker = bytes[offset + 1].u8()
        if (marker == 0xD9 || marker == 0xDA) break
        if (offset + 4 > bytes.size) break

        val segmentLength = ((bytes[offset + 2].u8() shl 8) or bytes[offset + 3].u8())
        if (segmentLength < 2 || offset + 2 + segmentLength > bytes.size) break

        if (marker == 0xE1 && segmentLength >= 8) {
            val exifStart = offset + 4
            val signatureMatches =
                bytes[exifStart].u8() == 0x45 &&
                    bytes[exifStart + 1].u8() == 0x78 &&
                    bytes[exifStart + 2].u8() == 0x69 &&
                    bytes[exifStart + 3].u8() == 0x66 &&
                    bytes[exifStart + 4].u8() == 0x00 &&
                    bytes[exifStart + 5].u8() == 0x00
            if (signatureMatches) {
                return parseTiffExif(
                    bytes = bytes,
                    tiffStart = exifStart + 6,
                    tiffLength = segmentLength - 8
                )
            }
        }

        offset += 2 + segmentLength
    }

    return null
}

private fun parseTiffExif(
    bytes: ByteArray,
    tiffStart: Int,
    tiffLength: Int
): ParsedExifMetadata? {
    if (tiffLength < 8 || tiffStart < 0 || tiffStart + tiffLength > bytes.size) return null

    val littleEndian = when {
        bytes[tiffStart].u8() == 0x49 && bytes[tiffStart + 1].u8() == 0x49 -> true
        bytes[tiffStart].u8() == 0x4D && bytes[tiffStart + 1].u8() == 0x4D -> false
        else -> return null
    }
    val reader = ExifReader(bytes, littleEndian)
    if (reader.u16(tiffStart + 2) != 42) return null
    val ifd0Offset = reader.u32(tiffStart + 4) ?: return null

    val ifd0 = parseIfd(
        bytes = bytes,
        reader = reader,
        tiffStart = tiffStart,
        tiffLength = tiffLength,
        ifdOffset = ifd0Offset
    )
    val exifIfdOffset = ifd0.pointers[EXIF_IFD_POINTER_TAG]
    val exifIfd = exifIfdOffset?.let {
        parseIfd(
            bytes = bytes,
            reader = reader,
            tiffStart = tiffStart,
            tiffLength = tiffLength,
            ifdOffset = it
        )
    }
    val gpsIfdOffset = ifd0.pointers[GPS_INFO_POINTER_TAG]
    val gpsIfd = gpsIfdOffset?.let {
        parseIfd(
            bytes = bytes,
            reader = reader,
            tiffStart = tiffStart,
            tiffLength = tiffLength,
            ifdOffset = it
        )
    }

    val dateTimeOriginalRaw = exifIfd?.values?.get(DATE_TIME_ORIGINAL_TAG) ?: ifd0.values[DATE_TIME_TAG]
    val timezoneRawFromOffsetTags =
        exifIfd?.values?.get(OFFSET_TIME_ORIGINAL_TAG) ?: exifIfd?.values?.get(OFFSET_TIME_TAG)
    val timezoneRawFromTimeZoneOffsetTag =
        exifIfd?.values?.get(TIME_ZONE_OFFSET_TAG) ?: ifd0.values[TIME_ZONE_OFFSET_TAG]
    val timezoneRawFromGps =
        inferTimeZoneOffsetFromGps(
            dateTimeOriginal = dateTimeOriginalRaw,
            gpsDateStamp = gpsIfd?.values?.get(GPS_DATE_STAMP_TAG),
            gpsTimeStamp = gpsIfd?.values?.get(GPS_TIME_STAMP_TAG)
        )
    val cameraMake = ifd0.values[MAKE_TAG]
    val cameraModel = ifd0.values[MODEL_TAG]
    val parsedDateTimeOriginal = dateTimeOriginalRaw?.let(::parseExifDateTimeAndOffset)

    val metadata = linkedMapOf<String, String>()
    exifIfd?.values?.get(DATE_TIME_DIGITIZED_TAG)?.let { metadata["dateTimeDigitized"] = normalizeExifDateTime(it) ?: it }
    exifIfd?.values?.get(LENS_MODEL_TAG)?.let { metadata["lensModel"] = it }
    exifIfd?.values?.get(ISO_SPEED_TAG)?.let { metadata["iso"] = it }
    exifIfd?.values?.get(EXPOSURE_TIME_TAG)?.let { metadata["exposureTime"] = it }
    exifIfd?.values?.get(F_NUMBER_TAG)?.let { metadata["fNumber"] = it }
    exifIfd?.values?.get(FOCAL_LENGTH_TAG)?.let { metadata["focalLengthMm"] = it }
    ifd0.values[SOFTWARE_TAG]?.let { metadata["software"] = it }
    gpsIfd?.values?.get(GPS_DATE_STAMP_TAG)?.let { metadata["gpsDateStamp"] = it }
    gpsIfd?.values?.get(GPS_TIME_STAMP_TAG)?.let { metadata["gpsTimeStamp"] = it }

    val hasUsefulData =
        dateTimeOriginalRaw != null ||
            timezoneRawFromOffsetTags != null ||
            timezoneRawFromTimeZoneOffsetTag != null ||
            timezoneRawFromGps != null ||
            parsedDateTimeOriginal?.second != null ||
            cameraMake != null ||
            cameraModel != null ||
            metadata.isNotEmpty()

    if (!hasUsefulData) return null

    return ParsedExifMetadata(
        captureDateTime = parsedDateTimeOriginal?.first ?: dateTimeOriginalRaw?.let(::normalizeExifDateTime) ?: dateTimeOriginalRaw,
        timeZone = timezoneRawFromOffsetTags?.let(::normalizeTimeZoneOffset)
            ?: timezoneRawFromTimeZoneOffsetTag?.let(::normalizeTimeZoneOffsetHours)
            ?: parsedDateTimeOriginal?.second
            ?: timezoneRawFromGps
            ?: timezoneRawFromOffsetTags
            ?: timezoneRawFromTimeZoneOffsetTag,
        cameraMake = cameraMake,
        cameraModel = cameraModel,
        metadata = metadata
    )
}

private data class ParsedIfd(
    val values: Map<Int, String>,
    val pointers: Map<Int, Int>
)

private fun parseIfd(
    bytes: ByteArray,
    reader: ExifReader,
    tiffStart: Int,
    tiffLength: Int,
    ifdOffset: Int
): ParsedIfd {
    val ifdStart = tiffStart + ifdOffset
    if (ifdOffset < 0 || ifdStart + 2 > tiffStart + tiffLength || ifdStart + 2 > bytes.size) {
        return ParsedIfd(emptyMap(), emptyMap())
    }

    val entryCount = reader.u16(ifdStart) ?: return ParsedIfd(emptyMap(), emptyMap())
    val values = mutableMapOf<Int, String>()
    val pointers = mutableMapOf<Int, Int>()
    val dataEnd = tiffStart + tiffLength

    for (entryIndex in 0 until entryCount) {
        val entryOffset = ifdStart + 2 + (entryIndex * 12)
        if (entryOffset + 12 > dataEnd || entryOffset + 12 > bytes.size) break

        val tag = reader.u16(entryOffset) ?: continue
        val type = reader.u16(entryOffset + 2) ?: continue
        val count = reader.u32(entryOffset + 4) ?: continue
        val typeSize = exifTypeSize(type) ?: continue
        val byteCount = count.toLong() * typeSize.toLong()
        if (byteCount <= 0L || byteCount > Int.MAX_VALUE) continue

        val valueOffset = if (byteCount <= 4L) {
            entryOffset + 8
        } else {
            val relative = reader.u32(entryOffset + 8) ?: continue
            tiffStart + relative
        }
        val valueEnd = valueOffset + byteCount.toInt()
        if (valueOffset < tiffStart || valueEnd > dataEnd || valueEnd > bytes.size) continue

        val parsedValue = readExifValueAsString(
            bytes = bytes,
            reader = reader,
            type = type,
            count = count,
            valueOffset = valueOffset
        )
        if (!parsedValue.isNullOrBlank()) {
            values[tag] = parsedValue
        }

        if (tag == EXIF_IFD_POINTER_TAG || tag == GPS_INFO_POINTER_TAG) {
            val pointer = readExifPointerValue(reader, type, valueOffset)
            if (pointer != null) {
                pointers[tag] = pointer
            }
        }
    }

    return ParsedIfd(values = values, pointers = pointers)
}

private fun readExifValueAsString(
    bytes: ByteArray,
    reader: ExifReader,
    type: Int,
    count: Int,
    valueOffset: Int
): String? =
    when (type) {
        EXIF_TYPE_ASCII -> {
            if (count <= 0) return null
            val raw = bytes.decodeToString(
                startIndex = valueOffset,
                endIndex = valueOffset + count
            ).substringBefore('\u0000').trim()
            raw.ifEmpty { null }
        }

        EXIF_TYPE_SHORT -> {
            val value = reader.u16(valueOffset) ?: return null
            value.toString()
        }

        EXIF_TYPE_SSHORT -> {
            val value = reader.s16(valueOffset) ?: return null
            value.toString()
        }

        EXIF_TYPE_LONG -> {
            val value = reader.u32(valueOffset) ?: return null
            value.toString()
        }

        EXIF_TYPE_RATIONAL -> {
            if (count <= 0) return null
            val values = (0 until count).mapNotNull { index ->
                val componentOffset = valueOffset + (index * 8)
                val numerator = reader.u32(componentOffset) ?: return@mapNotNull null
                val denominator = reader.u32(componentOffset + 4) ?: return@mapNotNull null
                if (denominator == 0) return@mapNotNull null
                formatRationalValue(numerator, denominator)
            }
            values.takeIf { it.isNotEmpty() }?.joinToString(":")
        }

        else -> null
    }

private fun readExifPointerValue(
    reader: ExifReader,
    type: Int,
    valueOffset: Int
): Int? =
    when (type) {
        EXIF_TYPE_LONG -> reader.u32(valueOffset)
        EXIF_TYPE_SHORT -> reader.u16(valueOffset)
        else -> null
    }

private fun exifTypeSize(type: Int): Int? =
    when (type) {
        EXIF_TYPE_ASCII -> 1
        EXIF_TYPE_SHORT -> 2
        EXIF_TYPE_SSHORT -> 2
        EXIF_TYPE_LONG -> 4
        EXIF_TYPE_RATIONAL -> 8
        else -> null
    }

private fun formatRationalValue(numerator: Int, denominator: Int): String {
    val value = numerator.toDouble() / denominator.toDouble()
    val rounded = (value * 10.0).toInt() / 10.0
    return if (value < 1.0 && numerator != 0) {
        "1/${(1.0 / value).toInt().coerceAtLeast(1)}"
    } else if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun normalizeExifDateTime(value: String): String? {
    val trimmed = value.trim()
    val match = Regex("""^(\d{4})[:\-](\d{2})[:\-](\d{2})[ T](\d{2}):(\d{2}):(\d{2})$""")
        .matchEntire(trimmed)
        ?: return null

    return "${match.groupValues[1]}-${match.groupValues[2]}-${match.groupValues[3]}T" +
        "${match.groupValues[4]}:${match.groupValues[5]}:${match.groupValues[6]}"
}

private fun parseExifDateTimeAndOffset(value: String): Pair<String, String?>? {
    val trimmed = value.trim()
    val match = Regex(
        """^(\d{4})[:\-](\d{2})[:\-](\d{2})[ T](\d{2}):(\d{2}):(\d{2})(?:\s*(Z|[+-]\d{2}:?\d{2}))?$"""
    ).matchEntire(trimmed) ?: return null

    val normalizedDateTime = "${match.groupValues[1]}-${match.groupValues[2]}-${match.groupValues[3]}T" +
        "${match.groupValues[4]}:${match.groupValues[5]}:${match.groupValues[6]}"
    val rawOffset = match.groupValues[7].ifBlank { null }
    return normalizedDateTime to rawOffset?.let(::normalizeTimeZoneOffset)
}

private fun normalizeTimeZoneOffsetHours(value: String): String? {
    val hours = value.trim().toIntOrNull() ?: return null
    if (hours !in -23..23) return null
    val sign = if (hours >= 0) "+" else "-"
    val hh = kotlin.math.abs(hours).toString().padStart(2, '0')
    return "$sign$hh:00"
}

private fun inferTimeZoneOffsetFromGps(
    dateTimeOriginal: String?,
    gpsDateStamp: String?,
    gpsTimeStamp: String?
): String? {
    val local = parseExifDateTimeAndOffset(dateTimeOriginal ?: return null)?.first ?: return null
    val localParts = parseDateTimeParts(local) ?: return null
    val gpsParts = parseGpsUtcDateTime(gpsDateStamp ?: return null, gpsTimeStamp ?: return null) ?: return null

    val localMinutes = toApproximateEpochMinutes(localParts)
    val gpsMinutes = toApproximateEpochMinutes(gpsParts)
    var diff = localMinutes - gpsMinutes

    while (diff < -14 * 60) diff += 24 * 60
    while (diff > 14 * 60) diff -= 24 * 60
    if (diff !in (-14 * 60)..(14 * 60)) return null

    val sign = if (diff >= 0) "+" else "-"
    val absMinutes = kotlin.math.abs(diff)
    val hours = (absMinutes / 60).toString().padStart(2, '0')
    val minutes = (absMinutes % 60).toString().padStart(2, '0')
    return "$sign$hours:$minutes"
}

private data class DateTimeParts(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int
)

private fun parseDateTimeParts(value: String): DateTimeParts? {
    val match = Regex("""^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):\d{2}$""").matchEntire(value) ?: return null
    return DateTimeParts(
        year = match.groupValues[1].toIntOrNull() ?: return null,
        month = match.groupValues[2].toIntOrNull() ?: return null,
        day = match.groupValues[3].toIntOrNull() ?: return null,
        hour = match.groupValues[4].toIntOrNull() ?: return null,
        minute = match.groupValues[5].toIntOrNull() ?: return null
    )
}

private fun parseGpsUtcDateTime(dateStamp: String, timeStamp: String): DateTimeParts? {
    val date = Regex("""^(\d{4})[:\-](\d{2})[:\-](\d{2})$""").matchEntire(dateStamp.trim()) ?: return null
    val parts = timeStamp.split(':')
    if (parts.size < 2) return null
    val hour = parts[0].substringBefore('/').toIntOrNull() ?: return null
    val minute = parts[1].substringBefore('/').toIntOrNull() ?: return null
    return DateTimeParts(
        year = date.groupValues[1].toIntOrNull() ?: return null,
        month = date.groupValues[2].toIntOrNull() ?: return null,
        day = date.groupValues[3].toIntOrNull() ?: return null,
        hour = hour,
        minute = minute
    )
}

private fun toApproximateEpochMinutes(parts: DateTimeParts): Int {
    val monthDays = intArrayOf(31, if (isLeapYear(parts.year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val dayOfYear = monthDays.take(parts.month - 1).sum() + parts.day
    return (((parts.year * 366) + dayOfYear) * 24 + parts.hour) * 60 + parts.minute
}

private fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

private fun normalizeTimeZoneOffset(value: String): String? {
    val trimmed = value.trim()
    if (trimmed == "Z") return "Z"

    val withColon = Regex("""^([+-])(\d{2}):(\d{2})$""").matchEntire(trimmed)
    if (withColon != null) {
        return "${withColon.groupValues[1]}${withColon.groupValues[2]}:${withColon.groupValues[3]}"
    }

    val compact = Regex("""^([+-])(\d{2})(\d{2})$""").matchEntire(trimmed)
    if (compact != null) {
        return "${compact.groupValues[1]}${compact.groupValues[2]}:${compact.groupValues[3]}"
    }

    return null
}

private class ExifReader(
    private val bytes: ByteArray,
    private val littleEndian: Boolean
) {
    fun u16(offset: Int): Int? {
        if (offset < 0 || offset + 1 >= bytes.size) return null
        val a = bytes[offset].u8()
        val b = bytes[offset + 1].u8()
        return if (littleEndian) {
            a or (b shl 8)
        } else {
            (a shl 8) or b
        }
    }

    fun u32(offset: Int): Int? {
        if (offset < 0 || offset + 3 >= bytes.size) return null
        val a = bytes[offset].u8()
        val b = bytes[offset + 1].u8()
        val c = bytes[offset + 2].u8()
        val d = bytes[offset + 3].u8()
        return if (littleEndian) {
            a or (b shl 8) or (c shl 16) or (d shl 24)
        } else {
            (a shl 24) or (b shl 16) or (c shl 8) or d
        }
    }

    fun s16(offset: Int): Int? {
        val unsigned = u16(offset) ?: return null
        return if (unsigned and 0x8000 != 0) unsigned - 0x10000 else unsigned
    }
}

private fun Byte.u8(): Int = toInt() and 0xFF

private const val MAKE_TAG = 0x010F
private const val MODEL_TAG = 0x0110
private const val SOFTWARE_TAG = 0x0131
private const val DATE_TIME_TAG = 0x0132
private const val EXIF_IFD_POINTER_TAG = 0x8769
private const val GPS_INFO_POINTER_TAG = 0x8825

private const val DATE_TIME_ORIGINAL_TAG = 0x9003
private const val DATE_TIME_DIGITIZED_TAG = 0x9004
private const val OFFSET_TIME_TAG = 0x9010
private const val OFFSET_TIME_ORIGINAL_TAG = 0x9011
private const val TIME_ZONE_OFFSET_TAG = 0x882A
private const val GPS_TIME_STAMP_TAG = 0x0007
private const val GPS_DATE_STAMP_TAG = 0x001D
private const val ISO_SPEED_TAG = 0x8827
private const val EXPOSURE_TIME_TAG = 0x829A
private const val F_NUMBER_TAG = 0x829D
private const val FOCAL_LENGTH_TAG = 0x920A
private const val LENS_MODEL_TAG = 0xA434

private const val EXIF_TYPE_ASCII = 2
private const val EXIF_TYPE_SHORT = 3
private const val EXIF_TYPE_SSHORT = 8
private const val EXIF_TYPE_LONG = 4
private const val EXIF_TYPE_RATIONAL = 5
