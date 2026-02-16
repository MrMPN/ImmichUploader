package com.marcportabella.immichuploader.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.data.ApiImmichOnlineTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichCatalogTransport
import com.marcportabella.immichuploader.data.ApiKeyGatedImmichTransport
import com.marcportabella.immichuploader.data.DryRunImmichCatalogTransport
import com.marcportabella.immichuploader.data.ImmichCatalogResult
import com.marcportabella.immichuploader.data.ImmichTransportResult
import com.marcportabella.immichuploader.domain.UploadExecutionStatus
import com.marcportabella.immichuploader.domain.UploadPrepAction
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.domain.canApplyBulkEdit
import com.marcportabella.immichuploader.domain.mapLocalIntakeFilesToAssets
import com.marcportabella.immichuploader.domain.preflightBulkEditDraft
import com.marcportabella.immichuploader.web.revokeObjectUrl
import com.marcportabella.immichuploader.web.toLocalIntakeFile
import kotlinx.browser.document
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event

@OptIn(ExperimentalLayoutApi::class)
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
                    val nextFiles = mutableListOf<com.marcportabella.immichuploader.domain.LocalIntakeFile>()
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
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Immich Upload Prep",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Prepare metadata, validate the dry-run payload, then execute upload safely.",
                    style = MaterialTheme.typography.bodyMedium
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryPill("Assets", state.assets.size.toString())
                    SummaryPill("Selected", state.selectedAssetIds.size.toString())
                    SummaryPill("Staged", state.stagedEditsByAssetId.size.toString())
                    SummaryPill("Transport", gateStatus.toString())
                    SummaryPill("Execution", executionPath.toString())
                    SummaryPill("Catalog", catalogGateStatus.toString())
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "API key is required for catalog lookups, catalog create operations, and upload execution.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = state.apiKey,
                    onValueChange = { store.dispatch(UploadPrepAction.SetApiKey(it)) },
                    label = { Text("Immich API key") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Queue selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Load local media first, then select assets for bulk actions and dry-run generation.",
                    style = MaterialTheme.typography.bodySmall
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val input = document.getElementById("local-file-input") as? HTMLInputElement
                            input?.click()
                        }
                    ) {
                        Text("Select local media")
                    }
                    Button(
                        onClick = { store.dispatch(UploadPrepAction.SelectAll) },
                        enabled = state.assets.isNotEmpty()
                    ) {
                        Text("Select all")
                    }
                    Button(
                        onClick = { store.dispatch(UploadPrepAction.ClearSelection) },
                        enabled = state.selectedAssetIds.isNotEmpty()
                    ) {
                        Text("Clear selection")
                    }
                }
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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Dry-run and execution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Generate a preview before execution to verify payload details and request count.",
                    style = MaterialTheme.typography.bodySmall
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                HorizontalDivider()
                Text("Execution status: ${state.executionStatus}")
                if (state.executionMessage != null) {
                    Text("Execution message: ${state.executionMessage}")
                }
                if (state.executionRequestCount != null) {
                    Text("Submitted requests: ${state.executionRequestCount}")
                }
            }
        }

        DryRunInspectorSection(state)

        AssetQueueSection(
            state = state,
            onToggleSelection = { store.dispatch(UploadPrepAction.ToggleSelection(it)) }
        )
    }
}

@Composable
private fun SummaryPill(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = "$label:", style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
