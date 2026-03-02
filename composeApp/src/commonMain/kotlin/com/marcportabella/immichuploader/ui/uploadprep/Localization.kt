package com.marcportabella.immichuploader.ui.uploadprep

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class UiLanguage {
    Catalan,
    English
}

val LocalUiLanguage = compositionLocalOf { UiLanguage.Catalan }

@Composable
internal fun i18nString(
    english: StringResource,
    catalan: StringResource,
    vararg formatArgs: Any
): String {
    val selected = LocalUiLanguage.current
    return when (selected) {
        UiLanguage.Catalan -> stringResource(catalan, *formatArgs)
        UiLanguage.English -> stringResource(english, *formatArgs)
    }
}
