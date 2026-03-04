package com.marcportabella.immichuploader.data

internal actual fun platformRewriteRequestUrl(rawUrl: String): String = jsRewriteRequestUrl(rawUrl)

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(url) => {
  try {
    const input = (url ?? '').toString();
    const current = globalThis.location;
    if (!current || !current.origin) return input;
    const host = (current.hostname ?? '').toLowerCase();
    const isLocalDevOrigin = host === 'localhost' || host === '127.0.0.1' || host === '::1' || host === '[::1]';
    if (isLocalDevOrigin) return input;

    const hasScheme = /^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(input);
    if (!hasScheme) return input;

    const target = new URL(input);
    if (target.origin === current.origin) {
      return target.pathname + target.search;
    }

    const scheme = target.protocol.replace(':', '').toLowerCase();
    if (scheme !== 'http' && scheme !== 'https') return input;

    return '/__immich_proxy/' + scheme + '/' + target.host + target.pathname + target.search;
  } catch (_) {
    return url;
  }
}"""
)
private external fun jsRewriteRequestUrl(url: String): String
