package com.xiaoyou.diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen(onEnter: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1677FF),
                        Color(0xFF0E5BD6),
                        Color(0xFF0A3E96)
                    )
                )
            )
            .clickable { onEnter() }
    ) {
        // Center content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .background(
                        Color(0x24FFFFFF),
                        androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Waves,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(46.dp)
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "小幽日记",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "MOTION DIARY",
                color = Color(0xBAFFFFFF),
                fontSize = 12.sp,
                letterSpacing = 7.sp
            )
            Spacer(Modifier.height(32.dp))
            Text(
                "传感器动作实时监测系统",
                color = Color(0xB3FFFFFF),
                fontSize = 13.sp
            )
        }

        // Bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.width(24.dp).height(4.dp)
                        .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                )
                Box(Modifier.size(4.dp).background(Color(0x66FFFFFF), androidx.compose.foundation.shape.CircleShape))
                Box(Modifier.size(4.dp).background(Color(0x66FFFFFF), androidx.compose.foundation.shape.CircleShape))
            }
            Spacer(Modifier.height(16.dp))
            Text("v1.0.0 · 检测引擎已就绪", color = Color(0x8CFFFFFF), fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Text("轻触屏幕进入", color = Color(0x66FFFFFF), fontSize = 11.sp)
        }
    }
}
