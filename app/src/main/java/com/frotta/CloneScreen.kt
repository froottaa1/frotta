package com.frotta

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CInk = Color(0xFF111111)
private val CYellow = Color(0xFFFFD600)
private val CSoft = Color(0xFFFFE866)

data class CloneCategory(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

val cloneCategories = listOf(
    CloneCategory("photos", "الصور", "بنفس ترتيب المعرض", Icons.Rounded.Image),
    CloneCategory("videos", "الفيديوهات", "بنفس الفولدرات", Icons.Rounded.Videocam),
    CloneCategory("audio", "الصوتيات", "موسيقى وتسجيلات", Icons.Rounded.MusicNote),
    CloneCategory("contacts", "الأسماء", "جهات الاتصال", Icons.Rounded.Contacts),
    CloneCategory("calls", "سجل المكالمات", "بالتواريخ والمدد", Icons.Rounded.Call),
    CloneCategory("sms", "الرسائل", "بتواريخها واتجاهها", Icons.Rounded.Sms),
    CloneCategory("apps", "التطبيقات", "ملفات التثبيت فقط", Icons.Rounded.Apps)
)

@Composable
fun CloneSelectScreen(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onStart: () -> Unit
) {
    Column {
        Text(
            text = "اختار اللي عايز تنسخه",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CInk
        )
        Spacer(modifier = Modifier.height(12.dp))

        cloneCategories.forEach { cat ->
            val isOn = cat.key in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isOn) CInk else CSoft)
                    .clickable { onToggle(cat.key) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(CYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = null,
                        tint = CInk,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cat.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOn) CYellow else CInk
                    )
                    Text(
                        text = cat.subtitle,
                        fontSize = 12.sp,
                        color = if (isOn) CSoft else CInk
                    )
                }
                Checkbox(
                    checked = isOn,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = CYellow,
                        checkmarkColor = CInk,
                        uncheckedColor = CInk
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = "التطبيقات بتتنقل كملفات تثبيت بس، من غير بياناتها أو حساباتها.",
            fontSize = 12.sp,
            color = CInk
        )
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onStart,
            enabled = selected.isNotEmpty(),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Text(
                text = "ابدأ الاستنساخ (${selected.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CloneRestoreScreen(progress: Map<String, Float> = emptyMap()) {
    Column {
        Text(
            text = "الاسترداد",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CInk
        )
        Text(
            text = "في انتظار الهاتف القديم يبدأ النقل...",
            fontSize = 14.sp,
            color = CInk
        )
        Spacer(modifier = Modifier.height(12.dp))

        cloneCategories.forEach { cat ->
            val p = (progress[cat.key] ?: 0f).coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CSoft)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = null,
                        tint = CInk,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = cat.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CInk,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (p >= 1f) "تم" else if (p > 0f) "${(p * 100).toInt()}%" else "في الانتظار",
                        fontSize = 12.sp,
                        color = CInk
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = p,
                    color = CInk,
                    trackColor = CYellow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
