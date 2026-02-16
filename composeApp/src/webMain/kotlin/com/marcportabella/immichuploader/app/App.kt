package com.marcportabella.immichuploader.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.ui.UploadPrepScreen

@Composable
fun App() {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF0B57D0),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDCE6FF),
        onPrimaryContainer = Color(0xFF001A41),
        secondary = Color(0xFF2A4B6D),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD4E4FF),
        onSecondaryContainer = Color(0xFF0E1D31),
        tertiary = Color(0xFF005A5A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFB8F1EE),
        onTertiaryContainer = Color(0xFF00201F),
        background = Color(0xFFF4F7FB),
        onBackground = Color(0xFF101828),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF111827),
        surfaceVariant = Color(0xFFE5EAF2),
        onSurfaceVariant = Color(0xFF344054),
        outline = Color(0xFF8A94A6),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002)
    )

    MaterialTheme(colorScheme = colorScheme, typography = Typography()) {
        val store = remember { UploadPrepStore() }
        UploadPrepScreen(store)
    }
}
