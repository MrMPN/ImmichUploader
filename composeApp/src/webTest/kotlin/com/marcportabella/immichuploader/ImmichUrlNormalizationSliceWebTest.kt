package com.marcportabella.immichuploader

import com.marcportabella.immichuploader.data.buildImmichApiUrl
import com.marcportabella.immichuploader.data.normalizeImmichApiBaseUrl
import kotlin.test.Test
import kotlin.test.assertEquals

class ImmichUrlNormalizationSliceWebTest {
    @Test
    fun normalizeSupportsRelativeApiBaseUrl() {
        assertEquals("/api", normalizeImmichApiBaseUrl("/api"))
        assertEquals("/immich/api", normalizeImmichApiBaseUrl("/immich"))
    }

    @Test
    fun normalizeSupportsHostAndAbsoluteUrl() {
        assertEquals("https://fotos.example.com/api", normalizeImmichApiBaseUrl("fotos.example.com"))
        assertEquals("http://192.168.1.20:2283/api", normalizeImmichApiBaseUrl("http://192.168.1.20:2283"))
    }

    @Test
    fun buildUrlSupportsRelativeAndAbsoluteApiBaseUrl() {
        assertEquals("/api/users/me", buildImmichApiUrl("/api", "users/me"))
        assertEquals("https://fotos.example.com/api/albums", buildImmichApiUrl("https://fotos.example.com", "/albums"))
    }
}
