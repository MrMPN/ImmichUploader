package com.marcportabella.immichuploader.web

import kotlinx.coroutines.await
import kotlin.js.JsAny
import kotlin.js.Promise

suspend fun <T> Promise<JsAny?>.awaitJs(): T = await()
