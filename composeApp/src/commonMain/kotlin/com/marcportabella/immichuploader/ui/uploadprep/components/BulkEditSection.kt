package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.BulkEditDraft
import com.marcportabella.immichuploader.domain.UploadCatalogEntry
import immichuploader.composeapp.generated.resources.Res
import immichuploader.composeapp.generated.resources.bulk_album_title
import immichuploader.composeapp.generated.resources.bulk_album_title_ca
import immichuploader.composeapp.generated.resources.bulk_batch_size
import immichuploader.composeapp.generated.resources.bulk_batch_size_ca
import immichuploader.composeapp.generated.resources.bulk_button_apply_edits
import immichuploader.composeapp.generated.resources.bulk_button_apply_edits_ca
import immichuploader.composeapp.generated.resources.bulk_button_clear_staged
import immichuploader.composeapp.generated.resources.bulk_button_clear_staged_ca
import immichuploader.composeapp.generated.resources.bulk_button_create_select_album
import immichuploader.composeapp.generated.resources.bulk_button_create_select_album_ca
import immichuploader.composeapp.generated.resources.bulk_button_create_select_tag
import immichuploader.composeapp.generated.resources.bulk_button_create_select_tag_ca
import immichuploader.composeapp.generated.resources.bulk_button_reset_draft
import immichuploader.composeapp.generated.resources.bulk_button_reset_draft_ca
import immichuploader.composeapp.generated.resources.bulk_description
import immichuploader.composeapp.generated.resources.bulk_description_ca
import immichuploader.composeapp.generated.resources.bulk_favorite_button_false
import immichuploader.composeapp.generated.resources.bulk_favorite_button_false_ca
import immichuploader.composeapp.generated.resources.bulk_favorite_button_true
import immichuploader.composeapp.generated.resources.bulk_favorite_button_true_ca
import immichuploader.composeapp.generated.resources.bulk_label_add_tag_ids
import immichuploader.composeapp.generated.resources.bulk_label_add_tag_ids_ca
import immichuploader.composeapp.generated.resources.bulk_label_album_id_manual
import immichuploader.composeapp.generated.resources.bulk_label_album_id_manual_ca
import immichuploader.composeapp.generated.resources.bulk_label_datetime_original
import immichuploader.composeapp.generated.resources.bulk_label_datetime_original_ca
import immichuploader.composeapp.generated.resources.bulk_label_description
import immichuploader.composeapp.generated.resources.bulk_label_description_ca
import immichuploader.composeapp.generated.resources.bulk_label_new_session_album
import immichuploader.composeapp.generated.resources.bulk_label_new_session_album_ca
import immichuploader.composeapp.generated.resources.bulk_label_new_session_tag
import immichuploader.composeapp.generated.resources.bulk_label_new_session_tag_ca
import immichuploader.composeapp.generated.resources.bulk_label_remove_tag_ids
import immichuploader.composeapp.generated.resources.bulk_label_remove_tag_ids_ca
import immichuploader.composeapp.generated.resources.bulk_label_timezone
import immichuploader.composeapp.generated.resources.bulk_label_timezone_ca
import immichuploader.composeapp.generated.resources.bulk_step_title
import immichuploader.composeapp.generated.resources.bulk_step_title_ca
import immichuploader.composeapp.generated.resources.bulk_tags_to_add_title
import immichuploader.composeapp.generated.resources.bulk_tags_to_add_title_ca
import immichuploader.composeapp.generated.resources.bulk_toggle_hide_advanced_fields
import immichuploader.composeapp.generated.resources.bulk_toggle_hide_advanced_fields_ca
import immichuploader.composeapp.generated.resources.bulk_toggle_show_advanced_fields
import immichuploader.composeapp.generated.resources.bulk_toggle_show_advanced_fields_ca

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulkEditSection(
    draft: BulkEditDraft,
    batchAssetCount: Int,
    applyEnabled: Boolean,
    preflightMessage: String?,
    availableAlbums: List<UploadCatalogEntry>,
    availableTags: List<UploadCatalogEntry>,
    selectedTagIds: Set<String>,
    onDraftChange: (BulkEditDraft) -> Unit,
    onCreateSessionAlbum: (String) -> Unit,
    onCreateSessionTag: (String) -> Unit,
    onApply: () -> Unit,
    onClearDraft: () -> Unit,
    onClearBatchStaged: () -> Unit
) {
    var newAlbumName by rememberSaveable { mutableStateOf("") }
    var newTagName by rememberSaveable { mutableStateOf("") }
    var showAdvancedFields by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                i18nString(
                    english = Res.string.bulk_step_title,
                    catalan = Res.string.bulk_step_title_ca
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                i18nString(
                    english = Res.string.bulk_batch_size,
                    catalan = Res.string.bulk_batch_size_ca,
                    batchAssetCount
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                i18nString(
                    english = Res.string.bulk_description,
                    catalan = Res.string.bulk_description_ca
                ),
                style = MaterialTheme.typography.bodySmall
            )

            ToggleFieldRow(
                enabled = draft.includeDescription,
                onEnabledChange = { onDraftChange(draft.copy(includeDescription = it)) }
            ) {
                OutlinedTextField(
                    value = draft.description,
                    onValueChange = { onDraftChange(draft.copy(description = it)) },
                    label = {
                        Text(
                            i18nString(
                                english = Res.string.bulk_label_description,
                                catalan = Res.string.bulk_label_description_ca
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            ToggleFieldRow(
                enabled = draft.includeDateTimeOriginal,
                onEnabledChange = { onDraftChange(draft.copy(includeDateTimeOriginal = it)) }
            ) {
                OutlinedTextField(
                    value = draft.dateTimeOriginal,
                    onValueChange = { onDraftChange(draft.copy(dateTimeOriginal = it)) },
                    label = {
                        Text(
                            i18nString(
                                english = Res.string.bulk_label_datetime_original,
                                catalan = Res.string.bulk_label_datetime_original_ca
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            ToggleFieldRow(
                enabled = draft.includeTimeZone,
                onEnabledChange = { onDraftChange(draft.copy(includeTimeZone = it)) }
            ) {
                TimeZoneDropdownField(
                    value = draft.timeZone,
                    onValueChange = { onDraftChange(draft.copy(timeZone = it)) },
                    label = i18nString(
                        english = Res.string.bulk_label_timezone,
                        catalan = Res.string.bulk_label_timezone_ca
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = draft.includeFavorite,
                    onCheckedChange = { onDraftChange(draft.copy(includeFavorite = it)) }
                )
                Button(onClick = { onDraftChange(draft.copy(isFavorite = !draft.isFavorite)) }) {
                    Text(
                        if (draft.isFavorite) {
                            i18nString(
                                english = Res.string.bulk_favorite_button_true,
                                catalan = Res.string.bulk_favorite_button_true_ca
                            )
                        } else {
                            i18nString(
                                english = Res.string.bulk_favorite_button_false,
                                catalan = Res.string.bulk_favorite_button_false_ca
                            )
                        }
                    )
                }
            }

            SelectableChipGroup(
                title = i18nString(
                    english = Res.string.bulk_album_title,
                    catalan = Res.string.bulk_album_title_ca
                ),
                options = availableAlbums.map { ChipOption(id = it.id, label = it.name) },
                selectedIds = if (draft.includeAlbumId && draft.albumId.isNotBlank()) setOf(draft.albumId) else emptySet(),
                onToggle = { albumId ->
                    val selected = draft.includeAlbumId && draft.albumId == albumId
                    onDraftChange(
                        if (selected) {
                            draft.copy(includeAlbumId = false, albumId = "")
                        } else {
                            draft.copy(includeAlbumId = true, albumId = albumId)
                        }
                    )
                }
            )

            OutlinedTextField(
                value = newAlbumName,
                onValueChange = { newAlbumName = it },
                label = {
                    Text(
                        i18nString(
                            english = Res.string.bulk_label_new_session_album,
                            catalan = Res.string.bulk_label_new_session_album_ca
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val name = newAlbumName.trim()
                    if (name.isNotEmpty()) {
                        onCreateSessionAlbum(name)
                        newAlbumName = ""
                    }
                },
                enabled = newAlbumName.isNotBlank()
            ) {
                Text(
                    i18nString(
                        english = Res.string.bulk_button_create_select_album,
                        catalan = Res.string.bulk_button_create_select_album_ca
                    )
                )
            }

            SelectableChipGroup(
                title = i18nString(
                    english = Res.string.bulk_tags_to_add_title,
                    catalan = Res.string.bulk_tags_to_add_title_ca
                ),
                options = availableTags.map { ChipOption(id = it.id, label = it.name) },
                selectedIds = selectedTagIds,
                onToggle = { tagId ->
                    val selected = tagId in selectedTagIds
                    val next = if (selected) selectedTagIds - tagId else selectedTagIds + tagId
                    onDraftChange(draft.copy(addTagIds = next.sorted().joinToString(",")))
                }
            )

            OutlinedTextField(
                value = newTagName,
                onValueChange = { newTagName = it },
                label = {
                    Text(
                        i18nString(
                            english = Res.string.bulk_label_new_session_tag,
                            catalan = Res.string.bulk_label_new_session_tag_ca
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val name = newTagName.trim()
                    if (name.isNotEmpty()) {
                        onCreateSessionTag(name)
                        newTagName = ""
                    }
                },
                enabled = newTagName.isNotBlank()
            ) {
                Text(
                    i18nString(
                        english = Res.string.bulk_button_create_select_tag,
                        catalan = Res.string.bulk_button_create_select_tag_ca
                    )
                )
            }

            TextButton(onClick = { showAdvancedFields = !showAdvancedFields }) {
                Text(
                    if (showAdvancedFields) {
                        i18nString(
                            english = Res.string.bulk_toggle_hide_advanced_fields,
                            catalan = Res.string.bulk_toggle_hide_advanced_fields_ca
                        )
                    } else {
                        i18nString(
                            english = Res.string.bulk_toggle_show_advanced_fields,
                            catalan = Res.string.bulk_toggle_show_advanced_fields_ca
                        )
                    }
                )
            }

            if (showAdvancedFields) {
                ToggleFieldRow(
                    enabled = draft.includeAlbumId,
                    onEnabledChange = { onDraftChange(draft.copy(includeAlbumId = it)) }
                ) {
                    OutlinedTextField(
                        value = draft.albumId,
                        onValueChange = { onDraftChange(draft.copy(albumId = it)) },
                        label = {
                            Text(
                                i18nString(
                                    english = Res.string.bulk_label_album_id_manual,
                                    catalan = Res.string.bulk_label_album_id_manual_ca
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = draft.addTagIds,
                    onValueChange = { onDraftChange(draft.copy(addTagIds = it)) },
                    label = {
                        Text(
                            i18nString(
                                english = Res.string.bulk_label_add_tag_ids,
                                catalan = Res.string.bulk_label_add_tag_ids_ca
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = draft.removeTagIds,
                    onValueChange = { onDraftChange(draft.copy(removeTagIds = it)) },
                    label = {
                        Text(
                            i18nString(
                                english = Res.string.bulk_label_remove_tag_ids,
                                catalan = Res.string.bulk_label_remove_tag_ids_ca
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            preflightMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onApply, enabled = applyEnabled) {
                    Text(
                        i18nString(
                            english = Res.string.bulk_button_apply_edits,
                            catalan = Res.string.bulk_button_apply_edits_ca
                        )
                    )
                }
                Button(onClick = onClearDraft) {
                    Text(
                        i18nString(
                            english = Res.string.bulk_button_reset_draft,
                            catalan = Res.string.bulk_button_reset_draft_ca
                        )
                    )
                }
                Button(onClick = onClearBatchStaged, enabled = batchAssetCount > 0) {
                    Text(
                        i18nString(
                            english = Res.string.bulk_button_clear_staged,
                            catalan = Res.string.bulk_button_clear_staged_ca
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleFieldRow(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = enabled,
            onCheckedChange = onEnabledChange
        )
        content()
    }
}

@Preview
@Composable
private fun BulkEditSectionPreview(
    @PreviewParameter(BulkEditDraftPreviewProvider::class) draft: BulkEditDraft
) {
    MaterialTheme {
        BulkEditSection(
            draft = draft,
            batchAssetCount = 3,
            applyEnabled = true,
            preflightMessage = PREVIEW_PREFLIGHT_MESSAGE,
            availableAlbums = previewCatalogAlbums(),
            availableTags = previewCatalogTags(),
            selectedTagIds = setOf("tag-1", "tag-3"),
            onDraftChange = {},
            onCreateSessionAlbum = {},
            onCreateSessionTag = {},
            onApply = {},
            onClearDraft = {},
            onClearBatchStaged = {}
        )
    }
}
