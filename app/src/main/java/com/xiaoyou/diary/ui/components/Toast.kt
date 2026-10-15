package com.xiaoyou.diary.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

data class ToastState(val text: String, val id: Long = System.currentTimeMillis(), val type: ToastType = ToastType.OK)

enum class ToastType { OK, ERROR, INFO }

@Composable
fun ToastHost(
    toast: ToastState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            delay(2000)
            onDismiss()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
            modifier = Modifier.padding(bottom = 138.dp)
        ) {
            toast?.let {
                val icon: ImageVector = when (it.type) {
                    ToastType.OK -> Icons.Filled.Check
                    ToastType.ERROR -> Icons.Filled.Close
                    ToastType.INFO -> Icons.Filled.Notifications
                }
                val iconColor = when (it.type) {
                    ToastType.OK -> c.green
                    ToastType.ERROR -> c.red
                    ToastType.INFO -> c.blue
                }
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xF21C212A))
                        .border(1.dp, c.rule2, RoundedCornerShape(14.dp))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
                    Text(
                        text = it.text,
                        color = Color(0xFFF3F6FA),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
