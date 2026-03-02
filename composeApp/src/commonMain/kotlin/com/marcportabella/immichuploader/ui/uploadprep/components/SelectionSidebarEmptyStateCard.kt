package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import immichuploader.composeapp.generated.resources.Res
import immichuploader.composeapp.generated.resources.sidebar_empty_catalog_counts
import immichuploader.composeapp.generated.resources.sidebar_empty_catalog_counts_ca
import immichuploader.composeapp.generated.resources.sidebar_empty_description
import immichuploader.composeapp.generated.resources.sidebar_empty_description_ca
import immichuploader.composeapp.generated.resources.sidebar_empty_pick_media_first
import immichuploader.composeapp.generated.resources.sidebar_empty_pick_media_first_ca
import immichuploader.composeapp.generated.resources.sidebar_empty_title
import immichuploader.composeapp.generated.resources.sidebar_empty_title_ca

@Composable
internal fun SelectionSidebarEmptyStateCard(
    albumsCount: Int,
    tagsCount: Int,
    catalogMessage: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                i18nString(
                    english = Res.string.sidebar_empty_title,
                    catalan = Res.string.sidebar_empty_title_ca
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                i18nString(
                    english = Res.string.sidebar_empty_pick_media_first,
                    catalan = Res.string.sidebar_empty_pick_media_first_ca
                )
            )
            Text(
                i18nString(
                    english = Res.string.sidebar_empty_description,
                    catalan = Res.string.sidebar_empty_description_ca
                )
            )
            Text(
                i18nString(
                    english = Res.string.sidebar_empty_catalog_counts,
                    catalan = Res.string.sidebar_empty_catalog_counts_ca,
                    albumsCount,
                    tagsCount
                )
            )
            catalogMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
