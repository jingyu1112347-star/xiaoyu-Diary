package com.xiaoyou.diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.LocalAppColors
import com.xiaoyou.diary.viewmodel.AppViewModel

@Composable
fun CalibrateScreen(appVm: AppViewModel, onFinish: () -> Unit) {
    val c = LocalAppColors.current

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        // ---------- 导航栏：左返回 · 居中标题 ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "返回") { appVm.setTab(TabKey.HOME) }
            Text(
                "传感器校准",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.ink,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(44.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(c.orangeSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧭", fontSize = 32.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("校准前请按步骤操作", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "校准可消除传感器零偏，提升姿态解算与行为识别精度",
                        fontSize = 12.sp, color = c.ink3, textAlign = TextAlign.Center
                    )
                }
            }

            item {
                AppSurface {
                    CalStep(1, "水平静置设备",
                        "将手机屏幕朝上，平放在稳固的水平桌面，避免任何震动与触碰。",
                        done = true,
                        footnote = "检测到水平度 0.6°（合格）")
                    Hairline()
                    CalStep(2, "保持静止 3 秒",
                        "采样过程中请勿移动设备，系统将自动采集 150 组静态样本。",
                        done = true,
                        footnote = "150 / 150 组 · 已完成",
                        progress = 1f)
                    Hairline()
                    CalStep(3, "旋转设备一周",
                        "将设备沿三个轴向各缓慢旋转 360°，用于标定磁力计与陀螺仪。",
                        done = false)
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.orangeSoft)
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Text("⚠", fontSize = 13.sp, color = c.orange)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "校准期间请远离磁性物体与金属桌面，否则可能导致偏航角漂移。",
                        fontSize = 12.sp, color = c.orange, lineHeight = 18.sp
                    )
                }
            }

            item {
                PrimaryButton(
                    text = "完成校准",
                    modifier = Modifier.padding(top = 26.dp),
                    onClick = {
                        // Toast 由 onFinish 回调统一弹出，避免重复提示
                        onFinish()
                    }
                )
            }
        }
    }
}

@Composable
private fun CalStep(
    n: Int,
    title: String,
    desc: String,
    done: Boolean,
    footnote: String? = null,
    progress: Float? = null
) {
    val c = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (done) c.greenSoft else c.surface2),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$n",
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = if (done) c.green else c.ink2
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            Spacer(Modifier.height(2.dp))
            Text(desc, fontSize = 11.5.sp, color = c.ink3, lineHeight = 18.sp)
            if (progress != null) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(c.surface2)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(c.green)
                    )
                }
            }
            if (footnote != null) {
                Spacer(Modifier.height(7.dp))
                Text(footnote, fontSize = 11.sp, color = c.ink3)
            }
        }
    }
}
