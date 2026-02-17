package com.marcportabella.immichuploader.ui

import com.marcportabella.immichuploader.data.ImmichApiRequest
import com.marcportabella.immichuploader.data.ImmichCatalogEntry
import com.marcportabella.immichuploader.data.ImmichRequestPlan
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.BatchFeedback
import com.marcportabella.immichuploader.domain.BatchFeedbackLevel
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.FieldPatch
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.UploadExecutionStatus

internal fun previewAsset(
    id: String,
    name: String
): LocalAsset = LocalAsset(
    id = LocalAssetId(id),
    fileName = name,
    mimeType = "image/jpeg",
    fileSizeBytes = 2_500_000L,
    previewUrl = null,
    captureDateTime = "2016-11-08T02:43:27",
    timeZone = "+01:00",
    cameraMake = "NIKON CORPORATION",
    cameraModel = "NIKON D3200",
    exifSummary = "ISO 400 | f/5.0 | 1/40s | 18.0 mm"
)

internal fun previewCatalogAlbums(): List<ImmichCatalogEntry> = listOf(
    ImmichCatalogEntry(id = "album-1", name = "Japan 2016"),
    ImmichCatalogEntry(id = "album-2", name = "Mountains")
)

internal fun previewCatalogTags(): List<ImmichCatalogEntry> = listOf(
    ImmichCatalogEntry(id = "tag-1", name = "Night"),
    ImmichCatalogEntry(id = "tag-2", name = "Street"),
    ImmichCatalogEntry(id = "tag-3", name = "Travel")
)

internal fun previewSinglePatch(): AssetEditPatch = AssetEditPatch(
    description = FieldPatch.Set("Shibuya crossing"),
    isFavorite = FieldPatch.Set(true),
    albumId = FieldPatch.Set("album-1"),
    addTagIds = setOf("tag-1", "tag-3")
)

internal fun previewBulkDraft(): BulkEditDraft = BulkEditDraft(
    includeDescription = true,
    description = "Trip highlights",
    includeTimeZone = true,
    timeZone = "+09:00",
    includeAlbumId = true,
    albumId = "album-1",
    addTagIds = "tag-1,tag-3"
)

internal fun previewRequests(): List<ImmichApiRequest> = listOf(
    ImmichApiRequest(method = "GET", url = "https://fotos.marcportabella.com/api/albums"),
    ImmichApiRequest(method = "PATCH", url = "https://fotos.marcportabella.com/api/assets", body = """{"ids":["a1"]}""")
)

internal fun previewPlan(): ImmichRequestPlan = ImmichRequestPlan()

internal fun previewFeedback(): BatchFeedback = BatchFeedback(
    level = BatchFeedbackLevel.Success,
    message = "Applied bulk edits to 2 selected assets."
)

internal const val PREVIEW_GATE_STATUS: String = "Ready"
internal const val PREVIEW_EXECUTION_PATH: String = "ApiExecution"
internal const val PREVIEW_CATALOG_STATUS: String = "Ready"
internal const val PREVIEW_CATALOG_MESSAGE: String = "Loaded 3 tags from server."
internal const val PREVIEW_PREFLIGHT_MESSAGE: String = "Bulk timezone and tags will be applied to selected assets."
internal const val PREVIEW_EXECUTION_MESSAGE: String = "Submitted 2 API requests."
internal val PREVIEW_EXECUTION_STATUS: UploadExecutionStatus = UploadExecutionStatus.Submitted
