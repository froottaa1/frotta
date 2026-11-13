package com.frotta

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RInk = Color(0xFF111111)
private val RYellow = Color(0xFFFFD600)
private val RSoft = Color(0xFFFFE866)
private val RGreen = Color(0xFF4CAF50)
private val RAmber = Color(0xFFFFB300)

@Composable
fun ReceiverWaiting(qr: ImageBitmap, deviceName: String, code: String) {
    val transition = rememberInfiniteTransition(label = "scan")
    val p by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "في انتظار الاتصال...",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = RInk
        )
        Spacer(modifier = Modifier.height(8.dp))

        RadarView(diameter = 200.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(36.dp))
                .background(RInk)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White)
                ) {
                    Image(
                        bitmap = qr,
                        contentDescription = "QR",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val y = size.height * p
                        drawLine(
                            color = RAmber.copy(alpha = 0.55f),
                            start = Offset(12.dp.toPx(), y),
                            end = Offset(size.width - 12.dp.toPx(), y),
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF2A2A2A))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhoneAndroid,
                        contentDescription = null,
                        tint = RYellow,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = deviceName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = RYellow,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(RGreen)
                    )
                }
            }

            Canvas(modifier = Modifier.matchParentSize()) {
                val len = 34.dp.toPx()
                val inset = 16.dp.toPx()
                val sw = 5.dp.toPx()
                val w = size.width
                val h = size.height

                fun seg(a: Offset, b: Offset) {
                    drawLine(
                        color = RYellow,
                        start = a,
                        end = b,
                        strokeWidth = sw,
                        cap = StrokeCap.Round
                    )
                }

                seg(Offset(inset, inset), Offset(inset + len, inset))
                seg(Offset(inset, inset), Offset(inset, inset + len))

                seg(Offset(w - inset, inset), Offset(w - inset - len, inset))
                seg(Offset(w - inset, inset), Offset(w - inset, inset + len))

                seg(Offset(inset, h - inset), Offset(inset + len, h - inset))
                seg(Offset(inset, h - inset), Offset(inset, h - inset - len))

                seg(Offset(w - inset, h - inset), Offset(w - inset - len, h - inset))
                seg(Offset(w - inset, h - inset), Offset(w - inset, h - inset - len))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "أو اكتب الكود ده في الهاتف الآخر",
            fontSize = 13.sp,
            color = RInk
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(RInk)
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Text(
                text = code.chunked(4).joinToString("  "),
                color = RYellow,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(RSoft)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RInk),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = RYellow,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "اطلب من الهاتف الآخر يضغط \"إرسال\" ويختار اسم هاتفك ($deviceName)، أو يمسح الرمز.",
                fontSize = 13.sp,
                color = RInk,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
