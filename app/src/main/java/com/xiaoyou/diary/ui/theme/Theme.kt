package com.xiaoyou.diary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val primary: Color,
    val success: Color,
    val warn: Color,
    val danger: Color,
    val ink1: Color,
    val ink2: Color,
    val ink3: Color,
    val line: Color,
    val soft: Color,
    val card: Color,
    val bg: Color,
    val tabbarBg: Color,
    val tabInactive: Color,
    val settingRowPressed: Color,
    val diaryInputBg: Color,
    val diaryInputBorder: Color,
    val switchOffBg: Color,
    val chipBg: Color,
    val chipBorder: Color,
    val confirmBg: Color,
    val sheetBg: Color,
    val navIcon: Color,
    val dateChipBg: Color,
    val dateChipText: Color,
    val moodActiveBg: Color,
    val ghostBtnBg: Color,
    val ghostBtnBorder: Color,
    val ghostBtnText: Color,
    val cardLine: Color,
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        primary = Primary,
        success = Success,
        warn = Warn,
        danger = Danger,
        ink1 = Ink1,
        ink2 = Ink2,
        ink3 = Ink3,
        line = LineColor,
        soft = SoftBg,
        card = White,
        bg = SoftBg,
        tabbarBg = Color(0xFFFAFBFC),
        tabInactive = Ink3,
        settingRowPressed = Color(0x0F1677FF),
        diaryInputBg = White,
        diaryInputBorder = LineColor,
        switchOffBg = Color(0xFFDCE0E6),
        chipBg = Color(0xFFF5F7FA),
        chipBorder = LineColor,
        confirmBg = White,
        sheetBg = White,
        navIcon = Ink2,
        dateChipBg = SoftBg,
        dateChipText = Ink3,
        moodActiveBg = Color(0x1A1677FF),
        ghostBtnBg = White,
        ghostBtnBorder = Color(0xFFDCE0E6),
        ghostBtnText = Ink2,
        cardLine = LineColor,
    )
}

fun lightColors() = AppColors(
    primary = Primary,
    success = Success,
    warn = Warn,
    danger = Danger,
    ink1 = Ink1,
    ink2 = Ink2,
    ink3 = Ink3,
    line = LineColor,
    soft = SoftBg,
    card = White,
    bg = SoftBg,
    tabbarBg = Color(0xFFFAFBFC),
    tabInactive = Ink3,
    settingRowPressed = Color(0x0F1677FF),
    diaryInputBg = White,
    diaryInputBorder = LineColor,
    switchOffBg = Color(0xFFDCE0E6),
    chipBg = Color(0xFFF5F7FA),
    chipBorder = LineColor,
    confirmBg = White,
    sheetBg = White,
    navIcon = Ink2,
    dateChipBg = SoftBg,
    dateChipText = Ink3,
    moodActiveBg = Color(0x1A1677FF),
    ghostBtnBg = White,
    ghostBtnBorder = Color(0xFFDCE0E6),
    ghostBtnText = Ink2,
    cardLine = LineColor,
)

fun darkColors() = AppColors(
    primary = Primary,
    success = Success,
    warn = Warn,
    danger = Danger,
    ink1 = DarkText,
    ink2 = DarkTextSecondary,
    ink3 = DarkTextTertiary,
    line = DarkBorder,
    soft = DarkSoft,
    card = DarkCard,
    bg = DarkBg,
    tabbarBg = Color(0xE616181C),
    tabInactive = DarkTextTertiary,
    settingRowPressed = Color(0x1F1677FF),
    diaryInputBg = DarkSoft,
    diaryInputBorder = DarkBorder,
    switchOffBg = DarkBorder,
    chipBg = DarkSoft,
    chipBorder = DarkBorder,
    confirmBg = DarkCard,
    sheetBg = DarkCard,
    navIcon = DarkTextSecondary,
    dateChipBg = DarkSoft,
    dateChipText = DarkTextSecondary,
    moodActiveBg = Color(0x2E1677FF),
    ghostBtnBg = Color.Transparent,
    ghostBtnBorder = DarkBorder,
    ghostBtnText = DarkTextSecondary,
    cardLine = DarkBorder,
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Success,
    tertiary = Warn,
    background = SoftBg,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onBackground = Ink1,
    onSurface = Ink1,
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Success,
    tertiary = Warn,
    background = DarkBg,
    surface = DarkCard,
    onPrimary = White,
    onSecondary = White,
    onBackground = DarkText,
    onSurface = DarkText,
)

@Composable
fun XiaoYouTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) darkColors() else lightColors()

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            content = content
        )
    }
}
