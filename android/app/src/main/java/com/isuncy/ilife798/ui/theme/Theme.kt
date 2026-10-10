package com.isuncy.ilife798.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** 全局渐变背景：让底部玻璃胶囊导航有内容可模糊/折射 */
val Ilife798Background: Brush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFE7F0FC),
        Color(0xFFF7F9FC),
        Color(0xFFF0ECFA)
    )
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF43A047),
    tertiary = Color(0xFFFB8C00),
    // 背景/表面透明，露出全局渐变；卡片用白色系 surfaceContainer*
    background = Color.Transparent,
    surface = Color.Transparent,
    surfaceVariant = Color(0xFFECF1F8),
    onSurfaceVariant = Color(0xFF5B6472),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFCFDFF),
    surfaceContainerHigh = Color(0xFFF8FAFE),
    surfaceContainerHighest = Color(0xFFF2F5FB),
    outlineVariant = Color(0xFFD9E2EC)
)

@Composable
fun Ilife798Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
