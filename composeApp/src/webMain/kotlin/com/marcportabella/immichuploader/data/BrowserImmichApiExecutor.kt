package com.marcportabella.immichuploader.data

import com.marcportabella.immichuploader.web.awaitJs
import kotlin.js.JsAny
import kotlin.js.Promise

class BrowserImmichApiExecutor : ImmichApiExecutor {
    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun execute(request: ImmichApiRequest, apiKey: String): ImmichApiExecutorResult {
        val headers = Headers().apply {
            append("x-api-key", apiKey)
            append("Accept", "application/json")
            if (request.body != null) {
                append("Content-Type", "application/json")
            }
        }

        val init = RequestInit(
            method = request.method,
            headers = headers
        ).apply {
            if (request.body != null) {
                body = request.body
            }
        }

        val response = fetch(
            input = request.url,
            init = init
        ).awaitJs<FetchResponse>()
        val responseBody = response.text().awaitJs<String>()

        return ImmichApiExecutorResult(
            statusCode = response.status.toInt(),
            responseBody = responseBody
        )
    }
}

private external class Headers {
    fun append(name: String, value: String)
}

private external class RequestInit(
    method: String? = definedExternally,
    headers: Headers? = definedExternally,
    body: String? = definedExternally
) {
    var body: String?
}

@OptIn(ExperimentalWasmJsInterop::class)
private external interface FetchResponse : JsAny {
    val status: Short
    @OptIn(ExperimentalWasmJsInterop::class)
    fun text(): Promise<JsAny?>
}

@OptIn(ExperimentalWasmJsInterop::class)
private external fun fetch(input: String, init: RequestInit): Promise<JsAny?>
