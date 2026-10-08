package com.xiaoyou.diary.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 液态玻璃折射 / 色散色差修饰符。
 *
 * 说明：基于 AGSL 的 RuntimeShader 折射与色散色差效果需要 Compose UI 1.7+
 * （android.graphics 层的 RuntimeShader / RuntimeShaderEffect）。当前工程锁定
 * Kotlin 1.9.24 + Compose BOM 2024.02（UI 1.6.1），为避免高风险的 Kotlin 2.0 全量升级，
 * 此处保留为占位：真实的背景磨砂模糊由 Haze 提供（液态玻璃的核心质感），
 * 玻璃的高光边缘、半透明色调、阴影由 glassBar 样式层完成。
 * 未来升级到 Compose 1.7+ 后，可在此重新接入 AGSL 着色器实现折射变形与色散。
 */
@Composable
fun Modifier.chromaticRefraction(): Modifier = this
