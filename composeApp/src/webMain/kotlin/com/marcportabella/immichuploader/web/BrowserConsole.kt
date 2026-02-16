package com.marcportabella.immichuploader.web

fun logInfo(message: String) {
    browserConsole.log(message)
}

fun logError(message: String) {
    browserConsole.error(message)
}

@JsName("console")
private external val browserConsole: BrowserConsoleBindings

private external interface BrowserConsoleBindings {
    fun log(message: String)
    fun error(message: String)
}
