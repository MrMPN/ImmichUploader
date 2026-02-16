package com.marcportabella.immichuploader

interface Platform {
    val name: String
}

private class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

fun getPlatform(): Platform = WasmPlatform()
