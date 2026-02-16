package com.marcportabella.immichuploader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    MaterialTheme {
        val firstId = remember { LocalAssetId("local-1") }
        val secondId = remember { LocalAssetId("local-2") }
        val store = remember {
            UploadPrepStore(
                UploadPrepState(
                    assets = mapOf(
                        firstId to LocalAsset(
                            id = firstId,
                            fileName = "sample-1.jpg",
                            mimeType = "image/jpeg",
                            fileSizeBytes = 1_024,
                            captureDateTime = "2026-01-01T09:00:00Z",
                            timeZone = "UTC"
                        ),
                        secondId to LocalAsset(
                            id = secondId,
                            fileName = "sample-2.jpg",
                            mimeType = "image/jpeg",
                            fileSizeBytes = 2_048,
                            captureDateTime = null,
                            timeZone = null
                        )
                    ),
                    selectedAssetIds = setOf(firstId)
                )
            )
        }
        val state = store.state

        val stagedBulkRequest = remember(state.stagedEditsByAssetId) {
            val selection = state.selectedAssetIds.map { "remote-${it.value}" }.toSet()
            val combinedPatch = state.selectedAssetIds
                .mapNotNull { state.stagedEditsByAssetId[it] }
                .fold<AssetEditPatch, AssetEditPatch?>(null) { acc, patch -> if (acc == null) patch else acc.merge(patch) }

            if (combinedPatch == null) {
                null
            } else {
                ImmichRequestBuilder.buildBulkMetadataRequest(selection, combinedPatch)
            }
        }

        val transport = remember { ApiKeyGatedImmichTransport(DryRunImmichTransport()) }
        val gateStatus = transport.gateStatus(apiKey = null)

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text("Immich Upload Prep (Architecture Foundation)")
            Text("Assets loaded: ${state.assets.size}")
            Text("Selected: ${state.selectedAssetIds.size}")
            Text("Staged edits: ${state.stagedEditsByAssetId.size}")
            Text("Bulk metadata request ready: ${stagedBulkRequest != null}")
            Text("Transport gate: $gateStatus")

            Button(
                modifier = Modifier.padding(top = 16.dp),
                onClick = {
                    store.dispatch(
                        UploadPrepAction.StageEditForSelected(
                            AssetEditPatch(
                                description = FieldPatch.Set("Staged from architecture layer"),
                                isFavorite = FieldPatch.Set(true)
                            )
                        )
                    )
                }
            ) {
                Text("Stage favorite + description")
            }

            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = { store.dispatch(UploadPrepAction.ToggleSelection(secondId)) }
            ) {
                Text("Toggle second asset in selection")
            }
        }
    }
}
