package com.xiaoyou.diary.ui.theme

import androidx.compose.ui.graphics.Color

/* ============================================================
   小幽日记 设计 Token —— 与 design/design.html 一一对应
   深色优先（Dark First），配套浅色主题
   ============================================================ */

// ---------- 深色主题（默认） ----------
val DarkBg = Color(0xFF0C0E11)
val DarkSurface = Color(0xFF161A21)
val DarkSurface2 = Color(0xFF1C212A)
val DarkElevated = Color(0xFF242B36)
val DarkTabActive = Color(0xFF2C333F)

val DarkInk = Color(0xFFF3F6FA)
val DarkInk2 = Color(0xFFA6AEBD)
val DarkInk3 = Color(0xFF6E7686)

val DarkRule = Color(0x13FFFFFF)   // rgba(255,255,255,.075)
val DarkRule2 = Color(0x21FFFFFF)  // rgba(255,255,255,.13)

val DarkBlue = Color(0xFF2F8AF5)
val DarkYellow = Color(0xFFF7C948)
val DarkPurple = Color(0xFFB46CF0)
val DarkRed = Color(0xFFFF5A5F)
val DarkGreen = Color(0xFF34C759)
val DarkOrange = Color(0xFFFF9F0A)
val DarkCyan = Color(0xFF5AC8FA)
val DarkPink = Color(0xFFFF375F)

val DarkBlueSoft = Color(0x292F8AF5)
val DarkYellowSoft = Color(0x24F7C948)
val DarkPurpleSoft = Color(0x2EB46CF0)
val DarkRedSoft = Color(0x29FF5A5F)
val DarkGreenSoft = Color(0x2934C759)
val DarkOrangeSoft = Color(0x29FF9F0A)

val DarkScrim = Color(0x73000000)

// ---------- 浅色主题 ----------
val LightBg = Color(0xFFF1F2F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFF6F8FB)
val LightElevated = Color(0xFFFFFFFF)
val LightTabActive = Color(0xFF15181F)

val LightInk = Color(0xFF12161D)
val LightInk2 = Color(0xFF4B5361)
val LightInk3 = Color(0xFF8B93A1)

val LightRule = Color(0x14111419)   // rgba(17,20,25,.08)
val LightRule2 = Color(0x24111419)  // rgba(17,20,25,.14)

val LightBlue = Color(0xFF0A6FE6)
val LightYellow = Color(0xFFD19200)
val LightPurple = Color(0xFF9B44DC)
val LightRed = Color(0xFFE5302B)
val LightGreen = Color(0xFF1F9D44)
val LightOrange = Color(0xFFE8860A)
val LightCyan = Color(0xFF2BA7D6)
val LightPink = Color(0xFFE0234A)

val LightBlueSoft = Color(0x1A0A6FE6)
val LightYellowSoft = Color(0x1FD19200)
val LightPurpleSoft = Color(0x1F9B44DC)
val LightRedSoft = Color(0x1AE5302B)
val LightGreenSoft = Color(0x1F1F9D44)
val LightOrangeSoft = Color(0x1FE8860A)

val LightScrim = Color(0x59000000)

// ---------- 8 类行为分类色（深色主题取值；主题内通过 AppColors.behavior() 取适配色） ----------
val BehaviorWalk = Color(0xFF34C759)     // 行走 · 绿
val BehaviorJog = Color(0xFFF7C948)      // 慢跑 · 黄
val BehaviorRun = Color(0xFFFF5A5F)      // 快跑 · 红
val BehaviorSit = Color(0xFF2F8AF5)      // 静坐 · 蓝
val BehaviorStand = Color(0xFFB46CF0)    // 站立 · 紫
val BehaviorTilt = Color(0xFFFF9F0A)     // 倾斜 · 橙
val BehaviorLie = Color(0xFF5AC8FA)      // 平躺 · 青
val BehaviorSideLie = Color(0xFFFF375F)  // 侧躺 · 粉

// ---------- 兼容别名（业务/旧代码引用） ----------
val Primary = DarkBlue
val Success = DarkGreen
val Warn = DarkOrange
val Danger = DarkRed
val White = Color(0xFFFFFFFF)
