package com.xiaoyou.diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.ui.theme.LocalAppColors
import com.xiaoyou.diary.viewmodel.AppViewModel
import com.xiaoyou.diary.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** UI 心情档位（emoji → VM 中的心情名），由负面到正面排列 */
private val moodOptions = listOf(
    "😞" to "低落",
    "😕" to "疲惫",
    "😐" to "平静",
    "🙂" to "开心",
    "😄" to "兴奋"
)

@Composable
fun DiarySheet(
    appVm: AppViewModel,
    diaryVm: DiaryViewModel
) {
    val c = LocalAppColors.current
    var selectedTags by remember { mutableStateOf(setOf(BehaviorType.WALK)) }
    val dateStr = SimpleDateFormat("M 月 d 日", Locale.getDefault()).format(Date())

    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Sheet))
            .background(c.surface)
            .border(1.dp, c.rule2, RoundedCornerShape(Dimens.Sheet))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // 抓手
        Box(
            modifier = Modifier
                .padding(top = 9.dp)
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(c.rule2)
        )

        // 头部：左 X · 标题 · 右蓝色勾选
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SheetRoundX { appVm.closeDiarySheet() }
            Text("写日记 · $dateStr", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.ink)
            SheetRoundCheck {
                // 把用户勾选的行为标签一并传入保存，修复此前选择被丢弃的问题
                if (diaryVm.saveDiary(selectedTags) { appVm.showToast(it) }) {
                    appVm.closeDiarySheet()
                }
            }
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 28.dp)
        ) {
            SheetDescription("选择心情与绑定行为标签，日记仅保存在本机。")

            // 心情
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moodOptions.forEach { (emoji, mood) ->
                    val active = diaryVm.selectedMood == mood
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (active) c.yellowSoft else c.surface2)
                            .border(
                                width = 2.dp,
                                color = if (active) c.yellow else Color.Transparent,
                                shape = CircleShape
                            )
                            .tapScale { diaryVm.selectMood(mood) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 24.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "绑定行为标签",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.6.sp,
                color = c.ink3
            )
            Spacer(Modifier.height(10.dp))

            // 行为标签（可多选）
            FlowChips(
                types = BehaviorType.entries,
                selected = selectedTags,
                onToggle = { t ->
                    selectedTags = if (t in selectedTags) selectedTags - t else selectedTags + t
                }
            )

            Spacer(Modifier.height(16.dp))

            // 文本输入
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(Dimens.Field))
                    .background(c.surface2)
            ) {
                BasicEditableDiaryText(diaryVm)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FlowChips(
    types: List<BehaviorType>,
    selected: Set<BehaviorType>,
    onToggle: (BehaviorType) -> Unit
) {
    val c = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        types.chunked(4).forEach { rowTypes ->
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                rowTypes.forEach { t ->
                    val on = t in selected
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(if (on) c.behaviorSoft(t) else c.surface2)
                            .border(
                                width = 1.5.dp,
                                color = if (on) c.behavior(t) else Color.Transparent,
                                shape = CircleShape
                            )
                            .tapScale { onToggle(t) }
                            .padding(horizontal = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "#${t.label}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (on) c.behavior(t) else c.ink2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BasicEditableDiaryText(diaryVm: DiaryViewModel) {
    val c = LocalAppColors.current
    androidx.compose.material3.TextField(
        value = diaryVm.diaryText,
        onValueChange = { diaryVm.updateText(it) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "记录此刻的身体状态与感受…\n例如：午后散步 20 分钟，久坐提醒后起身拉伸。",
                fontSize = 14.sp,
                color = c.ink3,
                lineHeight = 22.sp
            )
        },
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = c.blue
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp,
            color = c.ink,
            lineHeight = 23.sp
        )
    )
}
