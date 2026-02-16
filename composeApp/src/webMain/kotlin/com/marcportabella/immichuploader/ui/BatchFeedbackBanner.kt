package com.marcportabella.immichuploader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marcportabella.immichuploader.domain.BatchFeedback
import com.marcportabella.immichuploader.domain.BatchFeedbackLevel

@Composable
fun BatchFeedbackBanner(
    feedback: BatchFeedback,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = when (feedback.level) {
        BatchFeedbackLevel.Error -> colorScheme.errorContainer
        BatchFeedbackLevel.Warning -> colorScheme.secondaryContainer
        BatchFeedbackLevel.Success -> colorScheme.tertiaryContainer
    }
    val contentColor = when (feedback.level) {
        BatchFeedbackLevel.Error -> colorScheme.onErrorContainer
        BatchFeedbackLevel.Warning -> colorScheme.onSecondaryContainer
        BatchFeedbackLevel.Success -> colorScheme.onTertiaryContainer
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = feedback.message,
            color = contentColor
        )
        Button(onClick = onDismiss) {
            Text("Dismiss")
        }
    }
}
