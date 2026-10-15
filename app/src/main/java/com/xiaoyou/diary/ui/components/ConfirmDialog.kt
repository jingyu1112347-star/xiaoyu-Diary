package com.xiaoyou.diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.ui.theme.LocalAppColors
import com.xiaoyou.diary.viewmodel.AppViewModel

@Composable
fun ConfirmDialog(appVm: AppViewModel) {
    val c = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { appVm.cancelConfirm() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 42.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(c.elevated)
                .border(1.dp, c.rule2, RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                appVm.confirmTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = c.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(9.dp))
            Text(
                appVm.confirmDesc,
                fontSize = 13.sp,
                color = c.ink2,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.rule))
            Row(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .tapScale { appVm.cancelConfirm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 15.sp, color = c.ink2)
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(c.rule))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .tapScale { appVm.confirmAction() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("确定", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = c.red)
                }
            }
        }
    }
}
