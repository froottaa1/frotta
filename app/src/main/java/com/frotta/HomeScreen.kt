package com.frotta

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.io.File
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

private fun mediaPermissions(): Array<String> {
    val list = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= 33) {
        list += Manifest.permission.READ_MEDIA_IMAGES
        list += Manifest.permission.READ_MEDIA_VIDEO
        list += Manifest.permission.READ_MEDIA_AUDIO
    } else {
        list += Manifest.permission.READ_EXTERNAL_STORAGE
    }
    if (Build.VERSION.SDK_INT >= 29) {
        list += Manifest.permission.ACCESS_MEDIA_LOCATION
    }
    return list.toTypedArray()
}

private fun senderDataPermissions(): Array<String> {
    val list = mutableListOf<String>()
    list.addAll(mediaPermissions())
    list += Manifest.permission.READ_CONTACTS
    list += Manifest.permission.READ_CALL_LOG
    list += Manifest.permission.READ_SMS
    list += Manifest.permission.READ_CALENDAR
    return list.toTypedArray()
}

private fun receiverDataPermissions(): Array<String> {
    val list = mutableListOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CALL_LOG,
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )
    if (Build.VERSION.SDK_INT < 29) {
        list += Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
    return list.toTypedArray()
}

private fun receivePermissions(clone: Boolean): Array<String> {
    val list = ArrayList<String>()
    list.addAll(requiredPermissions())
    if (clone) {
        list.addAll(receiverDataPermissions())
    } else if (Build.VERSION.SDK_INT < 29) {
        list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    return list.distinct().toTypedArray()
}

private fun hasAllPerms(context: Context, perms: Array<String>): Boolean {
    return perms.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
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

private fun prettyName(name: String): String {
    if (!name.startsWith("__frotta_")) return name
    val key = name.removePrefix("__frotta_").substringBefore('.')
    return cloneCategories.firstOrNull { it.key == key }?.title ?: name
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var screen by remember { mutableStateOf("home") }
    var sendStage by remember { mutableStateOf("pick") }
    var cloneMode by remember { mutableStateOf(false) }

    var selected by remember { mutableStateOf(cloneCategories.map { it.key }.toSet()) }
    var modes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var pickedItems by remember { mutableStateOf<Map<String, List<MediaItem>>>(emptyMap()) }
    var chosenFolders by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var folderKind by remember { mutableStateOf("") }
    var folderItems by remember { mutableStateOf<List<MediaItem>?>(null) }

    var pickTab by remember { mutableStateOf("photos") }
    var lists by remember { mutableStateOf<Map<String, List<MediaItem>>>(emptyMap()) }
    var sel by remember { mutableStateOf<Set<String>>(emptySet()) }
    var extraUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var permsVersion by remember { mutableStateOf(0) }
    var afterPerms by remember { mutableStateOf<(() -> Unit)?>(null) }
    var docsScan by remember { mutableStateOf<Boolean?>(null) }

    var batch by remember { mutableStateOf<List<OutFile>>(emptyList()) }
    var sending by remember { mutableStateOf(false) }
    var batchTotal by remember { mutableStateOf(0) }
    var doneCount by remember { mutableStateOf(0) }
    var failedCount by remember { mutableStateOf(0) }
    var skippedCount by remember { mutableStateOf(0) }
    var lastSaved by remember { mutableStateOf("") }
    var received by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var pendingData by remember { mutableStateOf<List<Pair<String, File>>>(emptyList()) }

    var smsPending by remember { mutableStateOf<File?>(null) }
    var smsPrev by remember { mutableStateOf<String?>(null) }
    var smsIsDefault by remember { mutableStateOf(SmsSync.isDefault(context)) }

    var status by remember { mutableStateOf("") }
    var permDenied by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    var needRestart by remember { mutableStateOf(false) }
    var allFiles by remember { mutableStateOf(hasAllFilesAccess()) }
    var searching by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }
    var showManual by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }
    val transfers = remember { mutableStateListOf<TransferItem>() }
    val myName = remember { Build.MODEL }

    fun importSmsNow(file: File) {
        status = "جاري كتابة الرسائل..."
        scope.launch {
            val n = withContext(Dispatchers.IO) { SmsSync.import(context, file) }
            if (n >= 0) {
                received = received + ("sms" to n)
                smsPending = null
                smsIsDefault = SmsSync.isDefault(context)
                status = "تم استرداد الرسائل: $n. رجّع تطبيق الرسائل الأصلي دلوقتي"
            } else {
                status = "تعذر استرداد الرسائل"
            }
        }
    }

    val smsRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val f = smsPending
        if (f != null && SmsSync.isDefault(context)) {
            importSmsNow(f)
        } else {
            status = "لازم توافق تخلي Frotta تطبيق الرسائل الافتراضي مؤقتاً عشان تكتب الرسائل"
        }
    }

    fun askSmsRole(file: File) {
        smsPending = file
        if (SmsSync.isDefault(context)) {
            importSmsNow(file)
            return
        }
        val prev = SmsSync.defaultPackage(context)
        if (prev != null && prev != context.packageName) smsPrev = prev
        val i = SmsSync.roleIntent(context)
        if (i == null) {
            status = "الهاتف ده مش بيسمح بتغيير تطبيق الرسائل من هنا"
        } else {
            try {
                smsRoleLauncher.launch(i)
            } catch (e: Exception) {
                status = "تعذر فتح نافذة اختيار تطبيق الرسائل"
            }
        }
    }

    fun runImport(kind: String, file: File) {
        if (kind == "sms") {
            askSmsRole(file)
            return
        }
        scope.launch {
            val n = withContext(Dispatchers.IO) { DataSync.import(context, kind, file) }
            val title = cloneCategories.first { it.key == kind }.title
            if (n >= 0) {
                received = received + (kind to n)
                pendingData = pendingData.filterNot { it.first == kind }
                status = "تم استرداد $title: $n"
            } else if (n == -1) {
                if (pendingData.none { it.first == kind }) {
                    pendingData = pendingData + (kind to file)
                }
                status = "اسمح بصلاحيات $title وهيتكمل الاسترداد تلقائي"
            } else if (n == -3) {
                status = "مفيش تقويم قابل للكتابة على الهاتف ده"
            } else {
                status = "تعذر استرداد $title"
            }
        }
    }

    val manager = remember {
        NearbyManager(
            context = context,
            onStatus = { status = it },
            onConnected = {
                connected = true
                searching = false
            },
            onDisconnected = {
                connected = false
                sending = false
                status = "انقطع الاتصال. أعد الاتصال وهيكمل من اللي وصل"
                needRestart = true
            },
            onTransfer = { item ->
                val i = transfers.indexOfFirst { it.id == item.id }
                if (item.done) {
                    if (item.skipped) skippedCount++
                    if (item.sending) {
                        doneCount++
                    } else if (!item.skipped) {
                        val k = mediaKindOf(item.name)
                        if (k != "data") {
                            received = received + (k to ((received[k] ?: 0) + 1))
                        }
                        if (item.note.isNotEmpty()) lastSaved = item.note
                    }
                }
                if (item.failed && item.sending) failedCount++
                if (item.done || (item.failed && item.sending)) {
                    if (i >= 0) transfers.removeAt(i)
                } else if (i >= 0) {
                    transfers[i] = item
                } else {
                    transfers.add(0, item)
                }
            },
            onDataFile = { kind, file -> runImport(kind, file) }
        )
    }

    DisposableEffect(Unit) {
        onDispose { manager.stopAll() }
    }

    fun openSettings() {
        val i = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + context.packageName)
        )
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }

    fun openDefaultApps() {
        try {
            val i = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (e: Exception) {
            openSettings()
        }
    }

    fun openAllFiles() {
        try {
            val i = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + context.packageName)
            )
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (e: Exception) {
            try {
                val i2 = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i2)
            } catch (e2: Exception) {
                openSettings()
            }
        }
    }

    fun beginReceive() {
        if (hasAllPerms(context, requiredPermissions())) {
            val t = newToken()
            token = t
            manager.startReceiving(myName, t)
        } else {
            permDenied = true
            status = "لازم توافق على صلاحيات البلوتوث والموقع عشان الاتصال"
        }
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

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permsVersion++
        val denied = result.filterValues { !it }.keys
        permDenied = denied.isNotEmpty()
        if (denied.isNotEmpty()) {
            status = "فيه ${denied.size} صلاحيات مترفضة. تقدر تفتح الإعدادات وتفعّلها"
        }
        pendingData.forEach { runImport(it.first, it.second) }
        val a = afterPerms
        afterPerms = null
        a?.invoke()
    }

    fun requestThen(perms: Array<String>, action: () -> Unit) {
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            action()
        } else {
            afterPerms = action
            permLauncher.launch(missing.toTypedArray())
        }
    }

    fun goConnect() {
        sendStage = "connect"
        requestThen(requiredPermissions()) {
            if (hasAllPerms(context, requiredPermissions())) {
                startScan()
            } else {
                permDenied = true
                status = "لازم توافق على صلاحيات البلوتوث والموقع عشان الاتصال"
            }
        }
    }

    fun confirmPick() {
        val chosen = lists.values.flatten().filter { it.uri.toString() in sel }
        val keepDates = cloneMode
        val out = ArrayList<OutFile>()
        chosen.forEach {
            out.add(OutFile(it.uri, it.name, "", if (keepDates) it.modified else 0L))
        }
        extraUris.forEach { out.add(OutFile(it, "", "")) }
        if (out.isEmpty()) {
            status = "اختار ملف واحد على الأقل"
            return
        }
        if (connected) {
            if (doneCount + failedCount >= batchTotal) {
                doneCount = 0
                failedCount = 0
                skippedCount = 0
                batchTotal = 0
            }
            batch = batch + out
            batchTotal += out.size
            sending = true
            status = "جاري الإرسال..."
            sel = emptySet()
            extraUris = emptyList()
            sendStage = "connect"
            manager.sendQueue(out)
        } else {
            batch = out
            batchTotal = out.size
            goConnect()
        }
    }

    fun confirmKindPick() {
        val kind = pickTab
        val chosen = (lists[kind] ?: emptyList()).filter { it.uri.toString() in sel }
        pickedItems = pickedItems + (kind to chosen)
        modes = modes + (kind to (if (chosen.isEmpty()) "all" else "pick"))
        sel = emptySet()
        sendStage = "cloneSelect"
    }

    fun startClone() {
        status = "جاري تجهيز الملفات..."
        scope.launch {
            val skipped = ArrayList<String>()
            val out = withContext(Dispatchers.IO) {
                val list = ArrayList<OutFile>()

                for (kind in listOf("contacts", "calls", "calendar")) {
                    if (kind !in selected) continue
                    val title = cloneCategories.first { it.key == kind }.title
                    if (!DataSync.hasPermission(context, kind, false)) {
                        skipped.add(title)
                        continue
                    }
                    val f = DataSync.export(context, kind)
                    if (f != null) {
                        list.add(OutFile(Uri.fromFile(f), f.name, ""))
                    } else {
                        skipped.add(title)
                    }
                }

                if ("sms" in selected) {
                    val f = SmsSync.export(context)
                    if (f != null) {
                        list.add(OutFile(Uri.fromFile(f), f.name, ""))
                    } else {
                        skipped.add("الرسائل")
                    }
                }

                for (kind in listOf("photos", "videos", "audio")) {
                    if (kind !in selected) continue
                    val mode = modes[kind] ?: "all"
                    if (mode == "pick") {
                        pickedItems[kind]?.forEach {
                            list.add(OutFile(it.uri, it.name, it.folder, it.modified))
                        }
                    } else if (MediaReader.hasAccess(context, kind)) {
                        val all = MediaReader.readAll(context, kind)
                        val src = if (mode == "folders") {
                            val ch = chosenFolders[kind] ?: emptySet()
                            all.filter { it.folder in ch }
                        } else {
                            all
                        }
                        src.forEach {
                            list.add(OutFile(it.uri, it.name, it.folder, it.modified))
                        }
                    }
                }
                list
            }
            val note = if (skipped.isEmpty()) {
                ""
            } else {
                "  (اتخطّى: " + skipped.joinToString("، ") + " بسبب الصلاحيات)"
            }
            if (out.isEmpty()) {
                status = "مفيش حاجة للإرسال. تأكد من الصلاحيات والاختيار" + note
            } else {
                batch = out
                batchTotal = out.size
                goConnect()
                status = "جاهز لإرسال ${out.size} ملف" + note
            }
        }
    }

    fun goHome() {
        manager.stopAll()
        transfers.clear()
        connected = false
        searching = false
        cloneMode = false
        sending = false
        batch = emptyList()
        batchTotal = 0
        doneCount = 0
        failedCount = 0
        skippedCount = 0
        lastSaved = ""
        received = emptyMap()
        pendingData = emptyList()
        smsPending = null
        modes = emptyMap()
        pickedItems = emptyMap()
        chosenFolders = emptyMap()
        folderKind = ""
        folderItems = null
        lists = emptyMap()
        docsScan = null
        sel = emptySet()
        extraUris = emptyList()
        pickTab = "photos"
        sendStage = "pick"
        permDenied = false
        token = ""
        status = ""
        showManual = false
        manualCode = ""
        screen = "home"
    }

    fun goBack() {
        if (screen == "send" && sendStage == "pick" && connected) {
            sel = emptySet()
            extraUris = emptyList()
            sendStage = "connect"
        } else if (screen == "send" && sendStage == "cloneKindPick") {
            sel = emptySet()
            sendStage = "cloneSelect"
        } else if (screen == "send" && sendStage == "connect" && !connected) {
            manager.stopAll()
            searching = false
            status = ""
            showManual = false
            batch = emptyList()
            sendStage = if (cloneMode) "cloneSelect" else "pick"
        } else if (screen == "send" && sendStage == "cloneSelect" && folderKind.isNotEmpty()) {
            folderKind = ""
        } else {
            goHome()
        }
    }

    val otherPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            extraUris = (extraUris + uris).distinct()
        }
    }

    LaunchedEffect(needRestart) {
        if (needRestart) {
            needRestart = false
            if (screen == "receive") beginReceive()
        }
    }

    LaunchedEffect(screen) {
        while (true) {
            allFiles = hasAllFilesAccess()
            smsIsDefault = SmsSync.isDefault(context)
            delay(1000)
        }
    }

    LaunchedEffect(connected) {
        if (connected && screen == "send" && batch.isNotEmpty()) {
            doneCount = 0
            failedCount = 0
            skippedCount = 0
            batchTotal = batch.size
            sending = true
            sel = emptySet()
            extraUris = emptyList()
            status = "جاري الإرسال..."
            manager.sendQueue(batch)
        }
    }

    LaunchedEffect(folderKind) {
        if (folderKind.isNotEmpty()) {
            folderItems = null
            folderItems = withContext(Dispatchers.IO) {
                MediaReader.readAll(context, folderKind)
            }
        }
    }

    LaunchedEffect(pickTab, permsVersion, screen, sendStage, allFiles) {
        val tab = pickTab
        val inPicker = screen == "send" && (sendStage == "pick" || sendStage == "cloneKindPick")
        if (inPicker) {
            if (tab == "other") {
                if (lists["other"] == null || docsScan != allFiles) {
                    val r = withContext(Dispatchers.IO) { DocsReader.readDocs(context) }
                    lists = lists + ("other" to r)
                    docsScan = allFiles
                }
            } else if (lists[tab] == null && MediaReader.hasAccess(context, tab)) {
                val r = withContext(Dispatchers.IO) {
                    MediaReader.readAll(context, tab).sortedByDescending { it.modified }
                }
                lists = lists + (tab to r)
            }
        }
    }

    val qrImage = remember(token) {
        if (token.isEmpty()) null else makeQr("FROTTA:$token").asImageBitmap()
    }

    val folders = remember(folderItems) {
        folderItems?.let { MediaReader.folders(it) }
    }

    BackHandler(enabled = screen != "home") { goBack() }

    if (screen == "send" && (sendStage == "pick" || sendStage == "cloneKindPick")) {
        val isKindPick = sendStage == "cloneKindPick"
        val access = remember(permsVersion, pickTab) {
            pickTab == "other" || MediaReader.hasAccess(context, pickTab)
        }
        PickerScreen(
            title = if (isKindPick) {
                pickerTabs.firstOrNull { it.key == pickTab }?.title ?: ""
            } else {
                "اختيار الملفات"
            },
            tabs = if (isKindPick) pickerTabs.filter { it.key == pickTab } else pickerTabs,
            currentTab = pickTab,
            onTab = { pickTab = it },
            files = lists[pickTab],
            hasAccess = access,
            selected = sel,
            totalSelected = if (isKindPick) sel.size else sel.size + extraUris.size,
            extraCount = extraUris.size,
            confirmLabel = if (isKindPick) "تم" else "إرسال",
            docsNeedAccess = pickTab == "other" && Build.VERSION.SDK_INT >= 30 && !allFiles,
            onToggle = { item ->
                val k = item.uri.toString()
                sel = if (k in sel) sel - k else sel + k
            },
            onSelectAll = { list -> sel = sel + list.map { it.uri.toString() } },
            onClear = { list -> sel = sel - list.map { it.uri.toString() }.toSet() },
            onPickOther = { otherPicker.launch(arrayOf("*/*")) },
            onOpenSettings = { openSettings() },
            onOpenAllFiles = { openAllFiles() },
            onRefresh = { permsVersion++ },
            onBack = { goBack() },
            onConfirm = { if (isKindPick) confirmKindPick() else confirmPick() }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            if (screen != "home") {
                TextButton(onClick = { goBack() }) {
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
            Spacer(modifier = Modifier.height(20.dp))

            if (smsIsDefault) {
                SmsBackCard(SmsSync.label(context, smsPrev)) { openDefaultApps() }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Crossfade(targetState = screen) { s ->
                Column {
                    if (s == "home") {
                        AppearIn(150) {
                            BigCard(Icons.Rounded.FileUpload, "إرسال", "اختار الملفات وبعدين امسح الكود") {
                                cloneMode = false
                                status = ""
                                screen = "send"
                                sendStage = "pick"
                                pickTab = "photos"
                                requestThen(requiredPermissions() + mediaPermissions()) { }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        AppearIn(300) {
                            BigCard(Icons.Rounded.FileDownload, "استقبال", "اعرض كود عشان الهاتف الآخر يتصل") {
                                cloneMode = false
                                status = ""
                                screen = "receive"
                                requestThen(receivePermissions(false)) { beginReceive() }
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
                            status = ""
                            screen = "send"
                            sendStage = "cloneSelect"
                            requestThen(requiredPermissions() + senderDataPermissions()) { }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        BigCard(Icons.Rounded.Restore, "الهاتف الجديد", "هسترد عليه البيانات") {
                            cloneMode = true
                            status = ""
                            screen = "receive"
                            requestThen(receivePermissions(true)) { beginReceive() }
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

                        if (s == "receive" && Build.VERSION.SDK_INT >= 30 && !allFiles) {
                            AllFilesCard { openAllFiles() }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (permDenied) {
                            OutlinedButton(
                                onClick = { openSettings() },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("فتح إعدادات الصلاحيات", color = Ink, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
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
                                text = "أو اكتب الكود ده في الهاتف الآخر",
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

                        if (s == "send" && sendStage == "cloneSelect") {
                            if (folderKind.isNotEmpty()) {
                                FolderPickerScreen(
                                    title = cloneCategories.first { it.key == folderKind }.title,
                                    folders = folders,
                                    chosen = chosenFolders[folderKind] ?: emptySet(),
                                    onToggle = { path ->
                                        val k = folderKind
                                        val cur = chosenFolders[k] ?: emptySet()
                                        chosenFolders = chosenFolders + (
                                            k to (if (path in cur) cur - path else cur + path)
                                        )
                                    },
                                    onDone = {
                                        val k = folderKind
                                        val any = (chosenFolders[k] ?: emptySet()).isNotEmpty()
                                        modes = modes + (k to (if (any) "folders" else "all"))
                                        folderKind = ""
                                    }
                                )
                            } else {
                                CloneSelectScreen(
                                    selected = selected,
                                    modes = modes,
                                    pickedCount = pickedItems.mapValues { it.value.size },
                                    folderCount = chosenFolders.mapValues { it.value.size },
                                    onToggle = { key ->
                                        selected = if (key in selected) selected - key else selected + key
                                    },
                                    onMode = { key, mode ->
                                        when (mode) {
                                            "all" -> modes = modes + (key to "all")
                                            "pick" -> {
                                                pickTab = key
                                                sel = pickedItems[key]
                                                    ?.map { it.uri.toString() }
                                                    ?.toSet() ?: emptySet()
                                                sendStage = "cloneKindPick"
                                            }
                                            "folders" -> {
                                                if (MediaReader.hasAccess(context, key)) {
                                                    folderKind = key
                                                } else {
                                                    status = "اسمح بالوصول للملفات الأول"
                                                    requestThen(mediaPermissions()) { permsVersion++ }
                                                }
                                            }
                                        }
                                    },
                                    onStart = { startClone() }
                                )
                            }
                        }

                        if (s == "send" && sendStage == "connect" && !connected) {
                            Text(
                                text = "جاهز لإرسال ${batch.size} ملف",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Spacer(modifier = Modifier.height(12.dp))
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
                                onClick = { goConnect() },
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
                                        text = "تم الاتصال",
                                        color = Yellow,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                if (s == "send") {
                                    if (sending) {
                                        CloneProgressCard(doneCount, failedCount, batchTotal)
                                    }
                                    if (skippedCount > 0) {
                                        Text(
                                            text = "اتخطّى $skippedCount ملف موجود بالفعل عند الهاتف الآخر",
                                            color = Ink,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            sel = emptySet()
                                            extraUris = emptyList()
                                            sendStage = "pick"
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FileUpload,
                                            contentDescription = null,
                                            tint = Ink,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            "إرسال ملفات إضافية",
                                            color = Ink,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (s == "receive") {
                                    if (cloneMode) {
                                        CloneRestoreScreen(counts = received)
                                        val pf = smsPending
                                        if (pf != null && !smsIsDefault) {
                                            OutlinedButton(
                                                onClick = { askSmsRole(pf) },
                                                shape = RoundedCornerShape(20.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                            ) {
                                                Text(
                                                    "إعادة محاولة استرداد الرسائل",
                                                    color = Ink,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                    } else {
                                        val total = received
                                            .filterKeys {
                                                it != "contacts" && it != "calls" &&
                                                    it != "calendar" && it != "sms"
                                            }
                                            .values.sum()
                                        Text(
                                            text = "استلمت $total ملف",
                                            color = Ink,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    if (skippedCount > 0) {
                                        Text(
                                            text = "تم تخطي $skippedCount ملف موجود بالفعل",
                                            color = Ink,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    if (lastSaved.isNotEmpty()) {
                                        Text(
                                            text = "آخر ملف اتحفظ في: $lastSaved",
                                            color = Ink,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
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
}

@Composable
private fun SmsBackCard(label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFB3261E))
            .padding(16.dp)
    ) {
        Text(
            text = "Frotta هو تطبيق الرسائل الافتراضي حالياً. الرسائل الجاية بتتحفظ من غير إشعار.",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Ink),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("رجّع $label", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AllFilesCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink)
            .padding(16.dp)
    ) {
        Text(
            text = "عشان الصور والفيديوهات ترجع لنفس مجلداتها بالظبط، فعّل الوصول لكل الملفات (مرة واحدة)",
            color = Yellow,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Ink),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تفعيل", fontWeight = FontWeight.Bold)
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
                text = prettyName(item.name),
                maxLines = 1,
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        when {
            item.failed -> Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                if (item.note.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.note, color = Ink, fontSize = 11.sp)
                }
            }
            item.done -> Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.skipped) {
                            "تم تخطيه (موجود بالفعل)"
                        } else {
                            "تم  " + formatSize(item.total)
                        },
                        color = Ink,
                        fontSize = 13.sp
                    )
                }
                if (!item.skipped && item.note.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.note, color = Ink, fontSize = 11.sp)
                }
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
