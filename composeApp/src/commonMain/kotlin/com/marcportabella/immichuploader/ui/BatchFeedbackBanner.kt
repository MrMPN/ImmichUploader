package com.marcportabella.immichuploader.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${feedback.level}: ${feedback.message}",
                color = contentColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Preview
@Composable
private fun BatchFeedbackBannerPreview(
    @PreviewParameter(BatchFeedbackPreviewProvider::class) feedback: BatchFeedback
) {
    MaterialTheme {
        BatchFeedbackBanner(
            feedback = feedback,
            onDismiss = {}
        )
    }
}
