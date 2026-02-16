package com.marcportabella.immichuploader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.browser.document
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

@Composable
fun UploadPrepScreen(store: UploadPrepStore) {
    val state = store.state
    val scope = rememberCoroutineScope()

    DisposableEffect(store) {
        val input = document.getElementById("local-file-input") as? HTMLInputElement
        if (input == null) {
            onDispose { }
        } else {
            val listener: (Event) -> Unit = {
                val fileList = input.files
                if (fileList != null) {
                    val nextFiles = mutableListOf<LocalIntakeFile>()
                    for (index in 0 until fileList.length) {
                        val file = fileList.item(index) ?: continue
                        nextFiles += file.toLocalIntakeFile()
                    }

                    store.state.assets.values.mapNotNull { it.previewUrl }.forEach {
                        revokeObjectUrl(it)
                    }

                    val assets = mapLocalIntakeFilesToAssets(nextFiles)
                    store.dispatch(UploadPrepAction.ReplaceAssets(assets))
                    store.dispatch(UploadPrepAction.ClearSelection)
                    input.value = ""
                }
            }

            input.addEventListener("change", listener)
            onDispose {
                input.removeEventListener("change", listener)
                store.state.assets.values.mapNotNull { it.previewUrl }.forEach {
                    revokeObjectUrl(it)
                }
            }
        }
    }

    val transport = remember { ApiKeyGatedImmichTransport(ApiImmichOnlineTransport()) }
    val gateStatus = transport.gateStatus(apiKey = state.apiKey.ifBlank { null })
    val executionPath = transport.selectExecutionPath(apiKey = state.apiKey.ifBlank { null })
    val catalogTransport = remember { ApiKeyGatedImmichCatalogTransport(DryRunImmichCatalogTransport()) }
    val catalogGateStatus = catalogTransport.gateStatus(state.apiKey.ifBlank { null })
    val bulkPreflightFeedback = preflightBulkEditDraft(state)

    Column(
        modifier = Modifier
            .background(androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Immich Upload Prep")
        Text("Assets loaded: ${state.assets.size}")
        Text("Selected: ${state.selectedAssetIds.size}")
        Text("Staged edits: ${state.stagedEditsByAssetId.size}")
        Text("Transport gate: $gateStatus")
        Text("Execution path: $executionPath")
        Text("Catalog gate: $catalogGateStatus")

        OutlinedTextField(
            value = state.apiKey,
            onValueChange = { store.dispatch(UploadPrepAction.SetApiKey(it)) },
            label = { Text("Immich API key (required for lookup/create)") },
            modifier = Modifier.fillMaxWidth()
        )

        CatalogSection(
            state = state,
            onLookupAlbums = {
                scope.launch {
                    store.dispatch(UploadPrepAction.CatalogRequestStarted)
                    when (val result = catalogTransport.lookupAlbums(state.apiKey.ifBlank { null })) {
                        is ImmichCatalogResult.BlockedMissingApiKey ->
                            store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                        is ImmichCatalogResult.DryRunSuccess ->
                            store.dispatch(UploadPrepAction.CatalogAlbumsLoaded(result.entries, result.message))
                    }
                }
            },
            onLookupTags = {
                scope.launch {
                    store.dispatch(UploadPrepAction.CatalogRequestStarted)
                    when (val result = catalogTransport.lookupTags(state.apiKey.ifBlank { null })) {
                        is ImmichCatalogResult.BlockedMissingApiKey ->
                            store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                        is ImmichCatalogResult.DryRunSuccess ->
                            store.dispatch(UploadPrepAction.CatalogTagsLoaded(result.entries, result.message))
                    }
                }
            },
            onAlbumDraftChange = { store.dispatch(UploadPrepAction.SetAlbumCreateDraft(it)) },
            onTagDraftChange = { store.dispatch(UploadPrepAction.SetTagCreateDraft(it)) },
            onCreateAlbum = {
                scope.launch {
                    store.dispatch(UploadPrepAction.CatalogRequestStarted)
                    when (
                        val result = catalogTransport.createAlbumIfMissing(
                            state.apiKey.ifBlank { null },
                            state.albumCreateDraft
                        )
                    ) {
                        is ImmichCatalogResult.BlockedMissingApiKey ->
                            store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                        is ImmichCatalogResult.DryRunSuccess -> {
                            store.dispatch(UploadPrepAction.CatalogAlbumsLoaded(result.entries, result.message))
                            store.dispatch(UploadPrepAction.SetAlbumCreateDraft(""))
                        }
                    }
                }
            },
            onCreateTag = {
                scope.launch {
                    store.dispatch(UploadPrepAction.CatalogRequestStarted)
                    when (
                        val result = catalogTransport.createTagIfMissing(
                            state.apiKey.ifBlank { null },
                            state.tagCreateDraft
                        )
                    ) {
                        is ImmichCatalogResult.BlockedMissingApiKey ->
                            store.dispatch(UploadPrepAction.CatalogBlockedMissingApiKey(result.message))

                        is ImmichCatalogResult.DryRunSuccess -> {
                            store.dispatch(UploadPrepAction.CatalogTagsLoaded(result.entries, result.message))
                            store.dispatch(UploadPrepAction.SetTagCreateDraft(""))
                        }
                    }
                }
            },
            onClearMessage = { store.dispatch(UploadPrepAction.ClearCatalogMessage) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = {
                    val input = document.getElementById("local-file-input") as? HTMLInputElement
                    input?.click()
                }
            ) {
                Text("Select local media")
            }

            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = { store.dispatch(UploadPrepAction.SelectAll) },
                enabled = state.assets.isNotEmpty()
            ) {
                Text("Select all")
            }

            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = { store.dispatch(UploadPrepAction.ClearSelection) },
                enabled = state.selectedAssetIds.isNotEmpty()
            ) {
                Text("Clear selection")
            }
        }

        BulkEditSection(
            draft = state.bulkEditDraft,
            selectedCount = state.selectedAssetIds.size,
            applyEnabled = canApplyBulkEdit(state),
            preflightMessage = bulkPreflightFeedback?.message,
            onDraftChange = { store.dispatch(UploadPrepAction.SetBulkEditDraft(it)) },
            onApply = { store.dispatch(UploadPrepAction.ApplyBulkEditDraftToSelected) },
            onClearDraft = { store.dispatch(UploadPrepAction.ClearBulkEditDraft) },
            onClearSelectedStaged = { store.dispatch(UploadPrepAction.ClearStagedForSelected) }
        )

        if (state.batchFeedback != null) {
            BatchFeedbackBanner(
                feedback = state.batchFeedback,
                onDismiss = { store.dispatch(UploadPrepAction.ClearBatchFeedback) }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { store.dispatch(UploadPrepAction.GenerateDryRunPreview) },
                enabled = state.selectedAssetIds.isNotEmpty()
            ) {
                Text("Generate dry-run plan")
            }
            Button(
                onClick = { store.dispatch(UploadPrepAction.ClearDryRunPreview) },
                enabled = state.dryRunPlan != null
            ) {
                Text("Clear dry-run")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val plan = state.dryRunPlan ?: return@Button
                    scope.launch {
                        store.dispatch(
                            UploadPrepAction.UploadExecutionStarted(
                                "Executing ${state.dryRunApiRequests.size} API requests."
                            )
                        )
                        when (val result = transport.submit(plan = plan, apiKey = state.apiKey.ifBlank { null })) {
                            is ImmichTransportResult.BlockedMissingApiKey ->
                                store.dispatch(
                                    UploadPrepAction.UploadExecutionBlocked(
                                        "API key required. Upload execution remained blocked."
                                    )
                                )

                            is ImmichTransportResult.DryRun ->
                                store.dispatch(
                                    UploadPrepAction.UploadExecutionFailed(
                                        "Execution stayed in dry-run mode. Configure executable transport first."
                                    )
                                )

                            is ImmichTransportResult.Submitted ->
                                store.dispatch(
                                    UploadPrepAction.UploadExecutionSubmitted(
                                        requestCount = result.requestCount,
                                        message = "Submitted ${result.requestCount} API requests."
                                    )
                                )

                            is ImmichTransportResult.Failed ->
                                store.dispatch(
                                    UploadPrepAction.UploadExecutionFailed(result.message)
                                )
                        }
                    }
                },
                enabled = state.dryRunPlan != null && state.executionStatus != UploadExecutionStatus.Executing
            ) {
                Text("Execute API upload")
            }
            Button(
                onClick = { store.dispatch(UploadPrepAction.ClearUploadExecutionStatus) },
                enabled = state.executionMessage != null || state.executionStatus != UploadExecutionStatus.Idle
            ) {
                Text("Clear execution status")
            }
        }

        Text("Execution status: ${state.executionStatus}")
        if (state.executionMessage != null) {
            Text("Execution message: ${state.executionMessage}")
        }
        if (state.executionRequestCount != null) {
            Text("Submitted requests: ${state.executionRequestCount}")
        }

        DryRunInspectorSection(state)

        AssetQueueSection(
            state = state,
            onToggleSelection = { store.dispatch(UploadPrepAction.ToggleSelection(it)) }
        )
    }
}
