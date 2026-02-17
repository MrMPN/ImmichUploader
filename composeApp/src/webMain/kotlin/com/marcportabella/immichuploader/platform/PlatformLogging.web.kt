package com.marcportabella.immichuploader.platform

actual fun platformLogInfo(message: String) {
    browserConsole.log(message)
}

actual fun platformLogError(message: String) {
    browserConsole.error(message)
}

@JsName("console")
private external val browserConsole: BrowserConsoleBindings

private external interface BrowserConsoleBindings {
    fun log(message: String)
    fun error(message: String)
}
