package com.frotta

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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

private fun notifPerms(): Array<String> {
    return if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }
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
    list.addAll(notifPerms())
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

private fun sanitizeName(s: String): String {
    val clean = s.replace("|", " ").trim().take(30)
    return if (clean.isEmpty()) Build.MODEL else clean
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

@Suppress("DEPRECATION")
private fun nearbyTempDir(): File {
    return File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        ".nearby"
    )
}

private fun dirSize(f: File): Long {
    if (!f.exists()) return 0L
    if (f.isFile) return f.length()
    var total = 0L
    f.listFiles()?.forEach { total += dirSize(it) }
    return total
}

private fun tempSize(context: Context): Long {
    return dirSize(context.cacheDir) +
        dirSize(nearbyTempDir()) +
        dirSize(File(context.filesDir, "apps"))
}

private fun prepareApkUri(context: Context): Uri? {
    return try {
        val src = File(context.applicationInfo.sourceDir)
        val dir = File(context.cacheDir, "share")
        dir.mkdirs()
        val dst = File(dir, "Frotta.apk")
        src.copyTo(dst, overwrite = true)
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", dst)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("frotta", Context.MODE_PRIVATE) }

    var screen by remember { mutableStateOf("home") }
    var sendStage by remember { mutableStateOf("pick") }
    var cloneMode by remember { mutableStateOf(false) }

    var selected by remember { mutableStateOf(cloneCategories.map { it.key }.toSet()) }
    var modes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var pickedItems by remember { mutableStateOf<Map<String, List<MediaItem>>>(emptyMap()) }
    var docExtras by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var chosenFolders by remember { mutableStateOf<Map<String, Set<String>>>(emptyMap()) }
    var folderKind by remember { mutableStateOf("") }
    var folderItems by remember { mutableStateOf<List<MediaItem>?>(null) }

    var pickTab by remember { mutableStateOf("photos") }
    var lists by remember { mutableStateOf<Map<String, List<MediaItem>>>(emptyMap()) }
    var sel by remember { mutableStateOf<Set<String>>(emptySet()) }
    var folderMarks by remember { mutableStateOf<Set<String>>(emptySet()) }
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

    var appMeta by remember { mutableStateOf<Map<String, Pair<String, Int>>>(emptyMap()) }
    var appsFiles by remember { mutableStateOf<Map<String, List<File>>>(emptyMap()) }
    var installedApps by remember { mutableStateOf<Set<String>>(emptySet()) }
    var installErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var installQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var installing by remember { mutableStateOf<String?>(null) }
    var installStart by remember { mutableStateOf(0L) }
    var canInstallApps by remember { mutableStateOf(AppInstaller.canInstall(context)) }

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

    var myName by remember {
        mutableStateOf(sanitizeName(prefs.getString("device_name", null) ?: Build.MODEL))
    }
    var confirmIncoming by remember { mutableStateOf(prefs.getBoolean("confirm_receive", true)) }
    val found = remember { mutableStateListOf<Pair<String, String>>() }
    var connectingTo by remember { mutableStateOf<String?>(null) }
    var requestFrom by remember { mutableStateOf<Triple<String, String, String>?>(null) }
    var cacheSizeText by remember { mutableStateOf("") }
    val appVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    var history by remember { mutableStateOf(HistoryStore.load(prefs)) }
    var peerName by remember { mutableStateOf("") }
    var histFiles by remember { mutableStateOf(0) }
    var histFailed by remember { mutableStateOf(0) }
    var histSkipped by remember { mutableStateOf(0) }
    var histBytes by remember { mutableStateOf(0L) }

    fun finishSession() {
        if (histFiles + histFailed + histSkipped > 0) {
            val kind = when {
                screen == "send" && cloneMode -> "clone_send"
                screen == "send" -> "send"
                screen == "receive" && cloneMode -> "clone_receive"
                else -> "receive"
            }
            history = HistoryStore.add(
                prefs,
                HistoryEntry(
                    System.currentTimeMillis(),
                    kind,
                    peerName.ifEmpty { "هاتف آخر" },
                    histFiles,
                    histFailed,
                    histSkipped,
                    histBytes
                )
            )
        }
        histFiles = 0
        histFailed = 0
        histSkipped = 0
        histBytes = 0L
        peerName = ""
    }

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
        if (kind == "apps") {
            scope.launch {
                val m = withContext(Dispatchers.IO) { AppsReader.parseMeta(file) }
                appMeta = appMeta + m
                received = received + ("apps" to appMeta.size)
                status = "وصلت قايمة التطبيقات: ${m.size}"
            }
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
                connectingTo = null
                requestFrom = null
                found.clear()
            },
            onDisconnected = {
                connected = false
                sending = false
                finishSession()
                status = "انقطع الاتصال. أعد الاتصال وهيكمل من اللي وصل"
                needRestart = true
            },
            onTransfer = { item ->
                val i = transfers.indexOfFirst { it.id == item.id }
                if (item.done) {
                    if (item.skipped) {
                        skippedCount++
                        histSkipped++
                    } else {
                        histFiles++
                        histBytes += item.total
                    }
                    if (item.sending) {
                        doneCount++
                    } else if (!item.skipped) {
                        val k = if (item.isDoc) "docs" else mediaKindOf(item.name)
                        if (k != "data" && !item.isApp) {
                            received = received + (k to ((received[k] ?: 0) + 1))
                        }
                        if (item.note.isNotEmpty()) lastSaved = item.note
                    }
                }
                if (item.failed) histFailed++
                if (item.failed && item.sending) failedCount++
                if (item.done || (item.failed && item.sending)) {
                    if (i >= 0) transfers.removeAt(i)
                } else if (i >= 0) {
                    transfers[i] = item
                } else {
                    transfers.add(0, item)
                }
            },
            onDataFile = { kind, file -> runImport(kind, file) },
            onAppFile = { pkg, file ->
                val cur = appsFiles[pkg] ?: emptyList()
                if (cur.none { it.name == file.name }) {
                    appsFiles = appsFiles + (pkg to (cur + file))
                }
            },
            onDeviceFound = { id, name ->
                if (found.none { it.first == id }) found.add(id to name)
            },
            onDeviceLost = { id -> found.removeAll { it.first == id } },
            onRequest = { id, name, digits -> requestFrom = Triple(id, name, digits) },
            onConnectionFailed = { connectingTo = null },
            onPeer = { peerName = it }
        )
    }

    SideEffect { manager.confirmIncoming = confirmIncoming }

    DisposableEffect(Unit) {
        onDispose {
            manager.stopAll()
            KeepAlive.stop(context)
        }
    }

    fun openSettings() {
        val i = Intent(
            AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + context.packageName)
        )
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }

    fun openDefaultApps() {
        try {
            val i = Intent(AndroidSettings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (e: Exception) {
            openSettings()
        }
    }

    fun openBatterySettings() {
        try {
            val i = Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (e: Exception) {
            openSettings()
        }
    }

    fun openUnknownSources() {
        try {
            val i = Intent(
                AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + context.packageName)
            )
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (e: Exception) {
            openSettings()
        }
    }

    fun openAllFiles() {
        try {
            val i = Intent(
                AndroidSettings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + context.packageName)
            )
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (e: Exception) {
            try {
                val i2 = Intent(AndroidSettings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(i2)
            } catch (e2: Exception) {
                openSettings()
            }
        }
    }

    fun openFacebook() {
        val url = AppInfo.FACEBOOK_URL
        if (url.isBlank()) {
            status = "رابط الصفحة لسه مش متضاف"
            return
        }
        try {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (e: Exception) {
            status = "تعذر فتح الرابط"
        }
    }

    fun openAppSettingsScreen() {
        status = ""
        cacheSizeText = ""
        screen = "settings"
    }

    fun shareApp() {
        scope.launch {
            val uri = withContext(Dispatchers.IO) { prepareApkUri(context) }
            if (uri == null) {
                status = "تعذر تجهيز ملف التطبيق"
            } else {
                try {
                    val i = Intent(Intent.ACTION_SEND)
                    i.type = "application/vnd.android.package-archive"
                    i.putExtra(Intent.EXTRA_STREAM, uri)
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val chooser = Intent.createChooser(i, "مشاركة Frotta")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                    status = "اختار بلوتوث أو Quick Share من القايمة"
                } catch (e: Exception) {
                    status = "تعذر فتح قايمة المشاركة"
                }
            }
        }
    }

    fun clearTemp() {
        scope.launch {
            withContext(Dispatchers.IO) {
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                nearbyTempDir().deleteRecursively()
                File(context.filesDir, "apps").deleteRecursively()
            }
            appsFiles = emptyMap()
            cacheSizeText = formatSize(withContext(Dispatchers.IO) { tempSize(context) })
            status = "تم مسح الملفات المؤقتة"
        }
    }

    fun startInstall(pkg: String) {
        val files = appsFiles[pkg] ?: emptyList()
        val need = appMeta[pkg]?.second ?: 0
        if (files.isEmpty() || files.size < need) {
            installErrors = installErrors + (pkg to "لسه مستلمش كل ملفات التطبيق")
            return
        }
        installErrors = installErrors - pkg
        installing = pkg
        installStart = System.currentTimeMillis()
        scope.launch {
            val err = withContext(Dispatchers.IO) { AppInstaller.install(context, pkg, files) }
            if (err != null) {
                installErrors = installErrors + (pkg to err)
                installing = null
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

    fun beginBrowse() {
        if (hasAllPerms(context, requiredPermissions())) {
            found.clear()
            connectingTo = null
            searching = true
            manager.startBrowsing(myName)
        } else {
            permDenied = true
            status = "لازم توافق على صلاحيات البلوتوث والموقع عشان الاتصال"
        }
    }

    fun connectToDevice(id: String) {
        connectingTo = id
        status = "بطلب الاتصال..."
        manager.connectTo(id)
    }

    fun startScan() {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { barcode ->
                val raw = barcode.rawValue ?: ""
                if (raw.startsWith("FROTTA:")) {
                    found.clear()
                    connectingTo = null
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
            found.clear()
            connectingTo = null
            searching = true
            manager.startSearching(myName, code)
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permsVersion++
        val denied = result.filterValues { !it }.keys
            .filter { it != Manifest.permission.POST_NOTIFICATIONS }
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
        requestThen(requiredPermissions()) { beginBrowse() }
    }

    fun finishPick(out: List<OutFile>) {
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
            folderMarks = emptySet()
            extraUris = emptyList()
            sendStage = "connect"
            manager.sendQueue(out)
        } else {
            batch = out
            batchTotal = out.size
            goConnect()
        }
    }

    fun confirmPick() {
        val out = ArrayList<OutFile>()
        lists.forEach { (tab, items) ->
            if (tab == "apps") return@forEach
            items.filter { it.uri.toString() in sel }.forEach { m ->
                val mark = "$tab|${m.folder}"
                val folderName = if (!cloneMode && mark in folderMarks) {
                    val label = m.folder.substringAfterLast('/').ifEmpty { "Root" }
                    when (tab) {
                        "photos" -> "Pictures/Frotta/$label"
                        "videos" -> "Movies/Frotta/$label"
                        "audio" -> "Music/Frotta/$label"
                        else -> ""
                    }
                } else {
                    ""
                }
                out.add(OutFile(m.uri, m.name, folderName, 0L))
            }
        }
        extraUris.forEach { out.add(OutFile(it, "", "")) }

        val appKeys = (lists["apps"] ?: emptyList())
            .filter { it.uri.toString() in sel }
            .map { it.folder }
            .toSet()

        if (appKeys.isEmpty()) {
            finishPick(out)
        } else {
            status = "جاري تجهيز التطبيقات..."
            scope.launch {
                val appOut = withContext(Dispatchers.IO) {
                    val chosen = AppsReader.readApps(context).filter { it.pkg in appKeys }
                    val list = ArrayList<OutFile>()
                    if (chosen.isNotEmpty()) {
                        val meta = AppsReader.exportMeta(context, chosen)
                        if (meta != null) {
                            list.add(OutFile(Uri.fromFile(meta), meta.name, ""))
                            list.addAll(AppsReader.outFiles(chosen))
                        }
                    }
                    list
                }
                finishPick(out + appOut)
            }
        }
    }

    fun confirmKindPick() {
        val tab = pickTab
        val kind = if (tab == "other") "docs" else tab
        val chosen = (lists[tab] ?: emptyList()).filter { it.uri.toString() in sel }
        pickedItems = pickedItems + (kind to chosen)
        if (kind == "docs") docExtras = extraUris
        val hasAny = chosen.isNotEmpty() || (kind == "docs" && extraUris.isNotEmpty())
        modes = modes + (kind to (if (hasAny) "pick" else "all"))
        sel = emptySet()
        folderMarks = emptySet()
        extraUris = emptyList()
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

                if ("apps" in selected) {
                    val all = AppsReader.readApps(context)
                    val chosen = if ((modes["apps"] ?: "all") == "pick") {
                        val keys = (pickedItems["apps"] ?: emptyList()).map { it.folder }.toSet()
                        all.filter { it.pkg in keys }
                    } else {
                        all
                    }
                    if (chosen.isEmpty()) {
                        skipped.add("التطبيقات")
                    } else {
                        val meta = AppsReader.exportMeta(context, chosen)
                        if (meta != null) {
                            list.add(OutFile(Uri.fromFile(meta), meta.name, ""))
                            list.addAll(AppsReader.outFiles(chosen))
                        } else {
                            skipped.add("التطبيقات")
                        }
                    }
                }

                if ("docs" in selected) {
                    val mode = modes["docs"] ?: "all"
                    val docs = ArrayList<OutFile>()
                    if (mode == "pick") {
                        pickedItems["docs"]?.forEach {
                            docs.add(
                                OutFile(it.uri, it.name, "__frotta_docs/" + it.folder, it.modified)
                            )
                        }
                        docExtras.forEach { docs.add(OutFile(it, "", "")) }
                    } else {
                        val all = DocsReader.readDocs(context, true)
                        val src = if (mode == "folders") {
                            val ch = chosenFolders["docs"] ?: emptySet()
                            all.filter { it.folder in ch }
                        } else {
                            all
                        }
                        src.forEach {
                            docs.add(
                                OutFile(it.uri, it.name, "__frotta_docs/" + it.folder, it.modified)
                            )
                        }
                    }
                    if (docs.isEmpty()) {
                        skipped.add("المستندات (فعّل الوصول لكل الملفات)")
                    } else {
                        list.addAll(docs)
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
                "  (اتخطّى: " + skipped.joinToString("، ") + ")"
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
        finishSession()
        manager.stopAll()
        transfers.clear()
        found.clear()
        connectingTo = null
        requestFrom = null
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
        appMeta = emptyMap()
        appsFiles = emptyMap()
        installQueue = emptyList()
        installErrors = emptyMap()
        installing = null
        modes = emptyMap()
        pickedItems = emptyMap()
        docExtras = emptyList()
        chosenFolders = emptyMap()
        folderKind = ""
        folderItems = null
        lists = emptyMap()
        docsScan = null
        sel = emptySet()
        folderMarks = emptySet()
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
            folderMarks = emptySet()
            extraUris = emptyList()
            sendStage = "connect"
        } else if (screen == "send" && sendStage == "cloneKindPick") {
            sel = emptySet()
            folderMarks = emptySet()
            extraUris = emptyList()
            sendStage = "cloneSelect"
        } else if (screen == "send" && sendStage == "connect" && !connected) {
            manager.stopAll()
            searching = false
            found.clear()
            connectingTo = null
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
            if (screen == "receive") {
                beginReceive()
            } else if (screen == "send" && sendStage == "connect") {
                beginBrowse()
            }
        }
    }

    LaunchedEffect(screen) {
        while (true) {
            allFiles = hasAllFilesAccess()
            smsIsDefault = SmsSync.isDefault(context)
            delay(1000)
        }
    }

    LaunchedEffect(screen) {
        if (screen == "settings") {
            cacheSizeText = formatSize(withContext(Dispatchers.IO) { tempSize(context) })
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            canInstallApps = AppInstaller.canInstall(context)
            val pkgs = appMeta.keys + appsFiles.keys
            if (pkgs.isNotEmpty()) {
                val now = pkgs.filter { AppInstaller.isInstalled(context, it) }.toSet()
                installedApps = now

                val toClean = now.filter { appsFiles.containsKey(it) }
                if (toClean.isNotEmpty()) {
                    withContext(Dispatchers.IO) {
                        toClean.forEach {
                            File(File(context.filesDir, "apps"), it).deleteRecursively()
                        }
                    }
                    appsFiles = appsFiles - toClean.toSet()
                }

                val cur = installing
                if (cur != null) {
                    val r = InstallLog.take(cur)
                    if (cur in now) {
                        installing = null
                    } else if (r != null && !r.ok) {
                        installErrors = installErrors + (cur to r.message)
                        installing = null
                    } else if (System.currentTimeMillis() - installStart > 180000L) {
                        installErrors = installErrors + (cur to "انتهت مهلة التثبيت")
                        installing = null
                    }
                }

                if (installing == null && installQueue.isNotEmpty()) {
                    val next = installQueue.first()
                    installQueue = installQueue.drop(1)
                    if (next !in now) startInstall(next)
                }
            }
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
            folderMarks = emptySet()
            extraUris = emptyList()
            status = "جاري الإرسال..."
            manager.sendQueue(batch)
        }
    }

    LaunchedEffect(folderKind) {
        if (folderKind.isNotEmpty()) {
            folderItems = null
            folderItems = withContext(Dispatchers.IO) {
                if (folderKind == "docs") {
                    DocsReader.readDocs(context, true)
                } else {
                    MediaReader.readAll(context, folderKind)
                }
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
            } else if (tab == "apps") {
                if (lists["apps"] == null) {
                    val r = withContext(Dispatchers.IO) {
                        AppsReader.toItems(AppsReader.readApps(context))
                    }
                    lists = lists + ("apps" to r)
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

    val appRows = remember(appMeta, appsFiles, installedApps, installErrors, installing) {
        appMeta.map { (pkg, v) ->
            AppRow(
                pkg = pkg,
                label = v.first,
                ready = v.second > 0 && (appsFiles[pkg]?.size ?: 0) >= v.second,
                installed = pkg in installedApps,
                error = installErrors[pkg] ?: "",
                installing = installing == pkg
            )
        }.sortedBy { it.label.lowercase() }
    }

    val view = LocalView.current
    val busy = connected &&
        (transfers.isNotEmpty() || (sending && doneCount + failedCount < batchTotal))
    DisposableEffect(busy) {
        view.keepScreenOn = busy
        onDispose { view.keepScreenOn = false }
    }

    val notifText = if (screen == "send") {
        if (batchTotal > 0) {
            "جاري الإرسال: ${doneCount + failedCount} / $batchTotal"
        } else {
            "متصل بالهاتف الآخر"
        }
    } else {
        "جاري الاستلام: ${received.values.sum()}"
    }
    val notifLatest by rememberUpdatedState(notifText)
    LaunchedEffect(connected) {
        if (connected) {
            KeepAlive.start(context, notifLatest)
            while (true) {
                delay(3000)
                KeepAlive.update(context, notifLatest)
            }
        } else {
            KeepAlive.stop(context)
        }
    }

    BackHandler(enabled = screen != "home") { goBack() }

    val rf = requestFrom
    if (rf != null) {
        AlertDialog(
            onDismissRequest = {
                manager.rejectRequest(rf.first)
                requestFrom = null
            },
            title = { Text("طلب اتصال", fontWeight = FontWeight.Bold) },
            text = {
                Text("«${rf.second}» عايز يتصل بيك.\nتأكد إن الرمز ${rf.third} هو نفس الرمز الظاهر عنده.")
            },
            confirmButton = {
                TextButton(onClick = {
                    manager.acceptRequest(rf.first)
                    requestFrom = null
                }) {
                    Text("قبول", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    manager.rejectRequest(rf.first)
                    requestFrom = null
                }) {
                    Text("رفض")
                }
            }
        )
    }

    if (screen == "send" && (sendStage == "pick" || sendStage == "cloneKindPick")) {
        val isKindPick = sendStage == "cloneKindPick"
        val access = remember(permsVersion, pickTab) {
            pickTab == "other" || pickTab == "apps" || MediaReader.hasAccess(context, pickTab)
        }
        PickerScreen(
            title = if (isKindPick) {
                if (pickTab == "other") {
                    "المستندات"
                } else {
                    allPickerTabs.firstOrNull { it.key == pickTab }?.title ?: ""
                }
            } else {
                "اختيار الملفات"
            },
            tabs = if (isKindPick) allPickerTabs.filter { it.key == pickTab } else allPickerTabs,
            currentTab = pickTab,
            onTab = { pickTab = it },
            files = lists[pickTab],
            hasAccess = access,
            selected = sel,
            totalSelected = if (isKindPick) {
                sel.size + (if (pickTab == "other") extraUris.size else 0)
            } else {
                sel.size + extraUris.size
            },
            extraCount = extraUris.size,
            confirmLabel = if (isKindPick) "تم" else "إرسال",
            docsNeedAccess = pickTab == "other" && Build.VERSION.SDK_INT >= 30 && !allFiles,
            onToggle = { item ->
                val k = item.uri.toString()
                sel = if (k in sel) sel - k else sel + k
            },
            onSelectAll = { list -> sel = sel + list.map { it.uri.toString() } },
            onClear = { list -> sel = sel - list.map { it.uri.toString() }.toSet() },
            onFolderToggle = { path, items, on ->
                val keys = items.map { it.uri.toString() }
                sel = if (on) sel + keys else sel - keys.toSet()
                val mk = "$pickTab|$path"
                folderMarks = if (on) folderMarks + mk else folderMarks - mk
            },
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
            Spacer(modifier = Modifier.height(4.dp))

            AppHeader(
                isHome = screen == "home",
                onHistory = {
                    status = ""
                    screen = "history"
                },
                onSettings = { openAppSettingsScreen() },
                onBack = { goBack() }
            )
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                SquareCard(
                                    Icons.Rounded.FileUpload,
                                    "إرسال",
                                    "اختار الملفات ثم الهاتف",
                                    Modifier.weight(1f)
                                ) {
                                    cloneMode = false
                                    status = ""
                                    screen = "send"
                                    sendStage = "pick"
                                    pickTab = "photos"
                                    requestThen(
                                        requiredPermissions() + mediaPermissions() + notifPerms()
                                    ) { }
                                }
                                SquareCard(
                                    Icons.Rounded.FileDownload,
                                    "استقبال",
                                    "خلّي هاتفك ظاهر للهاتف الآخر",
                                    Modifier.weight(1f)
                                ) {
                                    cloneMode = false
                                    status = ""
                                    screen = "receive"
                                    requestThen(receivePermissions(false)) { beginReceive() }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        AppearIn(300) {
                            BigCard(
                                Icons.Rounded.ContentCopy,
                                "استنساخ",
                                "انقل كل بيانات الهاتف القديم للجديد"
                            ) {
                                screen = "cloneRole"
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        AppearIn(450) {
                            RecentSection(history) {
                                status = ""
                                screen = "history"
                            }
                        }
                    } else if (s == "history") {
                        HistoryScreen(history) {
                            HistoryStore.clear(prefs)
                            history = emptyList()
                        }
                    } else if (s == "settings") {
                        SettingsScreen(
                            deviceName = myName,
                            version = appVersion,
                            cacheSize = cacheSizeText,
                            message = status,
                            confirm = confirmIncoming,
                            onConfirm = { v ->
                                confirmIncoming = v
                                prefs.edit().putBoolean("confirm_receive", v).apply()
                            },
                            onSaveName = { n ->
                                val clean = sanitizeName(n)
                                prefs.edit().putString("device_name", clean).apply()
                                myName = clean
                                status = "تم حفظ الاسم: $clean"
                            },
                            onBattery = { openBatterySettings() },
                            onShare = { shareApp() },
                            onClear = { clearTemp() }
                        )
                        DeveloperCard(
                            hasLink = AppInfo.FACEBOOK_URL.isNotBlank(),
                            onFacebook = { openFacebook() }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
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
                            requestThen(
                                requiredPermissions() + senderDataPermissions() + notifPerms()
                            ) { }
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
                            AllFilesCard(
                                "عشان الصور والفيديوهات والمستندات ترجع لنفس مجلداتها بالظبط، فعّل الوصول لكل الملفات (مرة واحدة)"
                            ) { openAllFiles() }
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
                            ReceiverWaiting(
                                qr = qrImage,
                                deviceName = myName,
                                code = token
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        if (s == "send" && sendStage == "cloneSelect") {
                            if ("docs" in selected && Build.VERSION.SDK_INT >= 30 && !allFiles) {
                                AllFilesCard(
                                    "عشان يجمع كل المستندات (PDF وWord وExcel وDWG...) من الهاتف، فعّل الوصول لكل الملفات (مرة واحدة)"
                                ) { openAllFiles() }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

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
                                    pickedCount = pickedItems.mapValues {
                                        it.value.size + (if (it.key == "docs") docExtras.size else 0)
                                    },
                                    folderCount = chosenFolders.mapValues { it.value.size },
                                    onToggle = { key ->
                                        selected = if (key in selected) selected - key else selected + key
                                    },
                                    onMode = { key, mode ->
                                        when (mode) {
                                            "all" -> modes = modes + (key to "all")
                                            "pick" -> {
                                                pickTab = if (key == "docs") "other" else key
                                                sel = pickedItems[key]
                                                    ?.map { it.uri.toString() }
                                                    ?.toSet() ?: emptySet()
                                                if (key == "docs") extraUris = docExtras
                                                sendStage = "cloneKindPick"
                                            }
                                            "folders" -> {
                                                val ok = if (key == "docs") {
                                                    Build.VERSION.SDK_INT < 30 || allFiles
                                                } else {
                                                    MediaReader.hasAccess(context, key)
                                                }
                                                if (ok) {
                                                    folderKind = key
                                                } else if (key == "docs") {
                                                    status = "فعّل الوصول لكل الملفات الأول"
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
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                RadarView(diameter = 240.dp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (searching) "جاري البحث عن أجهزة قريبة..." else "البحث متوقف",
                                color = Ink,
                                fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            TextButton(
                                onClick = { goConnect() },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("إعادة البحث", color = Ink, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            found.forEach { d ->
                                DeviceCard(
                                    name = d.second,
                                    connecting = connectingTo == d.first
                                ) { connectToDevice(d.first) }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            OptionCard(
                                Icons.Rounded.QrCodeScanner,
                                "مسح رمز QR",
                                "امسح الرمز اللي على شاشة الهاتف المستقبِل"
                            ) { startScan() }
                            Spacer(modifier = Modifier.height(10.dp))
                            OptionCard(
                                Icons.Rounded.Keyboard,
                                "إدخال الكود يدوياً",
                                "لو الكاميرا مش شغالة"
                            ) { showManual = !showManual }
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
                                            folderMarks = emptySet()
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
                                                    it != "calendar" && it != "sms" &&
                                                    it != "apps" && it != "docs"
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

                        if (s == "receive" && appRows.isNotEmpty()) {
                            AppsInstallCard(
                                rows = appRows,
                                canInstall = canInstallApps,
                                onAllowInstall = { openUnknownSources() },
                                onInstallAll = {
                                    installQueue = appRows
                                        .filter { it.ready && !it.installed && !it.installing }
                                        .map { it.pkg }
                                },
                                onInstallOne = { pkg ->
                                    if (installing == null) {
                                        startInstall(pkg)
                                    } else if (pkg !in installQueue) {
                                        installQueue = installQueue + pkg
                                    }
                                }
                            )
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
private fun AllFilesCard(text: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink)
            .padding(16.dp)
    ) {
        Text(
            text = text,
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
private fun HeaderButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Ink)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Yellow,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun AppHeader(
    isHome: Boolean,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(20.dp))
                .background(Ink)
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(17.dp))
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "Frotta",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        if (isHome) {
            HeaderButton(Icons.Rounded.History, onHistory)
            Spacer(modifier = Modifier.width(8.dp))
            HeaderButton(Icons.Rounded.Settings, onSettings)
        } else {
            HeaderButton(Icons.Rounded.ArrowForward, onBack)
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
