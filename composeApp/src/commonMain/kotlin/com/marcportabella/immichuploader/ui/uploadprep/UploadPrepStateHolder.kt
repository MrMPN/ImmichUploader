package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.marcportabella.immichuploader.data.ApiImmichOnlineCatalogTransport
import com.marcportabella.immichuploader.data.ApiImmichOnlineTransport
import com.marcportabella.immichuploader.data.ImmichApiExecutor
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichCatalogTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichTransport
import com.marcportabella.immichuploader.data.ImmichCatalogRequestBuilder
import com.marcportabella.immichuploader.data.ImmichCatalogResult
import com.marcportabella.immichuploader.data.ImmichBulkUploadCheckResult
import com.marcportabella.immichuploader.data.ImmichRequestBuilder
import com.marcportabella.immichuploader.data.ImmichTransportResult
import com.marcportabella.immichuploader.data.defaultImmichApiExecutor
import com.marcportabella.immichuploader.data.immichJson
import com.marcportabella.immichuploader.data.normalizeImmichApiBaseUrl
import com.marcportabella.immichuploader.data.toDomainCatalogEntry
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.LocalIntakeFile
import com.marcportabella.immichuploader.domain.UploadPrepAction
import com.marcportabella.immichuploader.domain.UploadPrepState
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.domain.canApplyBulkEdit
import com.marcportabella.immichuploader.domain.mapLocalIntakeFilesToAssets
import com.marcportabella.immichuploader.domain.preflightBulkEditDraft
import com.marcportabella.immichuploader.platform.diagnosticMessage
import com.marcportabella.immichuploader.platform.platformLogError
import com.marcportabella.immichuploader.platform.platformLogInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed interface ApiKeyOwnerLookupResult {
    data object MissingApiKey : ApiKeyOwnerLookupResult
    data class Success(val displayName: String) : ApiKeyOwnerLookupResult
    data class Failed(val message: String) : ApiKeyOwnerLookupResult
}

