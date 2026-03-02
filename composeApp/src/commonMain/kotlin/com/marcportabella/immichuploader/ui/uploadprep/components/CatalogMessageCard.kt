package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import immichuploader.composeapp.generated.resources.Res
import immichuploader.composeapp.generated.resources.catalog_message_dismiss
import immichuploader.composeapp.generated.resources.catalog_message_dismiss_ca

@Composable
internal fun CatalogMessageCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(message, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onDismiss) {
                Text(
                    i18nString(
                        english = Res.string.catalog_message_dismiss,
                        catalan = Res.string.catalog_message_dismiss_ca
                    )
                )
            }
        }
    }
}
