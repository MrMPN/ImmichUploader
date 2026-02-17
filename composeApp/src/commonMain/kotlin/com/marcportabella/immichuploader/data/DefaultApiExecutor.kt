package com.marcportabella.immichuploader.data

import io.ktor.client.HttpClient

expect fun defaultImmichHttpClient(): HttpClient

fun defaultImmichApiExecutor(): ImmichApiExecutor = KtorImmichApiExecutor(defaultImmichHttpClient())
