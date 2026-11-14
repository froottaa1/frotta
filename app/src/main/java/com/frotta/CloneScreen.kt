package com.frotta

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val mediaKeys = setOf("photos", "videos", "audio")

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
    CloneCategory("docs", "المستندات", "PDF وWord وExcel وDWG بمساراتها", Icons.Rounded.Description),
    CloneCategory("contacts", "الأسماء", "جهات الاتصال", Icons.Rounded.Contacts),
    CloneCategory("calls", "سجل المكالمات", "بالتواريخ والمدد", Icons.Rounded.Call),
    CloneCategory("sms", "الرسائل", "بتواريخها واتجاهها", Icons.Rounded.Sms),
    CloneCategory("calendar", "التقويم", "المواعيد والأحداث", Icons.Rounded.CalendarMonth),
    CloneCategory("apps", "التطبيقات", "ملفات التثبيت فقط", Icons.Rounded.Apps)
)

data class AppRow(
    val pkg: String,
    val label: String,
    val ready: Boolean,
    val installed: Boolean,
    val error: String,
    val installing: Boolean
)

@Composable
private fun ModeChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) Pal.modeOnBg else Pal.modeOffBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) Pal.modeOnFg else Pal.modeOffFg
        )
    }
}

@Composable
fun CloneSelectScreen(
    selected: Set<String>,
    modes: Map<String, String>,
    pickedCount: Map<String, Int>,
    folderCount: Map<String, Int>,
    onToggle: (String) -> Unit,
    onMode: (String, String) -> Unit,
    onStart: () -> Unit
) {
    Column {
        Text(
            text = "اختار اللي عايز تنسخه",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.onPage
        )
        Spacer(modifier = Modifier.height(12.dp))

        cloneCategories.forEach { cat ->
            val isOn = cat.key in selected
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isOn) Pal.chip else Pal.soft)
                    .clickable { onToggle(cat.key) }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Pal.circleBg(isOn)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = null,
                            tint = Pal.onCircle(isOn),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cat.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOn) Pal.onChip else Pal.onSoft
                        )
                        Text(
                            text = cat.subtitle,
                            fontSize = 12.sp,
                            color = if (isOn) Pal.onChipSub else Pal.onSoft
                        )
                    }
                    Checkbox(
                        checked = isOn,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Pal.onChip,
                            checkmarkColor = Pal.chip,
                            uncheckedColor = Pal.onSoft
                        )
                    )
                }

                if (isOn && (cat.key in mediaKeys || cat.key == "apps" || cat.key == "docs")) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val mode = modes[cat.key] ?: "all"
                    val picked = pickedCount[cat.key] ?: 0
                    val folders = folderCount[cat.key] ?: 0
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeChip("الكل", mode == "all") { onMode(cat.key, "all") }
                        ModeChip(
                            if (picked > 0) "اختيار ($picked)" else "اختيار",
                            mode == "pick"
                        ) { onMode(cat.key, "pick") }
                        if (cat.key != "apps") {
                            ModeChip(
                                if (folders > 0) "مجلدات ($folders)" else "مجلدات",
                                mode == "folders"
                            ) { onMode(cat.key, "folders") }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = "التطبيقات بتتنقل كملفات تثبيت بس، من غير بياناتها أو حساباتها. والمستندات بترجع لنفس مساراتها لو الهاتف الجديد مفعّل الوصول لكل الملفات.",
            fontSize = 12.sp,
            color = Pal.onPage
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
fun FolderPickerScreen(
    title: String,
    folders: List<MediaFolder>?,
    chosen: Set<String>,
    onToggle: (String) -> Unit,
    onDone: () -> Unit
) {
    Column {
        Text(
            text = "اختار المجلدات: $title",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.onPage
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (folders == null) {
            Text(text = "جاري التحميل...", fontSize = 14.sp, color = Pal.onPage)
        } else if (folders.isEmpty()) {
            Text(text = "مفيش مجلدات", fontSize = 14.sp, color = Pal.onPage)
        } else {
            folders.forEach { f ->
                val isOn = f.path in chosen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isOn) Pal.chip else Pal.soft)
                        .clickable { onToggle(f.path) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = f.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOn) Pal.onChip else Pal.onSoft
                        )
                        Text(
                            text = f.path + "  ·  " + f.count + " ملف",
                            fontSize = 11.sp,
                            color = if (isOn) Pal.onChipSub else Pal.onSoft
                        )
                    }
                    Checkbox(
                        checked = isOn,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = Pal.onChip,
                            checkmarkColor = Pal.chip,
                            uncheckedColor = Pal.onSoft
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(text = "تم (${chosen.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CloneProgressCard(done: Int, failed: Int, total: Int) {
    val finished = done + failed
    val p = if (total > 0) (finished.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Pal.card)
            .padding(20.dp)
    ) {
        Text(
            text = if (total > 0 && finished >= total) "اكتمل الإرسال" else "جاري الإرسال...",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.accent
        )
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = p,
            color = Pal.accent,
            trackColor = Color(0xFF444444),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$done / $total" + if (failed > 0) "   (فشل $failed)" else "",
            fontSize = 14.sp,
            color = Pal.onCardSub
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
fun CloneRestoreScreen(counts: Map<String, Int> = emptyMap()) {
    Column {
        Text(
            text = "الاسترداد",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.onPage
        )
        Text(
            text = "الملفات بتتحفظ في نفس مجلداتها",
            fontSize = 14.sp,
            color = Pal.onPage
        )
        Spacer(modifier = Modifier.height(12.dp))

        cloneCategories.forEach { cat ->
            val got = counts.containsKey(cat.key)
            val n = counts[cat.key] ?: 0
            val unit = when {
                cat.key in mediaKeys || cat.key == "docs" -> "ملف"
                cat.key == "apps" -> "تطبيق"
                cat.key == "sms" -> "رسالة"
                else -> "عنصر"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Pal.soft)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = cat.icon,
                    contentDescription = null,
                    tint = Pal.onSoft,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = cat.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Pal.onSoft,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (got) "$n $unit" else "في الانتظار",
                    fontSize = 13.sp,
                    color = Pal.onSoft
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AppsInstallCard(
    rows: List<AppRow>,
    canInstall: Boolean,
    onAllowInstall: () -> Unit,
    onInstallAll: () -> Unit,
    onInstallOne: (String) -> Unit
) {
    val done = rows.count { it.installed }
    val pending = rows.count { it.ready && !it.installed }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Pal.card)
            .padding(16.dp)
    ) {
        Text(
            text = "التطبيقات المستلمة ($done / ${rows.size} مثبّت)",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Pal.accent
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (!canInstall) {
            Text(
                text = "لازم تسمح لـ Frotta بتثبيت التطبيقات (مرة واحدة)",
                fontSize = 13.sp,
                color = Pal.onCardSub
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAllowInstall,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Pal.accent,
                    contentColor = Pal.ink
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("السماح بالتثبيت", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onInstallAll,
            enabled = canInstall && pending > 0,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Pal.accent,
                contentColor = Pal.ink
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تثبيت الكل ($pending)", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))

        rows.forEach { r ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = r.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Pal.accent,
                        maxLines = 1
                    )
                    Text(
                        text = when {
                            r.installed -> "مثبّت"
                            r.installing -> "جاري التثبيت..."
                            r.error.isNotEmpty() -> r.error
                            r.ready -> "جاهز للتثبيت"
                            else -> "قيد الاستلام"
                        },
                        fontSize = 11.sp,
                        color = Pal.onCardSub
                    )
                }
                if (r.ready && !r.installed && !r.installing && canInstall) {
                    TextButton(onClick = { onInstallOne(r.pkg) }) {
                        Text("تثبيت", color = Pal.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}
