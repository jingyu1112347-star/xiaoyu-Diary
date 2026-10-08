package com.xiaoyou.diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.ui.theme.*
import com.xiaoyou.diary.viewmodel.AppViewModel
import com.xiaoyou.diary.viewmodel.DiaryViewModel

@Composable
fun DiarySheet(
    appVm: AppViewModel,
    diaryVm: DiaryViewModel
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // Handle
        Box(
            modifier = Modifier
                .width(40.dp).height(4.dp)
                .background(Color(0xFFDCE0E6), RoundedCornerShape(999.dp))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("写日记", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .clickable { appVm.closeDiarySheet() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Close, null, tint = colors.navIcon, modifier = Modifier.size(17.dp))
            }
        }
        Spacer(Modifier.height(16.dp))

        // Mood selection
        Text("选择心情", fontSize = 12.sp, color = colors.ink3)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            diaryVm.moods.forEach { (mood, emoji) ->
                val active = diaryVm.selectedMood == mood
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) colors.moodActiveBg else Color.Transparent)
                        .clickable { diaryVm.selectMood(mood) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(emoji, fontSize = 24.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        mood,
                        fontSize = 10.sp,
                        color = if (active) Primary else colors.ink3,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Text input
        Text("今天想说点什么", fontSize = 12.sp, color = colors.ink3)
        Spacer(Modifier.height(8.dp))
        TextField(
            value = diaryVm.diaryText,
            onValueChange = { diaryVm.updateText(it) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, if (diaryVm.diaryText.isNotEmpty()) Primary else colors.diaryInputBorder, RoundedCornerShape(10.dp)),
            placeholder = { Text("记录今天的行为状态、身体感受或心情...", fontSize = 14.sp, color = colors.ink3) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = colors.diaryInputBg,
                unfocusedContainerColor = colors.diaryInputBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Primary
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = colors.ink1, lineHeight = 22.sp)
        )

        // Auto tags
        Spacer(Modifier.height(16.dp))
        Text("自动关联今日数据", fontSize = 12.sp, color = colors.ink3)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BehaviorChip(BehaviorType.WALK, "行走 01:23")
            BehaviorChip(BehaviorType.SIT, "静坐 02:10")
            BehaviorChip(BehaviorType.STAND, "站立 00:48")
        }
        Spacer(Modifier.height(20.dp))

        // Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.ghostBtnBg)
                    .border(1.dp, colors.ghostBtnBorder, RoundedCornerShape(10.dp))
                    .clickable { appVm.closeDiarySheet() },
                contentAlignment = Alignment.Center
            ) {
                Text("取消", fontSize = 15.sp, color = colors.ghostBtnText)
            }
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary)
                    .clickable {
                        if (diaryVm.saveDiary { appVm.showToast(it) }) {
                            appVm.closeDiarySheet()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("保存日记", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun BehaviorChip(type: BehaviorType, text: String) {
    Box(
        modifier = Modifier
            .background(type.color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, fontSize = 11.sp, color = type.color)
    }
}
