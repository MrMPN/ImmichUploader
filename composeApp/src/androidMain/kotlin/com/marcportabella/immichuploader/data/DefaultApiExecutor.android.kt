package com.marcportabella.immichuploader.data

actual fun defaultImmichApiExecutor(): ImmichApiExecutor = object : ImmichApiExecutor {
    override suspend fun execute(request: ImmichApiRequest, apiKey: String): ImmichApiExecutorResult {
        throw UnsupportedOperationException("Android target is preview-only in this project.")
    }
}
