package com.frotta

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.Executors

private const val DOCS_PREFIX = "__frotta_docs"
private const val SEARCH_MARK = "SEARCH"

private fun isDocFolder(folder: String): Boolean {
    return folder == DOCS_PREFIX || folder.startsWith("$DOCS_PREFIX/")
}

private fun docRealFolder(folder: String): String {
    return folder.removePrefix(DOCS_PREFIX).trim('/')
}

data class TransferItem(
    val id: Long,
    val name: String,
    val sending: Boolean,
    val transferred: Long,
    val total: Long,
    val done: Boolean,
    val failed: Boolean,
    val note: String = "",
    val skipped: Boolean = false,
    val isApp: Boolean = false,
    val isDoc: Boolean = false
)

data class OutFile(
    val uri: Uri,
    val name: String,
    val folder: String,
    val mtime: Long = 0L
)

private data class SaveResult(
    val ok: Boolean,
    val note: String,
    val skipped: Boolean = false
)

fun hasAllFilesAccess(): Boolean {
    return Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()
}

fun mediaKindOf(name: String): String {
    if (name.startsWith("__frotta_")) return "data"
    return when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "dng" -> "photos"
        "mp4", "mkv", "3gp", "mov", "avi", "webm", "m4v" -> "videos"
        "mp3", "m4a", "wav", "aac", "ogg", "flac", "amr", "opus", "wma" -> "audio"
        else -> "other"
    }
}

