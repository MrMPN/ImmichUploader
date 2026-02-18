package com.marcportabella.immichuploader.data

import com.marcportabella.immichuploader.platform.platformLogInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Headers
import io.github.vinceglb.filekit.readBytes

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
                when (body) {
                    is ImmichUploadBody -> {
                        val sourceFile = body.payload.sourceFile
                        val fileBytes = sourceFile?.readBytes() ?: ByteArray(0)
                        platformLogInfo(
                            "[immichuploader][http] upload-part assetData file=${body.payload.fileName} mime=${body.payload.mimeType} bytes=${fileBytes.size}"
                        )
                        platformLogInfo(
                            "[immichuploader][http] upload-payload ${body.payload}"
                        )
                        setBody(
                            MultiPartFormDataContent(
                                formData {
                                    append(
                                        key = "assetData",
                                        value = fileBytes,
                                        headers = Headers.build {
                                            append(
                                                HttpHeaders.ContentDisposition,
                                                "filename=\"${body.payload.fileName}\""
                                            )
                                            append(HttpHeaders.ContentType, body.payload.mimeType)
                                        }
                                    )
                                    body.payload.sidecarData?.let { sidecar ->
                                        append(
                                            key = "sidecarData",
                                            value = sidecar.encodeToByteArray(),
                                            headers = Headers.build {
                                                append(
                                                    HttpHeaders.ContentDisposition,
                                                    "filename=\"${body.payload.fileName}.xmp\""
                                                )
                                                append(HttpHeaders.ContentType, "application/rdf+xml")
                                            }
                                        )
                                    }
                                    append("deviceAssetId", body.payload.deviceAssetId)
                                    append("deviceId", body.payload.deviceId)
                                    append("fileCreatedAt", body.payload.fileCreatedAt)
                                    append("fileModifiedAt", body.payload.fileModifiedAt)
                                }
                            )
                        )
                    }

                    is ImmichBulkMetadataBody -> {
                        header(HttpHeaders.ContentType, "application/json")
                        setBody(body.payload)
                    }

                    is ImmichTagAssignBody -> {
                        header(HttpHeaders.ContentType, "application/json")
                        setBody(body.payload)
                    }

                    is ImmichTagAssetsBody -> {
                        header(HttpHeaders.ContentType, "application/json")
                        setBody(body.payload)
                    }

                    is ImmichAlbumAddBody -> {
                        header(HttpHeaders.ContentType, "application/json")
                        setBody(body.payload)
                    }

                    is ImmichAlbumCreateBody -> {
                        header(HttpHeaders.ContentType, "application/json")
                        setBody(body.payload)
                    }

                    is ImmichTagCreateBody -> {
                        header(HttpHeaders.ContentType, "application/json")
                        setBody(body.payload)
                    }

                    is ImmichBulkUploadCheckBody -> {
                        header(HttpHeaders.ContentType, "application/json")
                        setBody(body.payload)
                    }

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
