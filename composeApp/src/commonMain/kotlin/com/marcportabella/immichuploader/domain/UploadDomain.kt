package com.marcportabella.immichuploader.domain

import io.github.vinceglb.filekit.PlatformFile

data class LocalAssetId(val value: String)

data class LocalAsset(
    val id: LocalAssetId,
    val fileName: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val previewUrl: String?,
    val captureDateTime: String?,
    val timeZone: String?,
    val description: String? = null,
    val isFavorite: Boolean? = null,
    val albumId: String? = null,
    val tagIds: Set<String> = emptySet(),
    val cameraMake: String? = null,
    val cameraModel: String? = null,
    val exifMetadata: Map<String, String> = emptyMap(),
    val exifSummary: String? = null,
    val sourceFile: PlatformFile? = null
)

sealed interface FieldPatch<out T> {
    data object Unset : FieldPatch<Nothing>
    data class Set<T>(val value: T) : FieldPatch<T>
}

data class AssetEditPatch(
    val description: FieldPatch<String?> = FieldPatch.Unset,
    val isFavorite: FieldPatch<Boolean> = FieldPatch.Unset,
    val dateTimeOriginal: FieldPatch<String> = FieldPatch.Unset,
    val timeZone: FieldPatch<String> = FieldPatch.Unset,
    val albumId: FieldPatch<String?> = FieldPatch.Unset,
    val addTagIds: Set<String> = emptySet(),
    val removeTagIds: Set<String> = emptySet(),
    val customMetadata: Map<String, String> = emptyMap()
) {
    fun merge(next: AssetEditPatch): AssetEditPatch {
        val mergedAdds = (addTagIds + next.addTagIds) - next.removeTagIds
        val mergedRemoves = (removeTagIds + next.removeTagIds) - next.addTagIds
        return copy(
            description = mergeField(description, next.description),
            isFavorite = mergeField(isFavorite, next.isFavorite),
            dateTimeOriginal = mergeField(dateTimeOriginal, next.dateTimeOriginal),
            timeZone = mergeField(timeZone, next.timeZone),
            albumId = mergeField(albumId, next.albumId),
            addTagIds = mergedAdds,
            removeTagIds = mergedRemoves,
            customMetadata = customMetadata + next.customMetadata
        )
    }
}

private fun <T> mergeField(current: FieldPatch<T>, next: FieldPatch<T>): FieldPatch<T> =
    when (next) {
        is FieldPatch.Set -> next
        FieldPatch.Unset -> current
    }
