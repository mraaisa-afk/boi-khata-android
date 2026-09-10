package com.boikhata.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val LocalLiteUi = staticCompositionLocalOf { false }

// D71/D73 — Full Lal Khata color scheme (26 M3 tokens; D73 adds 4 surfaceContainer tokens)
// D72 — M3 role mapping documented in DECISIONS.md
private val LalKhataColors = lightColorScheme(
    // ── Primary: Maroon #800000 ────────────────────────────────────────────────
    primary            = Color(0xFF800000),
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFFFD7D7),
    onPrimaryContainer = Color(0xFF5C0000),

    // ── Secondary: Money Green #1B6E3F  (6.03:1 WCAG AA) ────────────────────
    // secondaryContainer fixes the M3-default lavender nav-bar active chip
    secondary            = Color(0xFF1B6E3F),
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFB8F0D4),
    onSecondaryContainer = Color(0xFF003920),

    // ── Tertiary: Amber #9E5C00  (5.06:1 WCAG AA) ───────────────────────
    tertiary            = Color(0xFF9E5C00),
    onTertiary          = Color.White,
    tertiaryContainer   = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF321200),

    // ── Surface / Background: Warm Ivory #FDFAF6 ─────────────────────────────
    // surfaceVariant fixes the M3-default grey elevated card surface
    background       = Color(0xFFFDFAF6),
    onBackground     = Color(0xFF1A1110),
    surface          = Color(0xFFFDFAF6),
    onSurface        = Color(0xFF1A1110),
    surfaceVariant   = Color(0xFFF2EDE7),
    onSurfaceVariant = Color(0xFF4A3F3B),
    outline          = Color(0xFF8D7B6E),
    outlineVariant   = Color(0xFFD9CFC8),

    // ── Surface Container tokens (M3 1.2+) — D73 fix ──────────────────────
    // Card() uses surfaceContainer (not surfaceVariant) as its default background.
    // Without these tokens, M3 derives them from primary (maroon #800000) via its
    // tonal algorithm → lavender/purple tint on all Card surfaces.
    surfaceContainer        = Color(0xFFF2EDE7),   // warm ivory — same as surfaceVariant
    surfaceContainerLow     = Color(0xFFF7F3EE),   // slightly lighter (bottom sheets)
    surfaceContainerHigh    = Color(0xFFEDE7E1),   // slightly darker (nav bar bg)
    surfaceContainerHighest = Color(0xFFE8E1DB),   // darkest (chips, selected state)

    // ── Error ───────────────────────────────────────────────────────────────────
    error            = Color(0xFFB3261E),
    onError          = Color.White,
    errorContainer   = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

// D71 §2 — Bengali font applied to all 15 M3 text roles
// headlineSmall bumped to 28sp (from M3 default 24sp) for ledger readability
private val LalKhataTypography = Typography(
    displayLarge   = TextStyle(fontFamily = BengaliFontFamily, fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium  = TextStyle(fontFamily = BengaliFontFamily, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall   = TextStyle(fontFamily = BengaliFontFamily, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontFamily = BengaliFontFamily, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = BengaliFontFamily, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = BengaliFontFamily, fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge     = TextStyle(fontFamily = BengaliFontFamily, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal),
    titleMedium    = TextStyle(fontFamily = BengaliFontFamily, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.15.sp),
    titleSmall     = TextStyle(fontFamily = BengaliFontFamily, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    bodyLarge      = TextStyle(fontFamily = BengaliFontFamily, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium     = TextStyle(fontFamily = BengaliFontFamily, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall      = TextStyle(fontFamily = BengaliFontFamily, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge     = TextStyle(fontFamily = BengaliFontFamily, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontFamily = BengaliFontFamily, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontFamily = BengaliFontFamily, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

@Composable
fun BoiKhataTheme(
    liteMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    val typography = if (liteMode) LalKhataTypography.copy(
        bodyLarge = LalKhataTypography.bodyLarge.copy(fontSize = 19.2.sp),
        bodyMedium = LalKhataTypography.bodyMedium.copy(fontSize = 16.8.sp),
        titleMedium = LalKhataTypography.titleMedium.copy(fontSize = 19.2.sp),
    ) else LalKhataTypography
    CompositionLocalProvider(LocalLiteUi provides liteMode) {
        MaterialTheme(colorScheme = LalKhataColors, typography = typography, content = content)
    }
}
