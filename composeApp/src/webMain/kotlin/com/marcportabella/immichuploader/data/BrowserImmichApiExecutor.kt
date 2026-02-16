package com.marcportabella.immichuploader.data

import com.marcportabella.immichuploader.web.awaitJs
import com.marcportabella.immichuploader.web.logInfo
import kotlin.js.JsAny
import kotlin.js.Promise

class BrowserImmichApiExecutor : ImmichApiExecutor {
    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun execute(request: ImmichApiRequest, apiKey: String): ImmichApiExecutorResult {
        logInfo("[immichuploader][http] -> ${request.method} ${request.url}")
        logInfo("[immichuploader][http] stage=headers:init")
        val headers = createHeaders()
        logInfo("[immichuploader][http] stage=headers:append-api-key")
        headers.append("x-api-key", apiKey)
        logInfo("[immichuploader][http] stage=headers:append-accept")
        headers.append("Accept", "application/json")
        if (request.body != null) {
            logInfo("[immichuploader][http] stage=headers:append-content-type")
            headers.append("Content-Type", "application/json")
        }

        logInfo("[immichuploader][http] stage=request-init:create")
        val init = createRequestInit(
            method = request.method,
            headers = headers,
            body = request.body
        )

        logInfo("[immichuploader][http] stage=fetch:call")
        val responseAny = fetch(
            input = request.url,
            init = init
        ).awaitJs<JsAny?>()
        logInfo("[immichuploader][http] stage=fetch:resolved")
        val response = requireNotNull(responseAny) {
            "fetch resolved with null response for ${request.method} ${request.url}"
        }.unsafeCast<FetchResponse>()

        logInfo("[immichuploader][http] stage=response:text")
        val responseBodyAny = response.text().awaitJs<JsAny?>()
        val responseBody = responseBodyAny?.toString() ?: ""
        logInfo(
            "[immichuploader][http] <- ${request.method} ${request.url} status=${response.status.toInt()} bodyBytes=${responseBody.length}"
        )

        return ImmichApiExecutorResult(
            statusCode = response.status.toInt(),
            responseBody = responseBody
        )
    }
}

private external interface Headers : JsAny {
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

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsNewHeaders(): JsAny = js("new Headers()")

@OptIn(ExperimentalWasmJsInterop::class)
private fun createHeaders(): Headers = jsNewHeaders().unsafeCast<Headers>()
