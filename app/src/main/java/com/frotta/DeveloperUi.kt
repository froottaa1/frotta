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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeveloperCard(hasLink: Boolean, onFacebook: () -> Unit) {
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
                    imageVector = Icons.Rounded.SupportAgent,
                    contentDescription = null,
                    tint = Pal.onChip,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "تواصل مع المطور",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Pal.onSoft
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasLink) {
                "لأي استفسار أو اقتراح أو مشكلة، تواصل معنا على صفحة الفيسبوك."
            } else {
                "رابط صفحة الفيسبوك هيتضاف قريباً."
            },
            fontSize = 12.sp,
            color = Pal.onSoft
        )
        if (hasLink) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onFacebook,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text("فتح صفحة الفيسبوك", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
