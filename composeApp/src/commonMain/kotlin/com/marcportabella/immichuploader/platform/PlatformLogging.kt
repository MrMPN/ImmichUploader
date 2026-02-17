package com.marcportabella.immichuploader.platform

expect fun platformLogInfo(message: String)

expect fun platformLogError(message: String)

fun Throwable.diagnosticMessage(): String {
    val parts = mutableListOf<String>()
    parts += this::class.simpleName ?: "Throwable"
    if (!message.isNullOrBlank()) {
        parts += message!!
    }

    var current: Throwable? = cause
    while (current != null) {
        val type = current::class.simpleName ?: "Throwable"
        val msg = current.message
        parts += if (msg.isNullOrBlank()) "caused by $type" else "caused by $type: $msg"
        current = current.cause
    }

    return parts.joinToString(" | ")
}
