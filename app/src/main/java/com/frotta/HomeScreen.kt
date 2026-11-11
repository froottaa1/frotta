package com.frotta

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.security.SecureRandom
import kotlinx.coroutines.delay

private val Ink = Color(0xFF111111)
private val Yellow = Color(0xFFFFD600)
private val SoftYellow = Color(0xFFFFE866)

private fun requiredPermissions(): Array<String> {
    val list = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= 31) {
        list += Manifest.permission.BLUETOOTH_ADVERTISE
        list += Manifest.permission.BLUETOOTH_CONNECT
        list += Manifest.permission.BLUETOOTH_SCAN
    }
    if (Build.VERSION.SDK_INT >= 33) {
        list += Manifest.permission.NEARBY_WIFI_DEVICES
    }
    return list.toTypedArray()
}

private fun newToken(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val random = SecureRandom()
    return (1..8).map { chars[random.nextInt(chars.length)] }.joinToString("")
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024
    if (mb < 1024) return String.format("%.1f MB", mb)
    return String.format("%.2f GB", mb / 1024)
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf("home") }
    var cloneMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(cloneCategories.map { it.key }.toSet()) }
    var status by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }
    var showManual by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }
    val transfers = remember { mutableStateListOf<TransferItem>() }
    val myName = remember { Build.MODEL }

    val manager = remember {
        NearbyManager(
            context = context,
            onStatus = { status = it },
            onConnected = {
                connected = true
                searching = false
            },
            onDisconnected = { connected = false },
            onTransfer = { item ->
                val i = transfers.indexOfFirst { it.id == item.id }
                if (i >= 0) transfers[i] = item else transfers.add(0, item)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose { manager.stopAll() }
    }

    fun startScan() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { barcode ->
                val raw = barcode.rawValue ?: ""
                if (raw.startsWith("FROTTA:")) {
                    searching = true
                    manager.startSearching(myName, raw.removePrefix("FROTTA:"))
                } else {
                    status = "الكود ده مش تابع لتطبيق Frotta"
                }
            }
            .addOnCanceledListener {
                status = "تم إلغاء المسح. تقدر تدخل الكود يدوياً"
            }
            .addOnFailureListener {
                status = "الكاميرا مش شغالة؟ استخدم الإدخال اليدوي"
                showManual = true
            }
    }

    fun connectManual() {
        val code = manualCode.trim()
        if (code.length != 8) {
            status = "الكود لازم يكون 8 حروف وأرقام"
        } else {
            searching = true
            manager.startSearching(myName, code)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            if (screen == "receive") {
                val t = newToken()
                token = t
                manager.startReceiving(myName, t)
            }
            if (screen == "send") startScan()
        } else {
            status = "لازم توافق على الصلاحيات عشان التطبيق يشتغل"
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) manager.sendFiles(uris)
    }

    val qrImage = remember(token) {
        if (token.isEmpty()) null else makeQr("FROTTA:$token").asImageBitmap()
    }

    fun goHome() {
        manager.stopAll()
        transfers.clear()
        connected = false
        searching = false
        cloneMode = false
        token = ""
        status = ""
        showManual = false
        manualCode = ""
        screen = "home"
    }

    BackHandler(enabled = screen != "home") { goHome() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        if (screen != "home") {
            TextButton(onClick = { goHome() }) {
                Icon(
                    imageVector = Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "رجوع",
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        AppHeader()
        Spacer(modifier = Modifier.height(28.dp))

        Crossfade(targetState = screen) { s ->
            Column {
                if (s == "home") {
                    AppearIn(150) {
                        BigCard(Icons.Rounded.FileUpload, "إرسال", "امسح الكود أو اكتبه يدوياً") {
                            cloneMode = false
                            screen = "send"
                            permissionLauncher.launch(requiredPermissions())
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    AppearIn(300) {
                        BigCard(Icons.Rounded.FileDownload, "استقبال", "اعرض كود عشان الجهاز التاني يتصل") {
                            cloneMode = false
                            screen = "receive"
                            permissionLauncher.launch(requiredPermissions())
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    AppearIn(450) {
                        BigCard(Icons.Rounded.ContentCopy, "استنساخ", "انقل كل بيانات الهاتف القديم للجديد") {
                            screen = "cloneRole"
                        }
                    }
                } else if (s == "cloneRole") {
                    Text(
                        text = "استنساخ",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "الهاتف ده هو...", fontSize = 14.sp, color = Ink)
                    Spacer(modifier = Modifier.height(16.dp))
                    BigCard(Icons.Rounded.PhoneAndroid, "الهاتف القديم", "هبعت منه البيانات") {
                        cloneMode = true
                        screen = "send"
                        permissionLauncher.launch(requiredPermissions())
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    BigCard(Icons.Rounded.Restore, "الهاتف الجديد", "هسترد عليه البيانات") {
                        cloneMode = true
                        screen = "receive"
                        permissionLauncher.launch(requiredPermissions())
                    }
                } else {
                    val title = when {
                        cloneMode && s == "send" -> "استنساخ: الهاتف القديم"
                        cloneMode -> "استنساخ: الهاتف الجديد"
                        s == "send" -> "إرسال"
                        else -> "استقبال"
                    }
                    Text(
                        text = title,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    AnimatedVisibility(visible = status.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SoftYellow)
                                .padding(14.dp)
                        ) {
                            Text(text = status, color = Ink, fontSize = 14.sp)
                        }
                    }

                    if (s == "receive" && !connected && qrImage != null) {
                        AppearIn(100) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(Color.White)
                                        .padding(18.dp)
                                ) {
                                    Image(
                                        bitmap = qrImage,
                                        contentDescription = "QR",
                                        modifier = Modifier.size(230.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "أو اكتب الكود ده في الجهاز التاني",
                            color = Ink,
                            fontSize = 14.sp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Ink)
                                .padding(horizontal = 22.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = token.chunked(4).joinToString("  "),
                                color = Yellow,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    if (s == "send" && !connected) {
                        if (searching) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                PulseRings()
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Button(
                            onClick = { startScan() },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("امسح الكود", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showManual = !showManual },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Keyboard,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("إدخال الكود يدوياً", color = Ink, fontWeight = FontWeight.Bold)
                        }
                        AnimatedVisibility(visible = showManual) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = manualCode,
                                    onValueChange = { input ->
                                        manualCode = input.uppercase()
                                            .filter { it.isLetterOrDigit() }
                                            .take(8)
                                    },
                                    label = { Text("الكود (8 حروف وأرقام)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(18.dp),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Characters
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { connectManual() },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Text("اتصل", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    AnimatedVisibility(
                        visible = connected,
                        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Ink)
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Yellow,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "تم الاتصال بالجهاز التاني",
                                    color = Yellow,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            if (s == "send" && !cloneMode) {
                                Button(
                                    onClick = { filePicker.launch(arrayOf("*/*")) },
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(58.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FileUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "اختار ملفات وابعت",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            if (s == "send" && cloneMode) {
                                CloneSelectScreen(
                                    selected = selected,
                                    onToggle = { key ->
                                        selected = if (key in selected) selected - key else selected + key
                                    },
                                    onStart = {
                                        status = "تم اختيار ${selected.size} أنواع. نقل البيانات الفعلي بيتفعل في الخطوات الجاية"
                                    }
                                )
                            }

                            if (s == "receive" && cloneMode) {
                                CloneRestoreScreen()
                            }
                        }
                    }

                    transfers.forEach { item ->
                        TransferRow(item)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeader() {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = "Frotta",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Text(
                text = "أبو عمر الخيامي سوفت وير",
                fontSize = 14.sp,
                color = Ink
            )
        }
    }
}

@Composable
private fun AppearIn(delayMs: Long, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 4 }
    ) {
        content()
    }
}

@Composable
private fun BigCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(28.dp))
            .background(Ink)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Yellow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                color = Yellow,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = SoftYellow,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun PulseRings() {
    val transition = rememberInfiniteTransition()
    val p by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    Canvas(modifier = Modifier.size(170.dp)) {
        val maxR = size.minDimension / 2
        for (i in 0..2) {
            val f = (p + i / 3f) % 1f
            drawCircle(
                color = Ink.copy(alpha = (1f - f) * 0.6f),
                radius = maxR * f,
                style = Stroke(width = 5f)
            )
        }
        drawCircle(color = Ink, radius = maxR * 0.2f)
    }
}

@Composable
private fun TransferRow(item: TransferItem) {
    val fraction = if (item.total > 0) {
        (item.transferred.toFloat() / item.total.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animated by animateFloatAsState(
        targetValue = if (item.done) 1f else fraction,
        animationSpec = tween(300)
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SoftYellow)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (item.sending) Icons.Rounded.FileUpload else Icons.Rounded.FileDownload,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.name,
                maxLines = 1,
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        when {
            item.failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Error,
                    contentDescription = null,
                    tint = Color(0xFFB3261E),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "فشل النقل",
                    color = Color(0xFFB3261E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            item.done -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Ink,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تم  " + formatSize(item.total),
                    color = Ink,
                    fontSize = 13.sp
                )
            }
            else -> {
                LinearProgressIndicator(
                    progress = animated,
                    color = Ink,
                    trackColor = Yellow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${(fraction * 100).toInt()}%   " +
                        formatSize(item.transferred) + " / " + formatSize(item.total),
                    color = Ink,
                    fontSize = 12.sp
                )
            }
        }
    }
}
