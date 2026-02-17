package com.marcportabella.immichuploader.ui.uploadprep

import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset

data class DisplayMetadata(
    val dateTimeOriginal: String?,
    val timeZone: String?,
    val captureDisplay: String?,
    val cameraLabel: String?,
    val description: String?,
    val isFavorite: Boolean?,
    val albumId: String?,
    val tagIds: Set<String>,
    val exifSummary: String?
)

fun LocalAsset.toDisplayMetadata(patch: AssetEditPatch?): DisplayMetadata {
    val description = (patch?.description as? FieldPatch.Set<String?>)?.value ?: description
    val isFavorite = (patch?.isFavorite as? FieldPatch.Set<Boolean>)?.value ?: isFavorite
    val dateTimeOriginal = (patch?.dateTimeOriginal as? FieldPatch.Set<String>)?.value ?: captureDateTime
    val timeZone = (patch?.timeZone as? FieldPatch.Set<String>)?.value ?: timeZone
    val albumId = (patch?.albumId as? FieldPatch.Set<String?>)?.value ?: albumId

    val addTags = patch?.addTagIds ?: emptySet()
    val removeTags = patch?.removeTagIds ?: emptySet()

    return DisplayMetadata(
        dateTimeOriginal = dateTimeOriginal,
        timeZone = timeZone,
        captureDisplay = formatCaptureDateTime(dateTimeOriginal, timeZone),
        cameraLabel = listOfNotNull(cameraMake, cameraModel).joinToString(" ").ifBlank { null },
        description = description,
        isFavorite = isFavorite,
        albumId = albumId,
        tagIds = (tagIds + addTags) - removeTags,
        exifSummary = exifSummary
    )
}

fun formatCaptureDateTime(
    dateTimeOriginal: String?,
    timeZone: String?
): String? {
    val dateTime = dateTimeOriginal?.trim().orEmpty()
    if (dateTime.isBlank()) return null

    val normalizedDateTime = runCatching { LocalDateTime.parse(dateTime).toString() }.getOrElse { dateTime }
    val normalizedOffset = timeZone
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { UtcOffset.parse(it).toString() }.getOrElse { it } }

    return if (normalizedOffset == null) normalizedDateTime else "$normalizedDateTime $normalizedOffset"
}
