package com.marcportabella.immichuploader.app

import com.marcportabella.immichuploader.data.normalizeImmichApiBaseUrl

private const val API_KEY_STORAGE_KEY = "immichuploader.api_key"
private const val API_KEY_COOKIE_NAME = "immichuploader_api_key"
private const val SERVER_BASE_URL_STORAGE_KEY = "immichuploader.server_base_url"
private const val SERVER_BASE_URL_COOKIE_NAME = "immichuploader_server_base_url"
private const val API_KEY_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365

internal fun loadPersistedApiKey(): String? {
    val fromStorage = jsLocalStorageGet(API_KEY_STORAGE_KEY)?.trim().orEmpty()
    if (fromStorage.isNotEmpty()) {
        return fromStorage
    }
    val fromCookie = parseCookieValue(jsReadCookies(), API_KEY_COOKIE_NAME)?.trim().orEmpty()
    return fromCookie.ifEmpty { null }
}

internal fun persistApiKey(apiKey: String) {
    val normalized = apiKey.trim()
    if (normalized.isEmpty()) {
        jsLocalStorageRemove(API_KEY_STORAGE_KEY)
        jsClearCookie(API_KEY_COOKIE_NAME)
        return
    }
    jsLocalStorageSet(API_KEY_STORAGE_KEY, normalized)
    jsSetCookie(API_KEY_COOKIE_NAME, normalized, API_KEY_COOKIE_MAX_AGE_SECONDS)
}

internal fun loadPersistedServerBaseUrl(): String? {
    val fromStorage = jsLocalStorageGet(SERVER_BASE_URL_STORAGE_KEY)?.trim().orEmpty()
    if (fromStorage.isNotEmpty()) {
        return normalizeImmichApiBaseUrl(fromStorage).ifEmpty { null }
    }
    val fromCookie = parseCookieValue(jsReadCookies(), SERVER_BASE_URL_COOKIE_NAME)?.trim().orEmpty()
    return normalizeImmichApiBaseUrl(fromCookie).ifEmpty { null }
}

internal fun persistServerBaseUrl(serverBaseUrl: String) {
    val normalized = normalizeImmichApiBaseUrl(serverBaseUrl)
    if (normalized.isEmpty()) {
        jsLocalStorageRemove(SERVER_BASE_URL_STORAGE_KEY)
        jsClearCookie(SERVER_BASE_URL_COOKIE_NAME)
        return
    }
    jsLocalStorageSet(SERVER_BASE_URL_STORAGE_KEY, normalized)
    jsSetCookie(SERVER_BASE_URL_COOKIE_NAME, normalized, API_KEY_COOKIE_MAX_AGE_SECONDS)
}

private fun parseCookieValue(cookieHeader: String, key: String): String? =
    cookieHeader
        .split(';')
        .map { it.trim() }
        .firstOrNull { it.startsWith("$key=") }
        ?.substringAfter('=')
        ?.let { jsDecodeURIComponent(it) }

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => { try { return globalThis.localStorage?.getItem(key) ?? null; } catch (_) { return null; } }")
private external fun jsLocalStorageGet(key: String): String?

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key, value) => { try { globalThis.localStorage?.setItem(key, value); } catch (_) {} }")
private external fun jsLocalStorageSet(key: String, value: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(key) => { try { globalThis.localStorage?.removeItem(key); } catch (_) {} }")
private external fun jsLocalStorageRemove(key: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => (typeof document !== 'undefined' ? document.cookie : '')")
private external fun jsReadCookies(): String

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(name, value, maxAge) => { if (typeof document !== 'undefined') { document.cookie = name + '=' + encodeURIComponent(value) + '; path=/; max-age=' + maxAge + '; samesite=lax'; } }")
private external fun jsSetCookie(name: String, value: String, maxAge: Int)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(name) => { if (typeof document !== 'undefined') { document.cookie = name + '=; path=/; max-age=0; samesite=lax'; } }")
private external fun jsClearCookie(name: String)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(value) => { try { return decodeURIComponent(value); } catch (_) { return value; } }")
private external fun jsDecodeURIComponent(value: String): String
