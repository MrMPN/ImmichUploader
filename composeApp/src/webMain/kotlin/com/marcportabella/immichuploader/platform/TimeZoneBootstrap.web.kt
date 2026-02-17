package com.marcportabella.immichuploader.platform

@OptIn(ExperimentalWasmJsInterop::class)
@JsModule("@js-joda/timezone")
private external object JsJodaTimeZoneModule

private val tzModule = JsJodaTimeZoneModule

fun initializeTimeZoneDatabase() {
    // Accessing the module is enough to register timezone data for kotlinx-datetime on JS/Wasm.
    tzModule
}
