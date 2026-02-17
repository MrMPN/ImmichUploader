package com.marcportabella.immichuploader.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

actual fun defaultImmichHttpClient(): HttpClient = HttpClient(Js) {
    expectSuccess = false
    install(ContentNegotiation) {
        json(immichJson)
    }
}
