package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeZoneDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val zoneIds = remember { TimeZone.availableZoneIds.sorted() }
    val normalizedQuery = value.trim()
    val filteredZoneIds = remember(normalizedQuery, zoneIds) {
        if (normalizedQuery.isBlank()) zoneIds
        else zoneIds.filter { it.contains(normalizedQuery, ignoreCase = true) }
    }
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                if (enabled) expanded = true
            },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = modifier.menuAnchor(
                type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                enabled = enabled
            ),
            enabled = enabled
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            filteredZoneIds.forEach { zoneId ->
                DropdownMenuItem(
                    text = { Text(zoneId) },
                    onClick = {
                        onValueChange(zoneId)
                        expanded = false
                    }
                )
            }
        }
    }
}
