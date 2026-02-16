package com.marcportabella.immichuploader.web

import kotlinx.coroutines.await
import kotlin.js.JsAny
import kotlin.js.Promise

@OptIn(ExperimentalWasmJsInterop::class)
suspend fun <T> Promise<JsAny?>.awaitJs(): T = await()
