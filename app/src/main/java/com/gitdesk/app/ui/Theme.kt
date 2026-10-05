package com.gitdesk.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * 单色阶设计令牌：只有黑、白、灰，不引入任何无关彩色。
 * 唯一的语义色是 error（用于危险操作提示）。
 */

private val Ink = Color(0xFF111113)
private val Paper = Color(0xFFFFFFFF)

private val CardLight = Color(0xFFF5F5F7)
private val Card2Light = Color(0xFFEDEDF0)
private val HairLight = Color(0xFFE5E5EA)
private val Text1Light = Color(0xFF1D1D1F)
private val Text2Light = Color(0xFF6E6E73)
private val Text3Light = Color(0xFF8E8E93)

private val CardDark = Color(0xFF1C1C1E)
private val Card2Dark = Color(0xFF2C2C2E)
private val HairDark = Color(0xFF2C2C2E)
private val Text1Dark = Color(0xFFF5F5F7)
private val Text2Dark = Color(0xFFA1A1A6)
private val Text3Dark = Color(0xFF98989D)

data class GdColors(
    val text1: Color,
    val text2: Color,
    val text3: Color,
    val card: Color,
    val card2: Color,
    val hair: Color,
    val ink: Color,
    val paper: Color
)

val LocalGd = staticCompositionLocalOf {
    GdColors(
        text1 = Text1Light, text2 = Text2Light, text3 = Text3Light,
        card = CardLight, card2 = Card2Light, hair = HairLight,
        ink = Ink, paper = Paper
    )
}

object Gd {
    val c: GdColors
        @Composable get() = LocalGd.current
}

private val LightScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    secondary = Ink,
    onSecondary = Paper,
    tertiary = Ink,
    onTertiary = Paper,
    background = Paper,
    onBackground = Text1Light,
    surface = Paper,
    onSurface = Text1Light,
    surfaceVariant = CardLight,
    onSurfaceVariant = Text2Light,
    outline = HairLight,
    outlineVariant = HairLight,
    error = Color(0xFFD70015),
    onError = Paper
)

private val DarkScheme = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    secondary = Paper,
    onSecondary = Ink,
    tertiary = Paper,
    onTertiary = Ink,
    background = Color(0xFF000000),
    onBackground = Text1Dark,
    surface = Color(0xFF000000),
    onSurface = Text1Dark,
    surfaceVariant = CardDark,
    onSurfaceVariant = Text2Dark,
    outline = HairDark,
    outlineVariant = HairDark,
    error = Color(0xFFFF453A),
    onError = Ink
)

private val GdTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 30.sp,
        lineHeight = 36.sp, letterSpacing = (-0.6).sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold, fontSize = 22.sp,
        lineHeight = 28.sp, letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun GdTheme(theme: String, content: @Composable () -> Unit) {
    val dark = when (theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val gd = if (dark) {
        GdColors(
            text1 = Text1Dark, text2 = Text2Dark, text3 = Text3Dark,
            card = CardDark, card2 = Card2Dark, hair = HairDark,
            ink = Paper, paper = Color(0xFF000000)
        )
    } else {
        GdColors(
            text1 = Text1Light, text2 = Text2Light, text3 = Text3Light,
            card = CardLight, card2 = Card2Light, hair = HairLight,
            ink = Ink, paper = Paper
        )
    }

    CompositionLocalProvider(LocalGd provides gd) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = GdTypography,
            content = content
        )
    }
}
