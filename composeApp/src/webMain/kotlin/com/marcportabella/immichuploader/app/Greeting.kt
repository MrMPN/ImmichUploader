package com.marcportabella.immichuploader.app

import com.marcportabella.immichuploader.getPlatform

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}
