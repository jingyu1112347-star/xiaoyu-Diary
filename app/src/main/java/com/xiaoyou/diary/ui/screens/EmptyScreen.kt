package com.xiaoyou.diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.*
import com.xiaoyou.diary.viewmodel.AppViewModel

@Composable
fun EmptyScreen(appVm: AppViewModel) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("历史记录", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        .clickable { appVm.showToast("筛选面板已打开") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FilterList, null, tint = colors.navIcon, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        .clickable { appVm.showToast("已进入批量管理模式") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Checklist, null, tint = colors.navIcon, modifier = Modifier.size(17.dp))
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon with nested circles
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(120.dp).background(Primary.copy(alpha = 0.07f), CircleShape))
                Box(Modifier.size(86.dp).background(Primary.copy(alpha = 0.10f), CircleShape)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.BarChart, null, tint = Primary.copy(alpha = 0.7f), modifier = Modifier.size(36.dp))
                    }
                }
                // Plus badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = (-8).dp)
                        .size(36.dp)
                        .background(Color.White, CircleShape)
                        .shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = Primary, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            Text("还没有检测记录", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
            Spacer(Modifier.height(8.dp))
            Text(
                "返回首页点击「开始检测」，\n小幽会自动记录你的每一次行为状态",
                fontSize = 13.sp, color = colors.ink3, textAlign = TextAlign.Center, lineHeight = 20.sp
            )
            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary)
                    .clickable { appVm.setTab(com.xiaoyou.diary.ui.components.TabKey.HOME) }
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) {
                Text("开始首次检测", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(40.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f).height(1.dp).background(colors.line))
                Text("小提示", fontSize = 11.sp, color = colors.ink3, modifier = Modifier.padding(horizontal = 12.dp))
                Box(Modifier.weight(1f).height(1.dp).background(colors.line))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "检测时请将手机贴身放置或握持，\n以获得更准确的姿态判定结果",
                fontSize = 12.sp, color = colors.ink3, textAlign = TextAlign.Center, lineHeight = 20.sp
            )
        }
    }
}
