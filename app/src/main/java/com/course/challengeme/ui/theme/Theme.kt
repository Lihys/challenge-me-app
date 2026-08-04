package com.course.challengeme.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ChallengeMeColorScheme = lightColorScheme(
    background = AppBackground,
    surface = AppBackground,
    onBackground = AppText,
    onSurface = AppText,
    primary = ButtonDark,
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
