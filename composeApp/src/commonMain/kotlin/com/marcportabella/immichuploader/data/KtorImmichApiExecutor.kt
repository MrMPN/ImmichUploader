package com.marcportabella.immichuploader.data

import com.marcportabella.immichuploader.platform.platformLogInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod

class KtorImmichApiExecutor(
    private val client: HttpClient
) : ImmichApiExecutor {
    override suspend fun execute(request: ImmichApiRequest, apiKey: String): ImmichApiExecutorResult {
        platformLogInfo("[immichuploader][http] -> ${request.method} ${request.url}")
        val response = client.request(request.url) {
            method = HttpMethod.parse(request.method)
            header(HttpHeaders.Accept, "application/json")
            header("x-api-key", apiKey)
            request.body?.let { body ->
                header(HttpHeaders.ContentType, "application/json")
                when (body) {
                    is ImmichUploadBody -> setBody(body.payload)
                    is ImmichBulkMetadataBody -> setBody(body.payload)
                    is ImmichTagAssignBody -> setBody(body.payload)
                    is ImmichAlbumAddBody -> setBody(body.payload)
                    is ImmichAlbumCreateBody -> setBody(body.payload)
                    is ImmichTagCreateBody -> setBody(body.payload)
                    is ImmichBulkUploadCheckBody -> setBody(body.payload)
                }
            }
        }

        val responseBody = response.body<String>()
        platformLogInfo(
            "[immichuploader][http] <- ${request.method} ${request.url} status=${response.status.value} bodyBytes=${responseBody.length}"
        )
        if (response.status.value !in 200..299) {
            val snippet = responseBody.replace('\n', ' ').take(500)
            platformLogInfo(
                "[immichuploader][http] !! ${request.method} ${request.url} errorBody=$snippet"
            )
        }

        return ImmichApiExecutorResult(
            statusCode = response.status.value,
            responseBody = responseBody
        )
    }
}
