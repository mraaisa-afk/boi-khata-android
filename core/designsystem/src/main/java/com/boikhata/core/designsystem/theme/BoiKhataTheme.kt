package com.boikhata.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

val LocalLiteUi = staticCompositionLocalOf { false }
private val LalKhataTypography = Typography()
private val LalKhataColors = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF800000),
    background = Color(0xFFFDFAF6),
    surface = Color(0xFFFDFAF6),
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
