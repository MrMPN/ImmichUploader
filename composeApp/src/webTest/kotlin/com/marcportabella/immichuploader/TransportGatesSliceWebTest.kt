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
    private val testApiBaseUrl = "https://immich.test/api"

    @Test
    fun catalogTransportGateStatusFollowsApiKeyPresence() {
        val fakeTransport = object : ImmichOnlineCatalogTransport {
            private val albums = linkedMapOf<String, ImmichCatalogEntry>()
            private val tags = linkedMapOf<String, ImmichCatalogEntry>()

            override suspend fun lookupAlbums(apiKey: String, serverBaseUrl: String): ImmichCatalogResult.Success =
                ImmichCatalogResult.Success(
                    request = ImmichCatalogRequestBuilder.lookupAlbums(serverBaseUrl),
                    entries = albums.values.toList(),
                    message = "ok"
                )

            override suspend fun lookupTags(apiKey: String, serverBaseUrl: String): ImmichCatalogResult.Success =
                ImmichCatalogResult.Success(
                    request = ImmichCatalogRequestBuilder.lookupTags(serverBaseUrl),
                    entries = tags.values.toList(),
                    message = "ok"
                )

            override suspend fun createAlbumIfMissing(
                apiKey: String,
                serverBaseUrl: String,
                name: String
            ): ImmichCatalogResult.Success {
                val key = name.lowercase()
                if (albums[key] == null) {
                    albums[key] = ImmichCatalogEntry(key, name)
                }
                return lookupAlbums(apiKey, serverBaseUrl)
            }

            override suspend fun createTagIfMissing(
                apiKey: String,
                serverBaseUrl: String,
                name: String
            ): ImmichCatalogResult.Success {
                val key = name.lowercase()
                if (tags[key] == null) {
                    tags[key] = ImmichCatalogEntry(key, name)
                }
                return lookupTags(apiKey, serverBaseUrl)
            }

            override suspend fun bulkUploadCheck(
                apiKey: String,
                serverBaseUrl: String,
                items: List<com.marcportabella.immichuploader.data.ImmichBulkUploadCheckItem>
            ) = com.marcportabella.immichuploader.data.ImmichBulkUploadCheckResult.Success(
                request = ImmichCatalogRequestBuilder.bulkUploadCheck(serverBaseUrl, items),
                existingAssetIdByItemId = emptyMap(),
                message = "ok"
            )
        }
        val transport = ApiKeyGatedImmichCatalogTransport(fakeTransport)

        assertEquals(TransportGateStatus.MissingApiKey, transport.gateStatus(null, testApiBaseUrl))
        assertEquals(TransportGateStatus.MissingApiKey, transport.gateStatus("   ", testApiBaseUrl))
        assertEquals(TransportGateStatus.MissingServerBaseUrl, transport.gateStatus("k", ""))
        assertEquals(TransportGateStatus.Ready, transport.gateStatus("k", testApiBaseUrl))
    }

    @Test
    fun uploadTransportPathSelectionAndGateFollowApiKeyPresence() {
        val transport = ApiKeyGatedImmichTransport(ApiImmichOnlineTransport())

        assertEquals(
            UploadExecutionPath.BlockedMissingApiKey,
            transport.selectExecutionPath(apiKey = null, serverBaseUrl = testApiBaseUrl)
        )
        assertEquals(
            UploadExecutionPath.BlockedMissingApiKey,
            transport.selectExecutionPath(apiKey = "   ", serverBaseUrl = testApiBaseUrl)
        )
        assertEquals(
            UploadExecutionPath.BlockedMissingServerBaseUrl,
            transport.selectExecutionPath(apiKey = "key", serverBaseUrl = "")
        )
        assertEquals(
            UploadExecutionPath.ApiExecution,
            transport.selectExecutionPath(apiKey = "key", serverBaseUrl = testApiBaseUrl)
        )
        assertEquals(TransportGateStatus.MissingApiKey, transport.gateStatus(null, testApiBaseUrl))
        assertEquals(TransportGateStatus.MissingServerBaseUrl, transport.gateStatus("key", ""))
        assertEquals(TransportGateStatus.Ready, transport.gateStatus("key", testApiBaseUrl))
    }
}
