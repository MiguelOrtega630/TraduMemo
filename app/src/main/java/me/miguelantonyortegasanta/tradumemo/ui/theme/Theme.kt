package me.miguelantonyortegasanta.tradumemo.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun TraduMemoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = PrimaryRed,
            onPrimary = White,
            background = BackgroundLight,
            onBackground = Black
        ),
        typography = Typography(),
        content = content
    )
}