class UploadPrepStateHolder(
    private val store: UploadPrepStore,
    private val transport: ApiKeyGatedImmichTransport = ApiKeyGatedImmichTransport(ApiImmichOnlineTransport()),
    private val catalogTransport: ApiKeyGatedImmichCatalogTransport = ApiKeyGatedImmichCatalogTransport(ApiImmichOnlineCatalogTransport()),
    private val apiExecutor: ImmichApiExecutor = defaultImmichApiExecutor()
) {
    val state: UploadPrepState
        get() = store.state

    val gateStatus: String
        get() = transport.gateStatus(apiKey = apiKeyOrNull, serverBaseUrl = serverBaseUrlOrNull).toString()

    val executionPath: String
        get() = transport.selectExecutionPath(apiKey = apiKeyOrNull, serverBaseUrl = serverBaseUrlOrNull).toString()

    val catalogGateStatus: String
        get() = catalogTransport.gateStatus(apiKey = apiKeyOrNull, serverBaseUrl = serverBaseUrlOrNull).toString()

    val bulkPreflightMessage: String?
        get() = preflightBulkEditDraft(state)?.message

    suspend fun onFilesSelected(nextFiles: List<LocalIntakeFile>) {
        val assets = mapLocalIntakeFilesToAssets(nextFiles)
        dispatch(UploadPrepAction.ReplaceAssets(assets))
    }

    suspend fun loadCatalogAtInit() {
        dispatch(UploadPrepAction.CatalogRequestStarted)
        when (val albumResult = catalogTransport.lookupAlbums(apiKey = apiKeyOrNull, serverBaseUrl = serverBaseUrlOrNull)) {
            is ImmichCatalogResult.BlockedMissingApiKey ->
                dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(albumResult.message))

            is ImmichCatalogResult.BlockedMissingServerBaseUrl ->
                dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(albumResult.message))

            is ImmichCatalogResult.Success ->
                dispatch(
                    UploadPrepAction.CatalogAlbumsLoaded(
                        albumResult.entries.map { it.toDomainCatalogEntry() },
                        albumResult.message
                    )
                )
        }
        when (val tagResult = catalogTransport.lookupTags(apiKey = apiKeyOrNull, serverBaseUrl = serverBaseUrlOrNull)) {
            is ImmichCatalogResult.BlockedMissingApiKey ->
                dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(tagResult.message))

            is ImmichCatalogResult.BlockedMissingServerBaseUrl ->
                dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(tagResult.message))

            is ImmichCatalogResult.Success ->
                dispatch(
                    UploadPrepAction.CatalogTagsLoaded(
                        tagResult.entries.map { it.toDomainCatalogEntry() },
                        tagResult.message
                    )
                )
        }
    }

    fun setApiKey(value: String) {
        dispatch(UploadPrepAction.SetApiKey(value.trim()))
    }

    suspend fun lookupApiKeyOwner(): ApiKeyOwnerLookupResult {
        val apiKey = apiKeyOrNull ?: return ApiKeyOwnerLookupResult.MissingApiKey
        val serverBaseUrl = serverBaseUrlOrNull
            ?: return ApiKeyOwnerLookupResult.Failed("Immich server URL is required.")
        val request = ImmichCatalogRequestBuilder.lookupCurrentUser(serverBaseUrl)
        return runCatching { apiExecutor.execute(request = request, apiKey = apiKey) }
            .fold(
                onSuccess = { response ->
                    if (response.statusCode !in 200..299) {
                        ApiKeyOwnerLookupResult.Failed("User lookup failed with HTTP ${response.statusCode}.")
                    } else {
                        val displayName = parseUserDisplayName(response.responseBody)
                        if (displayName == null) {
                            ApiKeyOwnerLookupResult.Failed("User lookup returned an unexpected payload.")
                        } else {
                            ApiKeyOwnerLookupResult.Success(displayName)
                        }
                    }
                },
                onFailure = { throwable ->
                    val error = throwable.diagnosticMessage()
                    platformLogError(
                        "[immichuploader][auth] key owner lookup failed: ${throwable.diagnosticMessage()}"
                    )
                    ApiKeyOwnerLookupResult.Failed(error)
                }
            )
    }

    suspend fun executePlan() {
        val snapshot = state
        if (snapshot.dryRunPlan == null) return
        val executionPlan = ImmichRequestBuilder.buildDryRunPlan(snapshot)

        dispatch(UploadPrepAction.UploadExecutionStarted("Executing ${snapshot.dryRunApiRequests.size} API requests."))
        when (
            val result = transport.submit(
                plan = executionPlan,
                apiKey = snapshot.apiKey.ifBlank { null },
                serverBaseUrl = snapshot.serverBaseUrl.ifBlank { null }
            )
        ) {
            is ImmichTransportResult.BlockedMissingApiKey ->
                dispatch(UploadPrepAction.UploadExecutionBlocked("API key required. Upload execution remained blocked."))

            is ImmichTransportResult.BlockedMissingServerBaseUrl ->
                dispatch(UploadPrepAction.UploadExecutionBlocked("Immich server URL required. Upload execution remained blocked."))

            is ImmichTransportResult.Submitted ->
                dispatch(
                    UploadPrepAction.UploadExecutionSubmitted(
                        requestCount = result.requestCount,
                        message = "Submitted ${result.requestCount} API requests."
                    )
                )

            is ImmichTransportResult.Failed ->
                dispatch(UploadPrepAction.UploadExecutionFailed(result.message))
        }
    }

    fun updateBulkDraft(draft: BulkEditDraft) = dispatch(UploadPrepAction.SetBulkEditDraft(draft))

    fun applyBulk() = dispatch(UploadPrepAction.ApplyBulkEditDraftToSelected)

    fun clearBulkDraft() = dispatch(UploadPrepAction.ClearBulkEditDraft)

    fun clearBatchStaged() = dispatch(UploadPrepAction.ClearStagedForSelected)

    fun createSessionAlbumForBulk(name: String) = dispatch(UploadPrepAction.CreateSessionAlbumForBulk(name))

    fun createSessionAlbumForAsset(assetId: LocalAssetId, name: String) =
        dispatch(UploadPrepAction.CreateSessionAlbumForAsset(assetId, name))

    fun createSessionTagForBulk(name: String) = dispatch(UploadPrepAction.CreateSessionTagForBulk(name))

    fun createSessionTagForAsset(assetId: LocalAssetId, name: String) =
        dispatch(UploadPrepAction.CreateSessionTagForAsset(assetId, name))

    fun clearCatalogMessage() = dispatch(UploadPrepAction.ClearCatalogMessage)

    fun dismissBatchFeedback() = dispatch(UploadPrepAction.ClearBatchFeedback)

    fun generatePlan() {
        val before = state
        platformLogInfo(
            "[immichuploader][plan] generate requested batch=${before.selectedAssetIds.size} staged=${before.stagedEditsByAssetId.size}"
        )
        dispatch(UploadPrepAction.GenerateDryRunPreview)
        val after = state
        platformLogInfo(
            "[immichuploader][plan] generate result requests=${after.dryRunApiRequests.size} message=${after.dryRunMessage ?: "<none>"}"
        )
    }

    fun clearPlan() = dispatch(UploadPrepAction.ClearDryRunPreview)

    fun clearExecutionStatus() = dispatch(UploadPrepAction.ClearUploadExecutionStatus)

    fun canApplyBulkEdit(): Boolean = canApplyBulkEdit(state)

    suspend fun runDuplicateCheckForCurrentAssets() {
        val snapshot = state
        val items = snapshot.assets.values
            .mapNotNull { asset ->
                val checksum = asset.checksum ?: return@mapNotNull null
                com.marcportabella.immichuploader.data.ImmichBulkUploadCheckItem(
                    id = asset.id.value,
                    checksum = checksum,
                    originalPath = asset.fileName,
                    relativePath = asset.fileName
                )
            }
            .distinctBy { it.id }
        platformLogInfo(
            "[immichuploader][dedup] start assets=${snapshot.assets.size} items=${items.size} apiKeyPresent=${!apiKeyOrNull.isNullOrBlank()}"
        )
        if (items.isEmpty()) {
            platformLogInfo("[immichuploader][dedup] skipped reason=no-checksums")
            dispatch(
                UploadPrepAction.DuplicateCheckCompleted(
                    duplicateAssetIds = emptySet(),
                    message = "Duplicate check skipped. No checksums available."
                )
            )
            return
        }

        dispatch(UploadPrepAction.DuplicateCheckStarted)
        when (
            val result = catalogTransport.bulkUploadCheck(
                apiKey = apiKeyOrNull,
                serverBaseUrl = serverBaseUrlOrNull,
                items = items
            )
        ) {
            is ImmichBulkUploadCheckResult.BlockedMissingApiKey -> {
                platformLogInfo("[immichuploader][dedup] blocked reason=missing-api-key")
                dispatch(UploadPrepAction.DuplicateCheckBlockedMissingApiKey(result.message))
            }

            is ImmichBulkUploadCheckResult.BlockedMissingServerBaseUrl -> {
                platformLogInfo("[immichuploader][dedup] blocked reason=missing-server-url")
                dispatch(UploadPrepAction.DuplicateCheckBlockedMissingApiKey(result.message))
            }

            is ImmichBulkUploadCheckResult.Success -> {
                val duplicateAssetIds = snapshot.assets.values
                    .filter { asset -> result.existingAssetIdByItemId.containsKey(asset.id.value) }
                    .map { it.id }
                    .toSet()
                platformLogInfo(
                    "[immichuploader][dedup] completed serverMatches=${result.existingAssetIdByItemId.size} localDuplicates=${duplicateAssetIds.size}"
                )
                dispatch(
                    UploadPrepAction.DuplicateCheckCompleted(
                        duplicateAssetIds = duplicateAssetIds,
                        message = result.message
                    )
                )
            }
        }
    }

    private fun dispatch(action: UploadPrepAction) {
        store.dispatch(action)
    }

    private val apiKeyOrNull: String?
        get() = state.apiKey.ifBlank { null }

    private val serverBaseUrlOrNull: String?
        get() = normalizeImmichApiBaseUrl(state.serverBaseUrl).ifBlank { null }

    private fun parseUserDisplayName(responseBody: String): String? {
        val root = runCatching { immichJson.parseToJsonElement(responseBody).jsonObject }.getOrNull() ?: return null
        val candidate = extractDisplayName(root) ?: extractDisplayName(root["user"] as? JsonObject)
        return candidate?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun extractDisplayName(root: JsonObject?): String? = root?.let {
        it["name"]?.jsonPrimitive?.contentOrNull
            ?: it["email"]?.jsonPrimitive?.contentOrNull
            ?: it["id"]?.jsonPrimitive?.contentOrNull
    }
}

@Composable
fun rememberUploadPrepStateHolder(store: UploadPrepStore): UploadPrepStateHolder =
    remember(store) { UploadPrepStateHolder(store) }
