package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.marcportabella.immichuploader.data.ApiImmichOnlineCatalogTransport
import com.marcportabella.immichuploader.data.ApiImmichOnlineTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichCatalogTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichTransport
import com.marcportabella.immichuploader.data.ImmichCatalogResult
import com.marcportabella.immichuploader.data.ImmichTransportResult
import com.marcportabella.immichuploader.data.toDataRequestPlan
import com.marcportabella.immichuploader.data.toDomainCatalogEntry
import com.marcportabella.immichuploader.domain.AssetEditPatch
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.LocalAsset
import com.marcportabella.immichuploader.domain.LocalAssetId
import com.marcportabella.immichuploader.domain.LocalIntakeFile
import com.marcportabella.immichuploader.domain.UploadPrepAction
import com.marcportabella.immichuploader.domain.UploadPrepState
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.domain.canApplyBulkEdit
import com.marcportabella.immichuploader.domain.mapLocalIntakeFilesToAssets
import com.marcportabella.immichuploader.domain.preflightBulkEditDraft
import com.marcportabella.immichuploader.platform.revokePlatformPreviewUrl

class UploadPrepStateHolder(
    private val store: UploadPrepStore,
    private val transport: ApiKeyGatedImmichTransport = ApiKeyGatedImmichTransport(ApiImmichOnlineTransport()),
    private val catalogTransport: ApiKeyGatedImmichCatalogTransport = ApiKeyGatedImmichCatalogTransport(ApiImmichOnlineCatalogTransport())
) {
    private var catalogLoadedAtInit = false

    val state: UploadPrepState
        get() = store.state

    val gateStatus: String
        get() = transport.gateStatus(apiKey = apiKeyOrNull).toString()

    val executionPath: String
        get() = transport.selectExecutionPath(apiKey = apiKeyOrNull).toString()

    val catalogGateStatus: String
        get() = catalogTransport.gateStatus(apiKeyOrNull).toString()

    val sortedAssets: List<LocalAsset>
        get() = state.assets.values.sortedBy { it.fileName }

    val selectedAssets: List<LocalAsset>
        get() = state.selectedAssetIds.mapNotNull { state.assets[it] }.sortedBy { it.fileName }

    val bulkPreflightMessage: String?
        get() = preflightBulkEditDraft(state)?.message

    suspend fun onFilesSelected(nextFiles: List<LocalIntakeFile>) {
        state.assets.values.mapNotNull { it.previewUrl }.forEach { revokePlatformPreviewUrl(it) }
        val assets = mapLocalIntakeFilesToAssets(nextFiles)
        dispatch(UploadPrepAction.ReplaceAssets(assets))
        dispatch(UploadPrepAction.ClearSelection)
    }

    suspend fun loadCatalogAtInit() {
        if (catalogLoadedAtInit) return
        catalogLoadedAtInit = true

        dispatch(UploadPrepAction.CatalogRequestStarted)
        val apiKey = apiKeyOrNull
        when (val albumResult = catalogTransport.lookupAlbums(apiKey)) {
            is ImmichCatalogResult.BlockedMissingApiKey ->
                dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(albumResult.message))

            is ImmichCatalogResult.Success ->
                dispatch(
                    UploadPrepAction.CatalogAlbumsLoaded(
                        albumResult.entries.map { it.toDomainCatalogEntry() },
                        albumResult.message
                    )
                )
        }
        when (val tagResult = catalogTransport.lookupTags(apiKey)) {
            is ImmichCatalogResult.BlockedMissingApiKey ->
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

    suspend fun executePlan() {
        val snapshot = state
        val plan = snapshot.dryRunPlan ?: return

        dispatch(UploadPrepAction.UploadExecutionStarted("Executing ${snapshot.dryRunApiRequests.size} API requests."))
        when (val result = transport.submit(plan = plan.toDataRequestPlan(), apiKey = snapshot.apiKey.ifBlank { null })) {
            is ImmichTransportResult.BlockedMissingApiKey ->
                dispatch(UploadPrepAction.UploadExecutionBlocked("API key required. Upload execution remained blocked."))

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

    fun selectAll() = dispatch(UploadPrepAction.SelectAll)

    fun clearSelection() = dispatch(UploadPrepAction.ClearSelection)

    fun toggleSelection(assetId: LocalAssetId) = dispatch(UploadPrepAction.ToggleSelection(assetId))

    fun patchSingleAsset(assetId: LocalAssetId, patch: AssetEditPatch) =
        dispatch(UploadPrepAction.StageEditForAsset(assetId, patch))

    fun replaceSingleAssetTagSelection(
        assetId: LocalAssetId,
        addTagIds: Set<String>,
        removeTagIds: Set<String>
    ) = dispatch(UploadPrepAction.ReplaceTagEditsForAsset(assetId, addTagIds, removeTagIds))

    fun clearSingleSelectionStaged() = dispatch(UploadPrepAction.ClearStagedForSelected)

    fun updateBulkDraft(draft: BulkEditDraft) = dispatch(UploadPrepAction.SetBulkEditDraft(draft))

    fun applyBulk() = dispatch(UploadPrepAction.ApplyBulkEditDraftToSelected)

    fun clearBulkDraft() = dispatch(UploadPrepAction.ClearBulkEditDraft)

    fun clearSelectedStaged() = dispatch(UploadPrepAction.ClearStagedForSelected)

    fun createSessionTagForBulk(name: String) = dispatch(UploadPrepAction.CreateSessionTagForBulk(name))

    fun createSessionTagForAsset(assetId: LocalAssetId, name: String) =
        dispatch(UploadPrepAction.CreateSessionTagForAsset(assetId, name))

    fun clearCatalogMessage() = dispatch(UploadPrepAction.ClearCatalogMessage)

    fun dismissBatchFeedback() = dispatch(UploadPrepAction.ClearBatchFeedback)

    fun generatePlan() = dispatch(UploadPrepAction.GenerateDryRunPreview)

    fun clearPlan() = dispatch(UploadPrepAction.ClearDryRunPreview)

    fun clearExecutionStatus() = dispatch(UploadPrepAction.ClearUploadExecutionStatus)

    fun canApplyBulkEdit(): Boolean = canApplyBulkEdit(state)

    private fun dispatch(action: UploadPrepAction) {
        store.dispatch(action)
    }

    private val apiKeyOrNull: String?
        get() = state.apiKey.ifBlank { null }
}

@Composable
fun rememberUploadPrepStateHolder(store: UploadPrepStore): UploadPrepStateHolder =
    remember(store) { UploadPrepStateHolder(store) }
