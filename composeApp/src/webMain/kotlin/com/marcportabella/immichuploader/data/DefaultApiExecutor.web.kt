package com.marcportabella.immichuploader.data

actual fun defaultImmichApiExecutor(): ImmichApiExecutor = BrowserImmichApiExecutor()
