package com.frotta

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val UInk = Color(0xFF111111)
private val UYellow = Color(0xFFFFD600)
private val USoft = Color(0xFFFFE866)

@Composable
fun RadarView(diameter: Dp = 240.dp) {
    val transition = rememberInfiniteTransition(label = "radar")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    Box(
        modifier = Modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            val r = size.minDimension / 2f

            drawCircle(color = UInk, radius = r - 2f, center = c, style = Stroke(width = 4f))
            listOf(0.75f, 0.5f, 0.28f).forEach { f ->
                drawCircle(
                    color = UInk.copy(alpha = 0.35f),
                    radius = r * f,
                    center = c,
                    style = Stroke(width = 2f)
                )
            }
            drawLine(
                color = UInk.copy(alpha = 0.25f),
                start = Offset(c.x - r, c.y),
                end = Offset(c.x + r, c.y),
                strokeWidth = 2f
            )
            drawLine(
                color = UInk.copy(alpha = 0.25f),
                start = Offset(c.x, c.y - r),
                end = Offset(c.x, c.y + r),
                strokeWidth = 2f
            )

            for (i in 0 until 60) {
                val a = Math.toRadians(i * 6.0)
                val len = if (i % 5 == 0) 16f else 8f
                val outer = r - 8f
                val inner = outer - len
                val sx = c.x + (inner * cos(a)).toFloat()
                val sy = c.y + (inner * sin(a)).toFloat()
                val ex = c.x + (outer * cos(a)).toFloat()
                val ey = c.y + (outer * sin(a)).toFloat()
                drawLine(
                    color = UInk.copy(alpha = 0.7f),
                    start = Offset(sx, sy),
                    end = Offset(ex, ey),
                    strokeWidth = 2f
                )
            }

            val sweepR = r - 10f
            rotate(degrees = angle, pivot = c) {
                for (k in 0 until 28) {
                    val alpha = 0.32f * (1f - k / 28f)
                    drawArc(
                        color = UInk.copy(alpha = alpha),
                        startAngle = -(k + 1) * 2f,
                        sweepAngle = 2f,
                        useCenter = true,
                        topLeft = Offset(c.x - sweepR, c.y - sweepR),
                        size = Size(sweepR * 2f, sweepR * 2f)
                    )
                }
                drawLine(
                    color = UInk,
                    start = c,
                    end = Offset(c.x + sweepR, c.y),
                    strokeWidth = 4f
                )
            }
        }
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(UYellow)
                .border(3.dp, UInk, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PhoneAndroid,
                contentDescription = null,
                tint = UInk,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
fun DeviceCard(name: String, connecting: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(UInk)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(UYellow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PhoneAndroid,
                contentDescription = null,
                tint = UInk,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = UYellow,
                maxLines = 1
            )
            Text(
                text = if (connecting) "بطلب الاتصال... استنى الهاتف الآخر يقبل" else "اضغط للاتصال",
                fontSize = 12.sp,
                color = USoft
            )
        }
        if (connecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = UYellow,
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
fun OptionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(UInk)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(UYellow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = UInk,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = UYellow
            )
            Text(text = subtitle, fontSize = 12.sp, color = USoft)
        }
    }
}

@Composable
private fun SettingCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(USoft)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(UInk),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = UYellow,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = UInk
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun SettingsScreen(
    deviceName: String,
    version: String,
    cacheSize: String,
    message: String,
    onSaveName: (String) -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit
) {
    var name by remember(deviceName) { mutableStateOf(deviceName) }

    Column {
        Text(
            text = "الإعدادات",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = UInk
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (message.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(UInk)
                    .padding(14.dp)
            ) {
                Text(text = message, color = UYellow, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        SettingCard(Icons.Rounded.PhoneAndroid, "اسم الهاتف الظاهر للآخرين") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.replace("|", "").take(30) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { onSaveName(name) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("حفظ", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        SettingCard(Icons.Rounded.Share, "مشاركة التطبيق") {
            Text(
                text = "ابعت Frotta لهاتف تاني عن طريق البلوتوث أو Quick Share. الهاتف التاني لازم يسمح بالتثبيت من مصدر خارجي.",
                fontSize = 13.sp,
                color = UInk
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onShare,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("مشاركة التطبيق", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        SettingCard(Icons.Rounded.Delete, "الملفات المؤقتة") {
            Text(
                text = if (cacheSize.isEmpty()) "جاري الحساب..." else "الحجم الحالي: $cacheSize",
                fontSize = 13.sp,
                color = UInk
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("مسح الملفات المؤقتة", color = UInk, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        SettingCard(Icons.Rounded.Info, "حول التطبيق") {
            Text(
                text = "Frotta",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = UInk
            )
            Text(
                text = "تطبيق لنقل الملفات والبيانات بين هواتف أندرويد بدون إنترنت.",
                fontSize = 13.sp,
                color = UInk
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "الإصدار: $version", fontSize = 13.sp, color = UInk)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "تطوير: أبو عمر الخيامي",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = UInk
            )
            Text(text = "أبو عمر الخيامي سوفت وير", fontSize = 13.sp, color = UInk)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
