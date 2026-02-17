package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class ChipOption(
    val id: String,
    val label: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectableChipGroup(
    title: String,
    options: List<ChipOption>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    if (options.isEmpty()) return

    Text(text = title, style = MaterialTheme.typography.labelMedium)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option.id in selectedIds,
                onClick = { onToggle(option.id) },
                label = { Text(option.label) }
            )
        }
    }
}

@Preview
@Composable
private fun SelectableChipGroupPreview() {
    MaterialTheme {
        SelectableChipGroup(
            title = "Albums",
            options = listOf(
                ChipOption(id = "a1", label = "Tokyo 2016"),
                ChipOption(id = "a2", label = "Kyoto 2016"),
                ChipOption(id = "a3", label = "Nara 2016")
            ),
            selectedIds = setOf("a2"),
            onToggle = {}
        )
    }
}
