package com.marcportabella.immichuploader.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.marcportabella.immichuploader.domain.UploadPrepState
import com.marcportabella.immichuploader.domain.UploadPrepStore
import com.marcportabella.immichuploader.ui.uploadprep.LocalUiLanguage
import com.marcportabella.immichuploader.ui.uploadprep.UiLanguage
import com.marcportabella.immichuploader.ui.uploadprep.UploadPrepScreen

@Composable
fun App() {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF0E3A66),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDCEFFF),
        onPrimaryContainer = Color(0xFF001B33),
        secondary = Color(0xFF126E82),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFD1F2F9),
        onSecondaryContainer = Color(0xFF002A34),
        tertiary = Color(0xFF9A5D00),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFE1BA),
        onTertiaryContainer = Color(0xFF2F1600),
        background = Color(0xFFF2F7FA),
        onBackground = Color(0xFF0D2233),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF10283A),
        surfaceVariant = Color(0xFFE3EDF5),
        onSurfaceVariant = Color(0xFF385066),
        outline = Color(0xFF8398AB),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDBD7),
        onErrorContainer = Color(0xFF410E0B)
    )

    val typography = Typography(
        headlineSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp,
            lineHeight = 36.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 21.sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    )

    MaterialTheme(colorScheme = colorScheme, typography = typography) {
        val store = remember {
            UploadPrepStore(
                UploadPrepState(apiKey = BOOTSTRAP_IMMICH_API_KEY)
            )
        }
        var uiLanguage by rememberSaveable { mutableStateOf(UiLanguage.Catalan) }
        CompositionLocalProvider(LocalUiLanguage provides uiLanguage) {
            UploadPrepScreen(
                store = store,
                uiLanguage = uiLanguage,
                onUiLanguageChange = { uiLanguage = it }
            )
        }
    }
}
