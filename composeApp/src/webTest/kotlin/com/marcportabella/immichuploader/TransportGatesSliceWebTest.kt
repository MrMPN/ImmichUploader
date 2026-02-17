package com.marcportabella.immichuploader

import com.marcportabella.immichuploader.data.ApiImmichOnlineTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichCatalogTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichTransport
import com.marcportabella.immichuploader.data.ImmichCatalogEntry
import com.marcportabella.immichuploader.data.ImmichCatalogRequestBuilder
import com.marcportabella.immichuploader.data.ImmichCatalogResult
import com.marcportabella.immichuploader.data.ImmichOnlineCatalogTransport
import com.marcportabella.immichuploader.data.TransportGateStatus
import com.marcportabella.immichuploader.data.UploadExecutionPath
import kotlin.test.Test
import kotlin.test.assertEquals

class TransportGatesSliceWebTest {

    @Test
    fun catalogTransportGateStatusFollowsApiKeyPresence() {
        val fakeTransport = object : ImmichOnlineCatalogTransport {
            private val albums = linkedMapOf<String, ImmichCatalogEntry>()
            private val tags = linkedMapOf<String, ImmichCatalogEntry>()

            override suspend fun lookupAlbums(apiKey: String): ImmichCatalogResult.Success =
                ImmichCatalogResult.Success(
                    request = ImmichCatalogRequestBuilder.lookupAlbums(),
                    entries = albums.values.toList(),
                    message = "ok"
                )

            override suspend fun lookupTags(apiKey: String): ImmichCatalogResult.Success =
                ImmichCatalogResult.Success(
                    request = ImmichCatalogRequestBuilder.lookupTags(),
                    entries = tags.values.toList(),
                    message = "ok"
                )

            override suspend fun createAlbumIfMissing(apiKey: String, name: String): ImmichCatalogResult.Success {
                val key = name.lowercase()
                if (albums[key] == null) {
                    albums[key] = ImmichCatalogEntry(key, name)
                }
                return lookupAlbums(apiKey)
            }

            override suspend fun createTagIfMissing(apiKey: String, name: String): ImmichCatalogResult.Success {
                val key = name.lowercase()
                if (tags[key] == null) {
                    tags[key] = ImmichCatalogEntry(key, name)
                }
                return lookupTags(apiKey)
            }

            override suspend fun bulkUploadCheck(
                apiKey: String,
                items: List<com.marcportabella.immichuploader.data.ImmichBulkUploadCheckItem>
            ) = com.marcportabella.immichuploader.data.ImmichBulkUploadCheckResult.Success(
                request = ImmichCatalogRequestBuilder.bulkUploadCheck(items),
                existingAssetIdByItemId = emptyMap(),
                message = "ok"
            )
        }
        val transport = ApiKeyGatedImmichCatalogTransport(fakeTransport)

        assertEquals(TransportGateStatus.MissingApiKey, transport.gateStatus(null))
        assertEquals(TransportGateStatus.MissingApiKey, transport.gateStatus("   "))
        assertEquals(TransportGateStatus.Ready, transport.gateStatus("k"))
    }

    @Test
    fun uploadTransportPathSelectionAndGateFollowApiKeyPresence() {
        val transport = ApiKeyGatedImmichTransport(ApiImmichOnlineTransport())

        assertEquals(
            UploadExecutionPath.BlockedMissingApiKey,
            transport.selectExecutionPath(apiKey = null)
        )
        assertEquals(
            UploadExecutionPath.BlockedMissingApiKey,
            transport.selectExecutionPath(apiKey = "   ")
        )
        assertEquals(
            UploadExecutionPath.ApiExecution,
            transport.selectExecutionPath(apiKey = "key")
        )
        assertEquals(TransportGateStatus.MissingApiKey, transport.gateStatus(null))
        assertEquals(TransportGateStatus.Ready, transport.gateStatus("key"))
    }
}
