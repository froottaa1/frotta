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
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

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
            val line = Pal.radar

            drawCircle(color = line, radius = r - 2f, center = c, style = Stroke(width = 4f))
            listOf(0.75f, 0.5f, 0.28f).forEach { f ->
                drawCircle(
                    color = line.copy(alpha = 0.35f),
                    radius = r * f,
                    center = c,
                    style = Stroke(width = 2f)
                )
            }
            drawLine(
                color = line.copy(alpha = 0.25f),
                start = Offset(c.x - r, c.y),
                end = Offset(c.x + r, c.y),
                strokeWidth = 2f
            )
            drawLine(
                color = line.copy(alpha = 0.25f),
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
                    color = line.copy(alpha = 0.7f),
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
                        color = line.copy(alpha = alpha),
                        startAngle = -(k + 1) * 2f,
                        sweepAngle = 2f,
                        useCenter = true,
                        topLeft = Offset(c.x - sweepR, c.y - sweepR),
                        size = Size(sweepR * 2f, sweepR * 2f)
                    )
                }
                drawLine(
                    color = line,
                    start = c,
                    end = Offset(c.x + sweepR, c.y),
                    strokeWidth = 4f
                )
            }
        }
        Box(
            modifier = Modifier
                .size(diameter * 0.32f)
                .clip(CircleShape)
                .background(Pal.accent)
                .border(3.dp, Pal.ink, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PhoneAndroid,
                contentDescription = null,
                tint = Pal.ink,
                modifier = Modifier.size(diameter * 0.17f)
            )
        }
    }
}

@Composable
fun DeviceCard(name: String, connecting: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Pal.card)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Pal.accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PhoneAndroid,
                contentDescription = null,
                tint = Pal.ink,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.accent,
                maxLines = 1
            )
            Text(
                text = if (connecting) "بطلب الاتصال... استنى الهاتف الآخر يقبل" else "اضغط للاتصال",
                fontSize = 11.sp,
                color = Pal.onCardSub
            )
        }
        if (connecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Pal.accent,
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
            .clip(RoundedCornerShape(20.dp))
            .background(Pal.card)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Pal.accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Pal.ink,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.accent
            )
            Text(text = subtitle, fontSize = 11.sp, color = Pal.onCardSub)
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
            .clip(RoundedCornerShape(20.dp))
            .background(Pal.soft)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Pal.chip),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Pal.onChip,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.onSoft
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
fun SettingsScreen(
    deviceName: String,
    version: String,
    cacheSize: String,
    message: String,
    confirm: Boolean,
    onConfirm: (Boolean) -> Unit,
    onSaveName: (String) -> Unit,
    onBattery: () -> Unit,
    onShare: () -> Unit,
    onClear: () -> Unit
) {
    var name by remember(deviceName) { mutableStateOf(deviceName) }

    Column {
        Text(
            text = "الإعدادات",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.onPage
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (message.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Pal.card)
                    .padding(12.dp)
            ) {
                Text(text = message, color = Pal.accent, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        SettingCard(Icons.Rounded.PhoneAndroid, "اسم الهاتف الظاهر للآخرين") {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.replace("|", "").take(30) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onSaveName(name) },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text("حفظ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        SettingCard(Icons.Rounded.Lock, "تأكيد قبل الاستقبال") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "اطلب موافقتي قبل ما أي هاتف يتصل بيا ويبعتلي ملفات. (البحث بيطلب الموافقة دايماً)",
                    fontSize = 12.sp,
                    color = Pal.onSoft,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = confirm, onCheckedChange = onConfirm)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        SettingCard(Icons.Rounded.BatteryFull, "استمرار النقل") {
            Text(
                text = "بعض الهواتف بتوقف التطبيق لتوفير البطارية. افتح الإعدادات واختار Frotta Share ثم \"بدون قيود\" أو \"عدم التحسين\".",
                fontSize = 12.sp,
                color = Pal.onSoft
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBattery,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text(
                    "فتح إعدادات البطارية",
                    color = Pal.onSoft,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        SettingCard(Icons.Rounded.Share, "مشاركة التطبيق") {
            Text(
                text = "ابعت التطبيق لهاتف تاني عن طريق البلوتوث أو Quick Share. الهاتف التاني لازم يسمح بالتثبيت من مصدر خارجي.",
                fontSize = 12.sp,
                color = Pal.onSoft
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onShare,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text("مشاركة التطبيق", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        SettingCard(Icons.Rounded.Delete, "الملفات المؤقتة") {
            Text(
                text = if (cacheSize.isEmpty()) "جاري الحساب..." else "الحجم الحالي: $cacheSize",
                fontSize = 12.sp,
                color = Pal.onSoft
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text(
                    "مسح الملفات المؤقتة",
                    color = Pal.onSoft,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        SettingCard(Icons.Rounded.Info, "حول التطبيق") {
            Text(
                text = "Frotta Share",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.onSoft
            )
            Text(
                text = "تطبيق لنقل الملفات والبيانات بين هواتف أندرويد بدون إنترنت.",
                fontSize = 12.sp,
                color = Pal.onSoft
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "الإصدار: $version", fontSize = 12.sp, color = Pal.onSoft)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "المطور: أحمد عمر الخيامي",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.onSoft
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}
