package com.marcportabella.immichuploader.data

import com.marcportabella.immichuploader.domain.UploadAlbumAddRequest
import com.marcportabella.immichuploader.domain.UploadBulkMetadataRequest
import com.marcportabella.immichuploader.domain.UploadCatalogEntry
import com.marcportabella.immichuploader.domain.UploadLookupHook
import com.marcportabella.immichuploader.domain.UploadRequestPlan
import com.marcportabella.immichuploader.domain.UploadTagAssignRequest
import com.marcportabella.immichuploader.domain.UploadUploadRequest

fun UploadCatalogEntry.toDataCatalogEntry(): ImmichCatalogEntry =
    ImmichCatalogEntry(id = id, name = name)

fun ImmichCatalogEntry.toDomainCatalogEntry(): UploadCatalogEntry =
    UploadCatalogEntry(id = id, name = name)

fun UploadRequestPlan.toDataRequestPlan(): ImmichRequestPlan =
    ImmichRequestPlan(
        uploadRequests = uploadRequests.map(UploadUploadRequest::toDataUploadRequest),
        bulkMetadataRequests = bulkMetadataRequests.map(UploadBulkMetadataRequest::toDataBulkMetadataRequest),
        tagAssignRequests = tagAssignRequests.map(UploadTagAssignRequest::toDataTagAssignRequest),
        albumAddRequests = albumAddRequests.map(UploadAlbumAddRequest::toDataAlbumAddRequest),
        lookupHooks = lookupHooks.map(UploadLookupHook::toDataLookupHook),
        sessionTagsById = sessionTagsById
    )

private fun UploadUploadRequest.toDataUploadRequest(): ImmichUploadRequest =
    ImmichUploadRequest(
        localAssetId = localAssetId,
        fileName = fileName,
        mimeType = mimeType,
        sourceFile = null,
        sidecarData = sidecarData,
        deviceAssetId = deviceAssetId,
        deviceId = deviceId,
        fileCreatedAt = fileCreatedAt,
        fileModifiedAt = fileModifiedAt
    )

private fun UploadBulkMetadataRequest.toDataBulkMetadataRequest(): ImmichBulkMetadataRequest =
    ImmichBulkMetadataRequest(
        ids = ids,
        dateTimeOriginal = dateTimeOriginal,
        timeZone = timeZone,
        description = description,
        isFavorite = isFavorite
    )

private fun UploadTagAssignRequest.toDataTagAssignRequest(): ImmichTagAssignRequest =
    ImmichTagAssignRequest(
        assetIds = assetIds,
        tagIds = tagIds
    )

private fun UploadAlbumAddRequest.toDataAlbumAddRequest(): ImmichAlbumAddRequest =
    ImmichAlbumAddRequest(
        albumId = albumId,
        assetIds = assetIds
    )

private fun UploadLookupHook.toDataLookupHook(): ImmichLookupHook =
    when (this) {
        UploadLookupHook.LookupAlbums -> ImmichLookupHook.LookupAlbums
        UploadLookupHook.LookupTags -> ImmichLookupHook.LookupTags
        is UploadLookupHook.CreateAlbumIfMissing -> ImmichLookupHook.CreateAlbumIfMissing(name)
        is UploadLookupHook.CreateTagIfMissing -> ImmichLookupHook.CreateTagIfMissing(name)
    }
