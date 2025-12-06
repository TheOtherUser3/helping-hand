package com.example.helpinghand.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

data class DashboardColorScheme(
    val Dashboard: Color,
    val AppBar: Color,
    val Time: Color,
    val Headline: Color,
    val Icon: Color,
    val Label: Color,
    val CardBackground: Color,
    val GestureBar: Color,
    val Handle: Color
)

data class ShoppingColorScheme(
    val Background: Color,
    val Primary: Color,
    val OnBackground: Color,
    val OnSurfaceVariant: Color,
    val Surface: Color,
    val SurfaceVariant: Color,
    val GestureBar: Color
)

data class AppColorScheme(
    val Background: Color,
    val Primary: Color,
    val OnBackground: Color,
    val OnSurfaceVariant: Color,
    val Surface: Color,
    val SurfaceVariant: Color,
    val GestureBar: Color
)

// ----- light palettes (your current values) -----
val DashboardColorsLight = DashboardColorScheme(
    Dashboard     = Color(0xFFF7F2FA),
    AppBar        = Color(0xFFEADDFF),
    Time          = Color(0xFF1D1B20),
    Headline      = Color(0xFF1D1B20),
    Icon          = Color(0xFF49454F),
    Label         = Color(0xFF6750A4),
    CardBackground= Color(0xFFFFFFFF),
    GestureBar    = Color(0xFFF3EDF7),
    Handle        = Color(0xFF1D1B20)
)

val ShoppingColorsLight = ShoppingColorScheme(
    Background      = Color(0xFFEADDFF),
    Primary         = Color(0xFF6750A4),
    OnBackground    = Color(0xFF1D1B20),
    OnSurfaceVariant= Color(0xFF49454F),
    Surface         = Color(0xFFFFFFFF),
    SurfaceVariant  = Color(0xFFFEF7FF),
    GestureBar      = Color(0xFFF3EDF7)
)

val AppColorsLight = AppColorScheme(
    Background      = Color(0xFFEADDFF),
    Primary         = Color(0xFF6750A4),
    OnBackground    = Color(0xFF1D1B20),
    OnSurfaceVariant= Color(0xFF49454F),
    Surface         = Color(0xFFFFFFFF),
    SurfaceVariant  = Color(0xFFFEF7FF),
    GestureBar      = Color(0xFFF3EDF7)
)

// ----- rough dark versions (tweak to taste in Figma) -----
val DashboardColorsDark = DashboardColorScheme(
    Dashboard      = Color(0xFF18141C),
    AppBar         = Color(0xFF241C2E),
    Time           = Color(0xFFE8E1EF),
    Headline       = Color(0xFFE8E1EF),
    Icon           = Color(0xFFCAC4D0),
    Label          = Color(0xFFD4C4FF),
    CardBackground = Color(0xFF221A2A),
    GestureBar     = Color(0xFF1A1520),
    Handle         = Color(0xFFE8E1EF)
)

val ShoppingColorsDark = ShoppingColorScheme(
    Background       = Color(0xFF18141C),
    Primary          = Color(0xFFD4C4FF),
    OnBackground     = Color(0xFFE8E1EF),
    OnSurfaceVariant = Color(0xFFCAC4D0),
    Surface          = Color(0xFF221A2A),
    SurfaceVariant   = Color(0xFF2E233A),
    GestureBar       = Color(0xFF1A1520)
)

val AppColorsDark = AppColorScheme(
    Background       = Color(0xFF18141C),
    Primary          = Color(0xFFD4C4FF),
    OnBackground     = Color(0xFFE8E1EF),
    OnSurfaceVariant = Color(0xFFCAC4D0),
    Surface          = Color(0xFF221A2A),
    SurfaceVariant   = Color(0xFF2E233A),
    GestureBar       = Color(0xFF1A1520)
)



val LocalDashboardColors = staticCompositionLocalOf { DashboardColorsLight }
val LocalShoppingColors = staticCompositionLocalOf { ShoppingColorsLight }
val LocalAppColors      = staticCompositionLocalOf { AppColorsLight }

object DashboardColors {
    val Dashboard @Composable get() = LocalDashboardColors.current.Dashboard
    val AppBar    @Composable get() = LocalDashboardColors.current.AppBar
    val Time      @Composable get() = LocalDashboardColors.current.Time
    val Headline  @Composable get() = LocalDashboardColors.current.Headline
    val Icon      @Composable get() = LocalDashboardColors.current.Icon
    val Label     @Composable get() = LocalDashboardColors.current.Label
    val CardBackground @Composable get() = LocalDashboardColors.current.CardBackground
    val GestureBar @Composable get() = LocalDashboardColors.current.GestureBar
    val Handle    @Composable get() = LocalDashboardColors.current.Handle
}

object ShoppingColors {
    val Background      @Composable get() = LocalShoppingColors.current.Background
    val Primary         @Composable get() = LocalShoppingColors.current.Primary
    val OnBackground    @Composable get() = LocalShoppingColors.current.OnBackground
    val OnSurfaceVariant@Composable get() = LocalShoppingColors.current.OnSurfaceVariant
    val Surface         @Composable get() = LocalShoppingColors.current.Surface
    val SurfaceVariant  @Composable get() = LocalShoppingColors.current.SurfaceVariant
    val GestureBar      @Composable get() = LocalShoppingColors.current.GestureBar
}

object AppColors {
    val Background      @Composable get() = LocalAppColors.current.Background
    val Primary         @Composable get() = LocalAppColors.current.Primary
    val OnBackground    @Composable get() = LocalAppColors.current.OnBackground
    val OnSurfaceVariant@Composable get() = LocalAppColors.current.OnSurfaceVariant
    val Surface         @Composable get() = LocalAppColors.current.Surface
    val SurfaceVariant  @Composable get() = LocalAppColors.current.SurfaceVariant
    val GestureBar      @Composable get() = LocalAppColors.current.GestureBar
}
