package com.frotta

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HInk = Color(0xFF111111)
private val HYellow = Color(0xFFFFD600)
private val HSoft = Color(0xFFFFE866)

private val dateFmt = SimpleDateFormat("d/M  h:mm a", Locale.getDefault())

private fun hSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024)
}

@Composable
fun SquareCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(28.dp))
            .background(HInk)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(HYellow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HInk,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            color = HYellow,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = HSoft,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HistoryRow(e: HistoryEntry) {
    val icon = when (e.kind) {
        "send" -> Icons.Rounded.FileUpload
        "receive" -> Icons.Rounded.FileDownload
        else -> Icons.Rounded.ContentCopy
    }
    val title = when (e.kind) {
        "send" -> "إرسال"
        "receive" -> "استقبال"
        "clone_send" -> "استنساخ (من الهاتف ده)"
        "clone_receive" -> "استنساخ (للهاتف ده)"
        else -> "عملية"
    }
    val details = buildString {
        append(e.files).append(" ملف")
        if (e.bytes > 0) append("  ·  ").append(hSize(e.bytes))
        if (e.skipped > 0) append("  ·  تخطي ").append(e.skipped)
        if (e.failed > 0) append("  ·  فشل ").append(e.failed)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(HSoft)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(HInk),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HYellow,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = HInk
            )
            Text(text = e.peer, fontSize = 13.sp, color = HInk, maxLines = 1)
            Text(text = details, fontSize = 12.sp, color = HInk)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = dateFmt.format(Date(e.time)),
            fontSize = 11.sp,
            color = HInk
        )
    }
}

@Composable
fun RecentSection(entries: List<HistoryEntry>, onShowAll: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "آخر العمليات",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = HInk,
                modifier = Modifier.weight(1f)
            )
            if (entries.isNotEmpty()) {
                TextButton(onClick = onShowAll) {
                    Text("عرض الكل", fontWeight = FontWeight.Bold, color = HInk)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(2.dp, HInk, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = HInk,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "لا توجد عمليات سابقة",
                        fontSize = 14.sp,
                        color = HInk
                    )
                }
            }
        } else {
            entries.take(3).forEach {
                HistoryRow(it)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun HistoryScreen(entries: List<HistoryEntry>, onClear: () -> Unit) {
    Column {
        Text(
            text = "السجل",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = HInk
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (entries.isEmpty()) {
            Text(text = "مفيش عمليات سابقة", fontSize = 14.sp, color = HInk)
        } else {
            entries.forEach {
                HistoryRow(it)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("مسح السجل", color = HInk, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
