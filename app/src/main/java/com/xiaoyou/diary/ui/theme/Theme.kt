package com.xiaoyou.diary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.xiaoyou.diary.model.BehaviorType

/**
 * 全局色板 token：与 design/design.html 的 CSS 变量逐一对齐。
 * 深色优先，AppColors 内所有颜色随主题切换。
 */
data class AppColors(
    // 层级表面
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val elevated: Color,
    val tabActive: Color,
    // 文本
    val ink: Color,
    val ink2: Color,
    val ink3: Color,
    // 分隔线
    val rule: Color,
    val rule2: Color,
    // 强调色（蓝=运行/确认 黄=实时/今日 紫=状态 红=危险 绿=正常/达标）
    val blue: Color,
    val yellow: Color,
    val purple: Color,
    val red: Color,
    val green: Color,
    val orange: Color,
    val cyan: Color,
    val pink: Color,
    // 强调色低透明底
    val blueSoft: Color,
    val yellowSoft: Color,
    val purpleSoft: Color,
    val redSoft: Color,
    val greenSoft: Color,
    val orangeSoft: Color,
    // 遮罩
    val scrim: Color,
    // 行为分类色（随主题）
    private val behaviorWalk: Color,
    private val behaviorJog: Color,
    private val behaviorRun: Color,
    private val behaviorSit: Color,
    private val behaviorStand: Color,
    private val behaviorTilt: Color,
    private val behaviorLie: Color,
    private val behaviorSide: Color,
    private val behaviorWalkSoft: Color,
    private val behaviorJogSoft: Color,
    private val behaviorRunSoft: Color,
    private val behaviorSitSoft: Color,
    private val behaviorStandSoft: Color,
    private val behaviorTiltSoft: Color,
    private val behaviorLieSoft: Color,
    private val behaviorSideSoft: Color,
    val isDark: Boolean,
) {
    fun behavior(type: BehaviorType): Color = when (type) {
        BehaviorType.WALK -> behaviorWalk
        BehaviorType.JOG -> behaviorJog
        BehaviorType.RUN -> behaviorRun
        BehaviorType.SIT -> behaviorSit
        BehaviorType.STAND -> behaviorStand
        BehaviorType.TILT -> behaviorTilt
        BehaviorType.LIE -> behaviorLie
        BehaviorType.SIDE_LIE -> behaviorSide
    }

    fun behaviorSoft(type: BehaviorType): Color = when (type) {
        BehaviorType.WALK -> behaviorWalkSoft
        BehaviorType.JOG -> behaviorJogSoft
        BehaviorType.RUN -> behaviorRunSoft
        BehaviorType.SIT -> behaviorSitSoft
        BehaviorType.STAND -> behaviorStandSoft
        BehaviorType.TILT -> behaviorTiltSoft
        BehaviorType.LIE -> behaviorLieSoft
        BehaviorType.SIDE_LIE -> behaviorSideSoft
    }
}

val LocalAppColors = staticCompositionLocalOf { darkAppColors() }

fun darkAppColors() = AppColors(
    bg = DarkBg,
    surface = DarkSurface,
    surface2 = DarkSurface2,
    elevated = DarkElevated,
    tabActive = DarkTabActive,
    ink = DarkInk,
    ink2 = DarkInk2,
    ink3 = DarkInk3,
    rule = DarkRule,
    rule2 = DarkRule2,
    blue = DarkBlue,
    yellow = DarkYellow,
    purple = DarkPurple,
    red = DarkRed,
    green = DarkGreen,
    orange = DarkOrange,
    cyan = DarkCyan,
    pink = DarkPink,
    blueSoft = DarkBlueSoft,
    yellowSoft = DarkYellowSoft,
    purpleSoft = DarkPurpleSoft,
    redSoft = DarkRedSoft,
    greenSoft = DarkGreenSoft,
    orangeSoft = DarkOrangeSoft,
    scrim = DarkScrim,
    behaviorWalk = BehaviorWalk,
    behaviorJog = BehaviorJog,
    behaviorRun = BehaviorRun,
    behaviorSit = BehaviorSit,
    behaviorStand = BehaviorStand,
    behaviorTilt = BehaviorTilt,
    behaviorLie = BehaviorLie,
    behaviorSide = BehaviorSideLie,
    behaviorWalkSoft = DarkGreenSoft,
    behaviorJogSoft = DarkYellowSoft,
    behaviorRunSoft = DarkRedSoft,
    behaviorSitSoft = DarkBlueSoft,
    behaviorStandSoft = DarkPurpleSoft,
    behaviorTiltSoft = DarkOrangeSoft,
    behaviorLieSoft = Color(0x295AC8FA),
    behaviorSideSoft = Color(0x29FF375F),
    isDark = true,
)

fun lightAppColors() = AppColors(
    bg = LightBg,
    surface = LightSurface,
    surface2 = LightSurface2,
    elevated = LightElevated,
    tabActive = LightTabActive,
    ink = LightInk,
    ink2 = LightInk2,
    ink3 = LightInk3,
    rule = LightRule,
    rule2 = LightRule2,
    blue = LightBlue,
    yellow = LightYellow,
    purple = LightPurple,
    red = LightRed,
    green = LightGreen,
    orange = LightOrange,
    cyan = LightCyan,
    pink = LightPink,
    blueSoft = LightBlueSoft,
    yellowSoft = LightYellowSoft,
    purpleSoft = LightPurpleSoft,
    redSoft = LightRedSoft,
    greenSoft = LightGreenSoft,
    orangeSoft = LightOrangeSoft,
    scrim = LightScrim,
    behaviorWalk = LightGreen,
    behaviorJog = LightYellow,
    behaviorRun = LightRed,
    behaviorSit = LightBlue,
    behaviorStand = LightPurple,
    behaviorTilt = LightOrange,
    behaviorLie = LightCyan,
    behaviorSide = LightPink,
    behaviorWalkSoft = LightGreenSoft,
    behaviorJogSoft = LightYellowSoft,
    behaviorRunSoft = LightRedSoft,
    behaviorSitSoft = LightBlueSoft,
    behaviorStandSoft = LightPurpleSoft,
    behaviorTiltSoft = LightOrangeSoft,
    behaviorLieSoft = Color(0x1F2BA7D6),
    behaviorSideSoft = Color(0x1FE0234A),
    isDark = false,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBlue,
    onPrimary = White,
    secondary = DarkPurple,
    tertiary = DarkYellow,
    error = DarkRed,
    background = DarkBg,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurface2,
    outline = DarkRule2,
)

private val LightColorScheme = lightColorScheme(
    primary = LightBlue,
    onPrimary = White,
    secondary = LightPurple,
    tertiary = LightYellow,
    error = LightRed,
    background = LightBg,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightSurface2,
    outline = LightRule2,
)

@Composable
fun XiaoYouTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) darkAppColors() else lightAppColors()

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