class NearbyManager(
    context: Context,
    private val onStatus: (String) -> Unit,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit,
    private val onTransfer: (TransferItem) -> Unit,
    private val onDataFile: (String, File) -> Unit = { _, _ -> },
    private val onAppFile: (String, File) -> Unit = { _, _ -> },
    private val onDeviceFound: (String, String) -> Unit = { _, _ -> },
    private val onDeviceLost: (String) -> Unit = { _ -> },
    private val onRequest: (String, String, String) -> Unit = { _, _, _ -> },
    private val onConnectionFailed: () -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val client: ConnectionsClient = Nearby.getConnectionsClient(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val saver = Executors.newSingleThreadExecutor()
    private val checker = Executors.newFixedThreadPool(2)
    private val serviceId = "com.frotta.share"
    private val strategy = Strategy.P2P_POINT_TO_POINT
    private val maxInFlight = 3
    private var connectedEndpoint: String? = null
    private var expectedToken: String = ""
    private var myName: String = ""
    private var receiving = false
    private var browsing = false
    private var requestedEndpoint: String? = null

    private val fileIds = HashSet<Long>()
    private val outgoingIds = HashSet<Long>()
    private val fileNames = HashMap<Long, String>()
    private val fileMtimes = HashMap<Long, Long>()
    private val outSizes = HashMap<Long, Long>()
    private val incoming = HashMap<Long, Payload>()
    private val openFds = HashMap<Long, ParcelFileDescriptor>()
    private val skipIds = HashSet<Long>()
    private val finishedIn = HashSet<Long>()
    private val queue = ArrayDeque<OutFile>()
    private var inFlight = 0

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes() ?: return
                handleControl(endpointId, String(bytes, Charsets.UTF_8))
            } else if (payload.type == Payload.Type.FILE) {
                fileIds.add(payload.id)
                incoming[payload.id] = payload
            }
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate
        ) {
            val id = update.payloadId

            if (id in skipIds) {
                if (update.status != PayloadTransferUpdate.Status.IN_PROGRESS) {
                    skipIds.remove(id)
                    fileIds.remove(id)
                    incoming.remove(id)
                    fileNames.remove(id)
                    fileMtimes.remove(id)
                }
                return
            }

            if (id !in fileIds) return
            val rel = fileNames[id] ?: "ملف"
            val name = rel.substringAfterLast('/')
            val sending = id in outgoingIds
            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    onTransfer(
                        TransferItem(
                            id, name, sending,
                            update.bytesTransferred, update.totalBytes,
                            false, false
                        )
                    )
                }
                PayloadTransferUpdate.Status.SUCCESS -> {
                    if (sending) {
                        onTransfer(
                            TransferItem(
                                id, name, true,
                                update.totalBytes, update.totalBytes,
                                true, false
                            )
                        )
                        finishOutgoing(id)
                    } else {
                        saveIncoming(id)
                    }
                }
                PayloadTransferUpdate.Status.FAILURE,
                PayloadTransferUpdate.Status.CANCELED -> {
                    onTransfer(
                        TransferItem(
                            id, name, sending,
                            update.bytesTransferred, update.totalBytes,
                            false, true, "اتقطع النقل"
                        )
                    )
                    if (sending) finishOutgoing(id)
                }
                else -> {}
            }
        }
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            val suffix = info.endpointName.substringAfterLast('|')
            val shownName = info.endpointName.substringBeforeLast('|')
            val digits = info.authenticationDigits ?: ""
            if (endpointId == requestedEndpoint) {
                onStatus("الرمز: $digits (لازم يطابق الرمز اللي ظاهر عند الهاتف الآخر)")
                client.acceptConnection(endpointId, payloadCallback)
            } else if (expectedToken.isNotEmpty() && suffix == expectedToken) {
                client.acceptConnection(endpointId, payloadCallback)
            } else if (receiving && suffix == SEARCH_MARK) {
                onRequest(endpointId, shownName, digits)
            } else {
                client.rejectConnection(endpointId)
                onStatus("تم رفض اتصال غير مصرح به")
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpoint = endpointId
                requestedEndpoint = null
                browsing = false
                client.stopAdvertising()
                client.stopDiscovery()
                onStatus("تم الاتصال")
                onConnected()
            } else {
                if (endpointId == requestedEndpoint) requestedEndpoint = null
                onStatus("فشل الاتصال أو الهاتف الآخر رفض الطلب")
                onConnectionFailed()
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoint = null
            requestedEndpoint = null
            resetTransfers()
            onStatus("انقطع الاتصال")
            onDisconnected()
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (browsing) {
                onDeviceFound(endpointId, info.endpointName.substringBeforeLast('|'))
                return
            }
            val token = info.endpointName.substringAfterLast('|')
            if (expectedToken.isNotEmpty() && token == expectedToken) {
                onStatus("لقيت الهاتف، بتصل...")
                client.stopDiscovery()
                client.requestConnection("$myName|$expectedToken", endpointId, connectionCallback)
                    .addOnFailureListener {
                        onStatus("فشل الاتصال: ${it.message}")
                        onConnectionFailed()
                    }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            if (browsing) onDeviceLost(endpointId)
        }
    }

    fun startReceiving(deviceName: String, token: String) {
        expectedToken = token
        myName = deviceName
        receiving = true
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        client.startAdvertising("$deviceName|$token", serviceId, connectionCallback, options)
            .addOnSuccessListener { onStatus("جاهز. خلّي الهاتف الآخر يمسح الكود أو يدوّر عليك") }
            .addOnFailureListener { onStatus("فشل: ${it.message}") }
    }

    fun startSearching(deviceName: String, token: String) {
        expectedToken = token
        myName = deviceName
        browsing = false
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client.stopDiscovery()
        client.startDiscovery(serviceId, discoveryCallback, options)
            .addOnSuccessListener { onStatus("بدور على الهاتف...") }
            .addOnFailureListener { onStatus("فشل: ${it.message}") }
    }

    fun startBrowsing(deviceName: String) {
        expectedToken = ""
        myName = deviceName
        browsing = true
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client.stopDiscovery()
        client.startDiscovery(serviceId, discoveryCallback, options)
            .addOnSuccessListener { onStatus("بدور على أجهزة قريبة...") }
            .addOnFailureListener { onStatus("فشل البحث: ${it.message}") }
    }

    fun connectTo(endpointId: String) {
        requestedEndpoint = endpointId
        client.requestConnection("$myName|$SEARCH_MARK", endpointId, connectionCallback)
            .addOnFailureListener {
                requestedEndpoint = null
                onStatus("فشل الاتصال: ${it.message}")
                onConnectionFailed()
            }
    }

    fun acceptRequest(endpointId: String) {
        client.acceptConnection(endpointId, payloadCallback)
    }

    fun rejectRequest(endpointId: String) {
        client.rejectConnection(endpointId)
    }

    fun sendFiles(uris: List<Uri>) {
        sendQueue(uris.map { OutFile(it, "", "") })
    }

    fun sendQueue(files: List<OutFile>) {
        queue.addAll(files)
        pump()
    }

    private fun pump() {
        val endpoint = connectedEndpoint ?: return
        while (inFlight < maxInFlight && queue.isNotEmpty()) {
            val f = queue.removeFirst()
            try {
                val pfd = appContext.contentResolver.openFileDescriptor(f.uri, "r")
                if (pfd == null) continue
                val name = if (f.name.isEmpty()) queryName(f.uri) else f.name
                val payload = Payload.fromFile(pfd)
                val id = payload.id
                val size = pfd.statSize
                fileIds.add(id)
                outgoingIds.add(id)
                fileNames[id] = name
                outSizes[id] = size
                openFds[id] = pfd

                val rel = if (f.folder.isEmpty()) name else f.folder + "/" + name
                val header = "H\t$id\t$size\t${f.mtime}\t$rel"
                client.sendPayload(endpoint, Payload.fromBytes(header.toByteArray(Charsets.UTF_8)))
                client.sendPayload(endpoint, payload)
                inFlight++
                onTransfer(TransferItem(id, name, true, 0L, size, false, false))
            } catch (e: Exception) {
                onStatus("تعذر قراءة ملف")
            }
        }
    }

    private fun finishOutgoing(id: Long) {
        openFds.remove(id)?.let {
            try {
                it.close()
            } catch (e: Exception) {
            }
        }
        outgoingIds.remove(id)
        fileIds.remove(id)
        fileNames.remove(id)
        outSizes.remove(id)
        if (inFlight > 0) inFlight--
        pump()
    }

    private fun resetTransfers() {
        queue.clear()
        inFlight = 0
        openFds.values.forEach {
            try {
                it.close()
            } catch (e: Exception) {
            }
        }
        openFds.clear()
        fileIds.clear()
        outgoingIds.clear()
        fileNames.clear()
        fileMtimes.clear()
        outSizes.clear()
        incoming.clear()
        skipIds.clear()
        finishedIn.clear()
    }

    fun stopAll() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        connectedEndpoint = null
        requestedEndpoint = null
        expectedToken = ""
        receiving = false
        browsing = false
        resetTransfers()
    }

    private fun appFile(folder: String, name: String): File {
        val pkg = folder.removePrefix("__frotta_apps/").substringBefore('/')
        return File(File(File(appContext.filesDir, "apps"), pkg), name)
    }

    private fun handleControl(endpointId: String, text: String) {
        val p = text.split('\t', limit = 5)
        if (p.size < 2) return
        val id = p[1].toLongOrNull() ?: return

        when (p[0]) {
            "H" -> {
                if (p.size < 5) return
                val size = p[2].toLongOrNull() ?: -1L
                val mtime = p[3].toLongOrNull() ?: 0L
                val rel = cleanPath(p[4])
                if (rel.isEmpty()) return
                fileNames[id] = rel
                fileMtimes[id] = mtime
                val name = rel.substringAfterLast('/')
                val folder = rel.substringBeforeLast('/', "")
                if (name.startsWith("__frotta_") || size <= 0) return
                val isApp = folder.startsWith("__frotta_apps/")
                val isDoc = isDocFolder(folder)

                checker.execute {
                    var installed = false
                    var fileHave = false
                    val have = if (isApp) {
                        val pkg = folder.removePrefix("__frotta_apps/").substringBefore('/')
                        installed = AppInstaller.isInstalled(appContext, pkg)
                        val f = appFile(folder, name)
                        fileHave = f.exists() && f.length() == size
                        installed || fileHave
                    } else if (isDoc) {
                        haveDoc(name, docRealFolder(folder), size)
                    } else {
                        alreadyHave(name, folder, size)
                    }
                    if (have) {
                        mainHandler.post {
                            if (finishedIn.add(id)) {
                                skipIds.add(id)
                                incoming.remove(id)
                                client.sendPayload(
                                    endpointId,
                                    Payload.fromBytes("S\t$id".toByteArray(Charsets.UTF_8))
                                )
                                onTransfer(
                                    TransferItem(
                                        id, name, false, size, size, true, false,
                                        rel, true, isApp, isDoc
                                    )
                                )
                                if (isApp && fileHave && !installed) {
                                    val pkg = folder.removePrefix("__frotta_apps/").substringBefore('/')
                                    onAppFile(pkg, appFile(folder, name))
                                }
                            }
                        }
                    }
                }
            }
            "S" -> {
                if (id !in outgoingIds) return
                val name = fileNames[id] ?: "ملف"
                val total = outSizes[id] ?: 0L
                client.cancelPayload(id)
                finishOutgoing(id)
                onTransfer(
                    TransferItem(
                        id, name, true, total, total, true, false,
                        "موجود بالفعل عند الهاتف الآخر", true
                    )
                )
            }
        }
    }

    private fun queryName(uri: Uri): String {
        var result: String? = null
        try {
            appContext.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) result = c.getString(idx)
            }
        } catch (e: Exception) {
        }
        return result ?: uri.lastPathSegment ?: "file"
    }

    private fun cleanPath(p: String): String {
        return p.replace('\\', '/').split('/')
            .filter { it.isNotBlank() && it != "." && it != ".." }
            .joinToString("/")
    }

    private fun cleanupIncoming(id: Long) {
        incoming.remove(id)
        fileIds.remove(id)
        fileNames.remove(id)
        fileMtimes.remove(id)
    }

    private fun deleteTemp(payload: Payload) {
        try {
            val uri = payload.asFile()?.asUri() ?: return
            if (uri.scheme == "file") {
                val p = uri.path
                if (p != null) File(p).delete()
            } else {
                appContext.contentResolver.delete(uri, null, null)
            }
        } catch (e: Exception) {
        }
    }

    private fun saveIncoming(id: Long) {
        if (!finishedIn.add(id)) return
        val payload = incoming[id] ?: return
        val rel = cleanPath(fileNames[id] ?: "frotta_$id")
        val name = rel.substringAfterLast('/')
        val folder = rel.substringBeforeLast('/', "")
        val mtime = fileMtimes[id] ?: 0L
        val total = payload.asFile()?.size ?: 0L
        onTransfer(TransferItem(id, name, false, total, total, false, false))

        if (folder.startsWith("__frotta_apps/")) {
            val pkg = folder.removePrefix("__frotta_apps/").substringBefore('/')
            saver.execute {
                val target = appFile(folder, name)
                target.parentFile?.mkdirs()
                val ok = copyToFile(payload, target)
                if (ok) deleteTemp(payload)
                mainHandler.post {
                    cleanupIncoming(id)
                    onTransfer(
                        TransferItem(
                            id, name, false, total, total, ok, !ok,
                            if (ok) "" else "تعذر حفظ ملف التطبيق", false, true
                        )
                    )
                    if (ok) onAppFile(pkg, target)
                }
            }
            return
        }

        if (name.startsWith("__frotta_")) {
            val kind = name.removePrefix("__frotta_").substringBefore('.')
            saver.execute {
                val target = File(appContext.cacheDir, name)
                val ok = copyToFile(payload, target)
                if (ok) deleteTemp(payload)
                mainHandler.post {
                    cleanupIncoming(id)
                    onTransfer(TransferItem(id, name, false, total, total, ok, !ok))
                    if (ok) onDataFile(kind, target)
                }
            }
            return
        }

        val doc = isDocFolder(folder)
        val realFolder = if (doc) docRealFolder(folder) else folder

        saver.execute {
            val exists = if (total > 0) {
                if (doc) haveDoc(name, realFolder, total) else alreadyHave(name, folder, total)
            } else {
                false
            }
            val r = if (exists) {
                SaveResult(true, rel, true)
            } else {
                saveFile(payload, name, realFolder, mtime, doc)
            }
            if (r.ok) deleteTemp(payload)
            mainHandler.post {
                cleanupIncoming(id)
                onTransfer(
                    TransferItem(
                        id, name, false, total, total, r.ok, !r.ok,
                        r.note, r.skipped, false, doc
                    )
                )
                if (r.ok && !r.skipped) onStatus("تم حفظ: $name")
            }
        }
    }

    private fun copyToFile(payload: Payload, target: File): Boolean {
        val pfd = payload.asFile()?.asParcelFileDescriptor() ?: return false
        return try {
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                FileOutputStream(target).use { o -> input.copyTo(o) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun useDirect(): Boolean {
        return Build.VERSION.SDK_INT < 29 || hasAllFilesAccess()
    }

    private fun defaultTop(kind: String): String = when (kind) {
        "photos" -> "Pictures"
        "videos" -> "Movies"
        else -> "Music"
    }

    @Suppress("DEPRECATION")
    private fun docDir(folder: String): File {
        val root = Environment.getExternalStorageDirectory()
        return if (folder.isEmpty()) root else File(root, folder)
    }

    @Suppress("DEPRECATION")
    private fun directDir(kind: String, folder: String): File {
        if (kind == "docs") return docDir(folder)
        val root = Environment.getExternalStorageDirectory()
        val rel = when {
            kind == "other" ->
                if (folder.isEmpty()) "Download/Frotta" else "Download/Frotta/$folder"
            folder.isEmpty() || folder.startsWith("Android") ->
                defaultTop(kind) + "/Frotta"
            else -> folder
        }
        return File(root, rel)
    }

    @Suppress("DEPRECATION")
    private fun relPath(f: File): String {
        return f.absolutePath
            .removePrefix(Environment.getExternalStorageDirectory().absolutePath)
            .trim('/')
    }

    private fun alreadyHave(name: String, folder: String, size: Long): Boolean {
        val kind = mediaKindOf(name)
        return if (useDirect()) {
            haveDirect(directDir(kind, folder), name, size)
        } else {
            existsScoped(kind, name, folder, size)
        }
    }

    private fun haveDoc(name: String, folder: String, size: Long): Boolean {
        return if (useDirect()) {
            haveDirect(docDir(folder), name, size)
        } else {
            existsScoped("docs", name, folder, size)
        }
    }

    private fun haveDirect(dir: File, name: String, size: Long): Boolean {
        val stem = name.substringBeforeLast('.', name)
        val ext = if (name.contains('.')) "." + name.substringAfterLast('.') else ""
        var cand = File(dir, name)
        var n = 1
        while (cand.exists()) {
            if (cand.length() == size) return true
            cand = File(dir, stem + "_" + n + ext)
            n++
        }
        return false
    }

    private fun existsScoped(kind: String, name: String, folder: String, size: Long): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        val rel = targetPath(kind, folder).trim('/') + "/"
        try {
            appContext.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                arrayOf(MediaStore.MediaColumns._ID),
                MediaStore.MediaColumns.RELATIVE_PATH + "=? AND " +
                    MediaStore.MediaColumns.DISPLAY_NAME + "=? AND " +
                    MediaStore.MediaColumns.SIZE + "=?",
                arrayOf(rel, name, size.toString()),
                null
            )?.use { c ->
                return c.count > 0
            }
        } catch (e: Exception) {
        }
        return false
    }

    private fun uniqueFile(dir: File, name: String): File {
        val stem = name.substringBeforeLast('.', name)
        val ext = if (name.contains('.')) "." + name.substringAfterLast('.') else ""
        var target = File(dir, name)
        var n = 1
        while (target.exists()) {
            target = File(dir, stem + "_" + n + ext)
            n++
        }
        return target
    }

    private fun targetPath(kind: String, folder: String): String {
        val allowed = when (kind) {
            "photos" -> listOf("DCIM", "Pictures")
            "videos" -> listOf("DCIM", "Movies")
            "audio" -> listOf("Music", "Alarms", "Notifications", "Podcasts", "Ringtones", "Audiobooks")
            else -> emptyList()
        }
        if (kind == "other" || kind == "docs") {
            return if (folder.isEmpty()) "Download/Frotta" else "Download/Frotta/$folder"
        }
        val top = folder.substringBefore('/')
        return if (top in allowed) {
            folder
        } else if (folder.isEmpty()) {
            defaultTop(kind) + "/Frotta"
        } else {
            defaultTop(kind) + "/" + folder
        }
    }

    private fun saveFile(
        payload: Payload,
        name: String,
        folder: String,
        mtime: Long,
        doc: Boolean
    ): SaveResult {
        val pfd = payload.asFile()?.asParcelFileDescriptor()
            ?: return SaveResult(false, "تعذر قراءة الملف المستلم")
        val kind = if (doc) "docs" else mediaKindOf(name)
        return try {
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                if (useDirect()) {
                    saveDirect(input, name, folder, kind, mtime)
                } else {
                    saveScoped(input, name, folder, kind, mtime)
                }
            }
        } catch (e: Exception) {
            SaveResult(false, "تعذر الحفظ: " + (e.message ?: ""))
        }
    }

    private fun saveDirect(
        input: InputStream,
        name: String,
        folder: String,
        kind: String,
        mtime: Long
    ): SaveResult {
        val dir = directDir(kind, folder)
        if (!dir.exists() && !dir.mkdirs()) {
            return SaveResult(false, "تعذر إنشاء المجلد " + relPath(dir) + " (فعّل الوصول لكل الملفات)")
        }
        val target = uniqueFile(dir, name)
        val temp = File(dir, target.name + ".frottapart")
        try {
            FileOutputStream(temp).use { o -> input.copyTo(o) }
        } catch (e: Exception) {
            temp.delete()
            return SaveResult(false, "تعذر الكتابة في " + relPath(dir))
        }
        if (!temp.renameTo(target)) {
            temp.delete()
            return SaveResult(false, "تعذر إنهاء الحفظ")
        }
        if (mtime > 0) target.setLastModified(mtime)
        MediaScannerConnection.scanFile(appContext, arrayOf(target.absolutePath), null, null)
        return SaveResult(true, relPath(target))
    }

    private fun saveScoped(
        input: InputStream,
        name: String,
        folder: String,
        kind: String,
        mtime: Long
    ): SaveResult {
        val ext = name.substringAfterLast('.', "").lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (kind) {
            "photos" -> "image/jpeg"
            "videos" -> "video/mp4"
            "audio" -> "audio/mpeg"
            else -> "application/octet-stream"
        }
        val resolver = appContext.contentResolver
        val collection = when (kind) {
            "photos" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "videos" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }
        val path = targetPath(kind, folder)
        var uri: Uri? = null
        return try {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, path)
            values.put(MediaStore.MediaColumns.IS_PENDING, 1)
            if (mtime > 0 && (kind == "photos" || kind == "videos")) {
                values.put(MediaStore.MediaColumns.DATE_TAKEN, mtime)
            }
            uri = resolver.insert(collection, values)
                ?: return SaveResult(false, "تعذر إنشاء الملف في المعرض")
            val out = resolver.openOutputStream(uri)
                ?: return SaveResult(false, "تعذر فتح الملف للكتابة")
            out.use { o -> input.copyTo(o) }
            val done = ContentValues()
            done.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, done, null, null)
            SaveResult(true, "$path/$name")
        } catch (e: Exception) {
            try {
                if (uri != null) resolver.delete(uri, null, null)
            } catch (e2: Exception) {
            }
            SaveResult(false, "تعذر الحفظ: " + (e.message ?: ""))
        }
    }
}
