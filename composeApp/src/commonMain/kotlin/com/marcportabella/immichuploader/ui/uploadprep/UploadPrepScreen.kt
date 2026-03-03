package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.platform.BindPlatformFileInput
import com.marcportabella.immichuploader.platform.openPlatformFilePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UploadPrepScreen(
    store: UploadPrepStore,
    uiLanguage: UiLanguage = UiLanguage.Catalan,
    onUiLanguageChange: (UiLanguage) -> Unit = {},
    onPersistApiKey: (String) -> Unit = {},
    onPersistServerBaseUrl: (String) -> Unit = {},
    enableWebEffects: Boolean = true
) {
    val stateHolder = rememberUploadPrepStateHolder(store)
    val state = stateHolder.state
    val scope = rememberCoroutineScope()
    var keyOwnerName by remember { mutableStateOf<String?>(null) }
    var keyOwnerLookupInProgress by remember { mutableStateOf(false) }
    var keyOwnerLookupFailed by remember { mutableStateOf(false) }
    val sortedAssets = remember(state.assets) {
        state.assets.values.sortedBy { it.fileName }
    }

    if (enableWebEffects) {
        BindPlatformFileInput { nextFiles ->
            scope.launch { stateHolder.onFilesSelected(nextFiles) }
        }
    }

    if (enableWebEffects) {
        LaunchedEffect(state.apiKey, state.serverBaseUrl) {
            if (state.apiKey.isNotBlank() && state.serverBaseUrl.isNotBlank()) {
                delay(350)
                stateHolder.loadCatalogAtInit()
            }
        }
    }

    if (enableWebEffects) {
        LaunchedEffect(state.apiKey, state.serverBaseUrl, state.assets.size) {
            if (state.assets.isNotEmpty() && state.apiKey.isNotBlank() && state.serverBaseUrl.isNotBlank()) {
                delay(350)
                stateHolder.runDuplicateCheckForCurrentAssets()
            }
        }
    }

    if (enableWebEffects) {
        LaunchedEffect(state.apiKey, state.serverBaseUrl) {
            if (state.apiKey.isBlank() || state.serverBaseUrl.isBlank()) {
                keyOwnerName = null
                keyOwnerLookupInProgress = false
                keyOwnerLookupFailed = false
                return@LaunchedEffect
            }
            keyOwnerLookupInProgress = true
            keyOwnerLookupFailed = false
            keyOwnerName = null
            delay(350)
            when (val result = stateHolder.lookupApiKeyOwner()) {
                ApiKeyOwnerLookupResult.MissingApiKey -> {
                    keyOwnerName = null
                    keyOwnerLookupFailed = false
                }

                is ApiKeyOwnerLookupResult.Success -> {
                    keyOwnerName = result.displayName
                    keyOwnerLookupFailed = false
                }

                is ApiKeyOwnerLookupResult.Failed -> {
                    keyOwnerName = null
                    keyOwnerLookupFailed = true
                }
            }
            keyOwnerLookupInProgress = false
        }
    }

    UploadPrepScreenContent(
        state = state,
        uiLanguage = uiLanguage,
        onUiLanguageChange = onUiLanguageChange,
        onApiKeyChange = { nextValue ->
            stateHolder.setApiKey(nextValue)
            onPersistApiKey(nextValue)
        },
        onServerBaseUrlChange = { nextValue ->
            stateHolder.setServerBaseUrl(nextValue)
            onPersistServerBaseUrl(nextValue)
        },
        keyOwnerName = keyOwnerName,
        keyOwnerLookupInProgress = keyOwnerLookupInProgress,
        keyOwnerLookupFailed = keyOwnerLookupFailed,
        gateStatus = stateHolder.gateStatus,
        executionPath = stateHolder.executionPath,
        catalogGateStatus = stateHolder.catalogGateStatus,
        sortedAssets = sortedAssets,
        bulkPreflightMessage = stateHolder.bulkPreflightMessage,
        onOpenFilePicker = { openPlatformFilePicker() },
        onBulkDraftChange = { draft -> stateHolder.updateBulkDraft(draft) },
        onCreateSessionAlbumForBulk = { name -> stateHolder.createSessionAlbumForBulk(name) },
        onCreateSessionTagForBulk = { name -> stateHolder.createSessionTagForBulk(name) },
        onApplyBulk = { stateHolder.applyBulk() },
        onClearBulkDraft = { stateHolder.clearBulkDraft() },
        onClearBatchStaged = { stateHolder.clearBatchStaged() },
        onClearCatalogMessage = { stateHolder.clearCatalogMessage() },
        onDismissBatchFeedback = { stateHolder.dismissBatchFeedback() },
        onGeneratePlan = { stateHolder.generatePlan() },
        onClearPlan = { stateHolder.clearPlan() },
        onExecute = { scope.launch { stateHolder.executePlan() } },
        onClearExecutionStatus = { stateHolder.clearExecutionStatus() },
        canApplyBulkEdit = { stateHolder.canApplyBulkEdit() }
    )
}

@Preview(widthDp = 1600, heightDp = 900, showBackground = true)
@Composable
private fun UploadPrepScreenRoutePreview(
    @PreviewParameter(UploadPrepScreenPreviewProvider::class) model: UploadPrepScreenPreviewModel
) {
    val previewStore = UploadPrepStore(model.state)
    MaterialTheme {
        UploadPrepScreen(
            store = previewStore,
            uiLanguage = UiLanguage.Catalan,
            onUiLanguageChange = {},
            onPersistApiKey = {},
            onPersistServerBaseUrl = {},
            enableWebEffects = false
        )
    }
}
