package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
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
import immichuploader.composeapp.generated.resources.Res
import immichuploader.composeapp.generated.resources.batch_feedback_dismiss
import immichuploader.composeapp.generated.resources.batch_feedback_dismiss_ca
import immichuploader.composeapp.generated.resources.feedback_level_error
import immichuploader.composeapp.generated.resources.feedback_level_error_ca
import immichuploader.composeapp.generated.resources.feedback_level_success
import immichuploader.composeapp.generated.resources.feedback_level_success_ca
import immichuploader.composeapp.generated.resources.feedback_level_warning
import immichuploader.composeapp.generated.resources.feedback_level_warning_ca

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
    val levelLabel = when (feedback.level) {
        BatchFeedbackLevel.Error -> i18nString(
            english = Res.string.feedback_level_error,
            catalan = Res.string.feedback_level_error_ca
        )

        BatchFeedbackLevel.Warning -> i18nString(
            english = Res.string.feedback_level_warning,
            catalan = Res.string.feedback_level_warning_ca
        )

        BatchFeedbackLevel.Success -> i18nString(
            english = Res.string.feedback_level_success,
            catalan = Res.string.feedback_level_success_ca
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$levelLabel: ${feedback.message}",
                    color = contentColor,
                    fontWeight = FontWeight.Medium
                )
            }
            Button(onClick = onDismiss) {
                Text(
                    i18nString(
                        english = Res.string.batch_feedback_dismiss,
                        catalan = Res.string.batch_feedback_dismiss_ca
                    )
                )
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
