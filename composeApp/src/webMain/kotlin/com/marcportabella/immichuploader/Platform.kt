package com.marcportabella.immichuploader

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform