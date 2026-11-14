package com.frotta

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DInk = Color(0xFF111111)
private val DYellow = Color(0xFFFFD600)
private val DSoft = Color(0xFFFFE866)

@Composable
fun DeveloperCard(hasLink: Boolean, onFacebook: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(DSoft)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DInk),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SupportAgent,
                    contentDescription = null,
                    tint = DYellow,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "تواصل مع المطور",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DInk
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (hasLink) {
                "لأي استفسار أو اقتراح أو مشكلة، تواصل معنا على صفحة الفيسبوك."
            } else {
                "رابط صفحة الفيسبوك هيتضاف قريباً."
            },
            fontSize = 13.sp,
            color = DInk
        )
        if (hasLink) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onFacebook,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("فتح صفحة الفيسبوك", fontWeight = FontWeight.Bold)
            }
        }
    }
}
