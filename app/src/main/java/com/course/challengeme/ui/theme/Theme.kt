package com.course.challengeme.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ChallengeMeColorScheme = lightColorScheme(
    background = AppBackground,
    surface = AppBackground,
    onBackground = AppText,
    onSurface = AppText,
    primary = Maroon,
    onPrimary = AppBackground,
    secondary = ChallengeBgTan,
    tertiary = ChallengeBgMauve,
)

@Composable
fun ChallengeMeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChallengeMeColorScheme,
        typography = Typography,
        content = content
    )
}
