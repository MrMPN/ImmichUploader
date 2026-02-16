package com.marcportabella.immichuploader.web

fun logInfo(message: String) {
    browserConsole.log(message)
}

fun logError(message: String) {
    browserConsole.error(message)
}

private external object browserConsole {
    fun log(message: String)
    fun error(message: String)
}
