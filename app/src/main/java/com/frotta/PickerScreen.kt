package com.frotta

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest

private val PInk = Color(0xFF111111)
private val PYellow = Color(0xFFFFD600)
private val PSoft = Color(0xFFFFE866)

data class PickerTab(val key: String, val title: String, val icon: ImageVector)

val pickerTabs = listOf(
    PickerTab("photos", "الصور", Icons.Rounded.Image),
    PickerTab("videos", "الفيديوهات", Icons.Rounded.Videocam),
    PickerTab("audio", "الصوتيات", Icons.Rounded.MusicNote),
    PickerTab("other", "ملفات أخرى", Icons.Rounded.InsertDriveFile)
)

private fun pkSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024)
}

@Composable
private fun RoundIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(PInk)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PYellow,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TabChip(tab: PickerTab, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (active) PInk else PSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = if (active) PYellow else PInk,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = tab.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) PYellow else PInk
        )
    }
}

@Composable
private fun DocsAccessCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PInk)
            .padding(14.dp)
    ) {
        Text(
            text = "عشان يجمع المستندات (PDF وWord وExcel وDWG...) تلقائي، فعّل الوصول لكل الملفات",
            color = PYellow,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PYellow, contentColor = PInk),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تفعيل", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Thumb(item: MediaItem, kind: String, loader: ImageLoader, modifier: Modifier) {
    if (kind == "audio") {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = PInk,
                modifier = Modifier.size(32.dp)
            )
        }
    } else if (kind == "other") {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = item.name.substringAfterLast('.', "?").uppercase().take(4),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PInk
            )
        }
    } else {
        val context = LocalContext.current
        val request = remember(item.uri) {
            ImageRequest.Builder(context)
                .data(item.uri)
                .size(300)
                .crossfade(true)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            imageLoader = loader,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
private fun GridCell(
    item: MediaItem,
    kind: String,
    isOn: Boolean,
    loader: ImageLoader,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(PSoft)
            .then(
                if (isOn) Modifier.border(3.dp, PInk, RoundedCornerShape(14.dp)) else Modifier
            )
            .clickable(onClick = onClick)
    ) {
        Thumb(item, kind, loader, Modifier.fillMaxSize())
        if (kind == "videos") {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .padding(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        if (isOn) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(PYellow)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = PInk,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun ListRow(
    item: MediaItem,
    kind: String,
    isOn: Boolean,
    loader: ImageLoader,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isOn) PInk else PSoft)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Thumb(
            item,
            kind,
            loader,
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PYellow)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOn) PYellow else PInk
            )
            Text(
                text = pkSize(item.size) + if (item.folder.isNotEmpty()) "  ·  " + item.folder else "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
                color = if (isOn) PSoft else PInk
            )
        }
        if (isOn) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = PYellow,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun PickerScreen(
    title: String,
    tabs: List<PickerTab>,
    currentTab: String,
    onTab: (String) -> Unit,
    files: List<MediaItem>?,
    hasAccess: Boolean,
    selected: Set<String>,
    totalSelected: Int,
    extraCount: Int,
    confirmLabel: String,
    docsNeedAccess: Boolean,
    onToggle: (MediaItem) -> Unit,
    onSelectAll: (List<MediaItem>) -> Unit,
    onClear: (List<MediaItem>) -> Unit,
    onPickOther: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAllFiles: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val loader = remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }
    var gridMode by remember { mutableStateOf(true) }
    var searching by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val shown = remember(files, query) {
        val l = files
        if (l == null) {
            null
        } else if (query.isBlank()) {
            l
        } else {
            l.filter { it.name.contains(query.trim(), ignoreCase = true) }
        }
    }
    val list = shown ?: emptyList()
    val useGrid = gridMode && currentTab != "other"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon(Icons.Rounded.ArrowForward, onBack)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PInk,
                modifier = Modifier.weight(1f)
            )
            RoundIcon(if (searching) Icons.Rounded.Close else Icons.Rounded.Search) {
                searching = !searching
                if (!searching) query = ""
            }
            Spacer(modifier = Modifier.width(8.dp))
            RoundIcon(if (gridMode) Icons.Rounded.ViewList else Icons.Rounded.GridView) {
                gridMode = !gridMode
            }
        }

        if (searching) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("بحث بالاسم") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (tabs.size > 1) {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEach { t ->
                    TabChip(t, t.key == currentTab) { onTab(t.key) }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (currentTab == "other" && docsNeedAccess) {
            DocsAccessCard(onOpenAllFiles)
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = list.size.toString() + " ملف",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PInk,
                modifier = Modifier.weight(1f)
            )
            if (list.isNotEmpty()) {
                TextButton(onClick = { onSelectAll(list) }) {
                    Text("تحديد الكل", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { onClear(list) }) {
                    Text("إلغاء التحديد", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (currentTab == "other") {
            OutlinedButton(
                onClick = onPickOther,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(
                    text = "إضافة ملفات يدوياً ($extraCount)",
                    color = PInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                !hasAccess -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "محتاج صلاحية الوصول للملفات. اختار \"السماح بالكل\" مش \"اختيار صور\".",
                            fontSize = 14.sp,
                            color = PInk
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("فتح الإعدادات", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onRefresh,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("تحديث", color = PInk, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                shown == null -> {
                    Text(
                        text = if (currentTab == "other") "جاري البحث عن المستندات..." else "جاري التحميل...",
                        fontSize = 14.sp,
                        color = PInk,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                list.isEmpty() -> {
                    Text(
                        text = if (currentTab == "other") {
                            "مفيش مستندات ظاهرة. فعّل الوصول لكل الملفات أو أضف يدوياً"
                        } else {
                            "مفيش ملفات"
                        },
                        fontSize = 14.sp,
                        color = PInk,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                useGrid -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(list, key = { it.uri.toString() }) { f ->
                            GridCell(f, currentTab, f.uri.toString() in selected, loader) {
                                onToggle(f)
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(list, key = { it.uri.toString() }) { f ->
                            ListRow(f, currentTab, f.uri.toString() in selected, loader) {
                                onToggle(f)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onConfirm,
            enabled = totalSelected > 0,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Text(
                text = "$confirmLabel ($totalSelected)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
