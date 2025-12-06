package com.example.helpinghand.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun HelpingHandTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val materialColors =
        if (darkTheme) darkColorScheme() else lightColorScheme()

    val dashboardColors = if (darkTheme) DashboardColorsDark else DashboardColorsLight
    val shoppingColors  = if (darkTheme) ShoppingColorsDark  else ShoppingColorsLight
    val appColors       = if (darkTheme) AppColorsDark       else AppColorsLight

    CompositionLocalProvider(
        LocalDashboardColors provides dashboardColors,
        LocalShoppingColors  provides shoppingColors,
        LocalAppColors       provides appColors
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = Typography,
            content = content
        )
    }
}



