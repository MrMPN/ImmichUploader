package com.marcportabella.immichuploader.data

import com.marcportabella.immichuploader.web.awaitJs
import kotlin.js.JsAny
import kotlin.js.Promise

class BrowserImmichApiExecutor : ImmichApiExecutor {
    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun execute(request: ImmichApiRequest, apiKey: String): ImmichApiExecutorResult {
        httpConsole.log("[immichuploader][http] -> ${request.method} ${request.url}")
        val headers = Headers().apply {
            append("x-api-key", apiKey)
            append("Accept", "application/json")
            if (request.body != null) {
                append("Content-Type", "application/json")
            }
        }

        val init = createRequestInit(
            method = request.method,
            headers = headers,
            body = request.body
        )

        val response = fetch(
            input = request.url,
            init = init
        ).awaitJs<FetchResponse>()
        val responseBody = response.text().awaitJs<String>()
        httpConsole.log(
            "[immichuploader][http] <- ${request.method} ${request.url} status=${response.status.toInt()} bodyBytes=${responseBody.length}"
        )

        return ImmichApiExecutorResult(
            statusCode = response.status.toInt(),
            responseBody = responseBody
        )
    }
}

private external class Headers {
    fun append(name: String, value: String)
}

private external interface RequestInit : JsAny {
    var method: String
    var headers: Headers
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

@OptIn(ExperimentalWasmJsInterop::class)
private fun createRequestInit(
    method: String,
    headers: Headers,
    body: String?
): RequestInit {
    val init = jsEmptyObject().unsafeCast<RequestInit>()
    init.method = method
    init.headers = headers
    if (body != null) {
        init.body = body
    }
    return init
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsEmptyObject(): JsAny = js("{}")

private external object httpConsole {
    fun log(message: String)
}
