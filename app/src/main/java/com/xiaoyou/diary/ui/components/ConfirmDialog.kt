package com.xiaoyou.diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.ui.theme.*
import com.xiaoyou.diary.viewmodel.AppViewModel

@Composable
fun ConfirmDialog(appVm: AppViewModel) {
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80101828))
            .clickable { appVm.cancelConfirm() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .liquidGlass(RoundedCornerShape(14.dp))
                .clickable(enabled = false) {}
                .padding(top = 24.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(appVm.confirmTitle, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
            Spacer(Modifier.height(8.dp))
            Text(appVm.confirmDesc, fontSize = 13.sp, color = colors.ink2, lineHeight = 20.sp)
            Spacer(Modifier.height(20.dp))
            // Buttons
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { appVm.cancelConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 15.sp, color = colors.ink2)
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(colors.line)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { appVm.confirmAction() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("确定", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Danger)
                }
            }
        }
    }
}
