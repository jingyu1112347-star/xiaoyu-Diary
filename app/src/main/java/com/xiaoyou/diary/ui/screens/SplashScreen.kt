package com.xiaoyou.diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.ui.theme.LocalAppColors
import com.xiaoyou.diary.ui.components.tapScale

@Composable
fun SplashScreen(onEnter: () -> Unit) {
    val c = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEnter
            )
    ) {
        // ---------- 中部品牌区 ----------
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(c.surface)
                    .border(1.dp, c.rule2, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Waves,
                    null,
                    tint = c.blue,
                    modifier = Modifier.size(46.dp)
                )
            }
            Spacer(Modifier.height(22.dp))
            Text("小幽日记", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
            Spacer(Modifier.height(14.dp))
            Text(
                "读取手机加速度计、陀螺仪与重力传感器，\n本地离线识别 8 类人体行为，\n实时给出类别、置信度与姿态角。",
                fontSize = 13.sp, color = c.ink2, lineHeight = 22.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "行走 · 慢跑 · 快跑 · 静坐 · 站立\n倾斜 · 平躺 · 侧躺",
                fontSize = 13.sp, color = c.ink2, lineHeight = 22.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(26.dp))
            // 隐私提示
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(c.surface2)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(Icons.Filled.Shield, null, tint = c.green, modifier = Modifier.size(14.dp))
                Text("全部本地运算 · 数据绝不上传", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.ink2)
            }
        }

        // ---------- 底部：FAB 引导 ----------
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("点这里开始第一次检测", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.ink2)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(c.blue)
                        .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                        .tapScale(onClick = onEnter),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("v1.1.0 · 检测引擎已就绪", fontSize = 11.sp, color = c.ink3)
        }
    }
}
