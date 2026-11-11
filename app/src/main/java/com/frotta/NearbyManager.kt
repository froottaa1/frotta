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

data class TransferItem(
    val id: Long,
    val name: String,
    val sending: Boolean,
    val transferred: Long,
    val total: Long,
    val done: Boolean,
    val failed: Boolean
)

data class OutFile(
    val uri: Uri,
    val name: String,
    val folder: String
)

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
    private val onDataFile: (String, File) -> Unit = { _, _ -> }
) {
    private val appContext = context.applicationContext
    private val client: ConnectionsClient = Nearby.getConnectionsClient(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val saver = Executors.newSingleThreadExecutor()
    private val serviceId = "com.frotta.share"
    private val strategy = Strategy.P2P_POINT_TO_POINT
    private val maxInFlight = 3
    private var connectedEndpoint: String? = null
    private var expectedToken: String = ""
    private var myName: String = ""

    private val fileIds = HashSet<Long>()
    private val outgoingIds = HashSet<Long>()
    private val fileNames = HashMap<Long, String>()
    private val incoming = HashMap<Long, Payload>()
    private val openFds = HashMap<Long, ParcelFileDescriptor>()
    private val queue = ArrayDeque<OutFile>()
    private var inFlight = 0

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val text = String(payload.asBytes() ?: return, Charsets.UTF_8)
                val idx = text.indexOf(':')
                if (idx > 0) {
                    val id = text.substring(0, idx).toLongOrNull()
                    if (id != null) fileNames[id] = text.substring(idx + 1)
                }
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
                            false, true
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
            val token = info.endpointName.substringAfterLast('|')
            if (expectedToken.isNotEmpty() && token == expectedToken) {
                client.acceptConnection(endpointId, payloadCallback)
            } else {
                client.rejectConnection(endpointId)
                onStatus("تم رفض جهاز غير مصرح له")
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpoint = endpointId
                client.stopAdvertising()
                client.stopDiscovery()
                onStatus("تم الاتصال")
                onConnected()
            } else {
                onStatus("فشل الاتصال")
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoint = null
            queue.clear()
            inFlight = 0
            onStatus("انقطع الاتصال")
            onDisconnected()
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val token = info.endpointName.substringAfterLast('|')
            if (expectedToken.isNotEmpty() && token == expectedToken) {
                onStatus("لقيت الجهاز، بتصل...")
                client.stopDiscovery()
                client.requestConnection("$myName|$expectedToken", endpointId, connectionCallback)
                    .addOnFailureListener { onStatus("فشل الاتصال: ${it.message}") }
            }
        }

        override fun onEndpointLost(endpointId: String) {}
    }

    fun startReceiving(deviceName: String, token: String) {
        expectedToken = token
        myName = deviceName
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        client.startAdvertising("$deviceName|$token", serviceId, connectionCallback, options)
            .addOnSuccessListener { onStatus("جاهز. خلّي الجهاز التاني يمسح الكود") }
            .addOnFailureListener { onStatus("فشل: ${it.message}") }
    }

    fun startSearching(deviceName: String, token: String) {
        expectedToken = token
        myName = deviceName
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client.startDiscovery(serviceId, discoveryCallback, options)
            .addOnSuccessListener { onStatus("بدور على الجهاز...") }
            .addOnFailureListener { onStatus("فشل: ${it.message}") }
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
                fileIds.add(id)
                outgoingIds.add(id)
                fileNames[id] = name
                openFds[id] = pfd

                val rel = if (f.folder.isEmpty()) name else f.folder + "/" + name
                client.sendPayload(
                    endpoint,
                    Payload.fromBytes("$id:$rel".toByteArray(Charsets.UTF_8))
                )
                client.sendPayload(endpoint, payload)
                inFlight++
                onTransfer(TransferItem(id, name, true, 0L, pfd.statSize, false, false))
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
        if (inFlight > 0) inFlight--
        pump()
    }

    fun stopAll() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        connectedEndpoint = null
        expectedToken = ""
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
        incoming.clear()
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

    private fun saveIncoming(id: Long) {
        val payload = incoming[id] ?: return
        val rel = cleanPath(fileNames[id] ?: "frotta_$id")
        val name = rel.substringAfterLast('/')
        val folder = rel.substringBeforeLast('/', "")
        val total = payload.asFile()?.size ?: 0L
        onTransfer(TransferItem(id, name, false, total, total, false, false))

        if (name.startsWith("__frotta_")) {
            val kind = name.removePrefix("__frotta_").substringBefore('.')
            saver.execute {
                val target = File(appContext.cacheDir, name)
                val ok = copyToFile(payload, target)
                mainHandler.post {
                    incoming.remove(id)
                    fileIds.remove(id)
                    fileNames.remove(id)
                    onTransfer(TransferItem(id, name, false, total, total, ok, !ok))
                    if (ok) onDataFile(kind, target)
                }
            }
            return
        }

        saver.execute {
            val ok = saveFile(payload, name, folder)
            mainHandler.post {
                incoming.remove(id)
                fileIds.remove(id)
                fileNames.remove(id)
                onTransfer(TransferItem(id, name, false, total, total, ok, !ok))
                if (ok) onStatus("تم حفظ: $name")
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

    private fun targetPath(kind: String, folder: String): String {
        val allowed = when (kind) {
            "photos" -> listOf("DCIM", "Pictures")
            "videos" -> listOf("DCIM", "Movies")
            "audio" -> listOf("Music", "Alarms", "Notifications", "Podcasts", "Ringtones", "Audiobooks")
            else -> emptyList()
        }
        if (kind == "other") {
            return if (folder.isEmpty()) "Download/Frotta" else "Download/Frotta/$folder"
        }
        val default = when (kind) {
            "photos" -> "Pictures"
            "videos" -> "Movies"
            else -> "Music"
        }
        val top = folder.substringBefore('/')
        return if (top in allowed) {
            folder
        } else if (folder.isEmpty()) {
            "$default/Frotta"
        } else {
            "$default/$folder"
        }
    }

    private fun saveFile(payload: Payload, name: String, folder: String): Boolean {
        val pfd = payload.asFile()?.asParcelFileDescriptor() ?: return false
        val kind = mediaKindOf(name)
        val ext = name.substringAfterLast('.', "").lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: when (kind) {
            "photos" -> "image/jpeg"
            "videos" -> "video/mp4"
            "audio" -> "audio/mpeg"
            else -> "application/octet-stream"
        }
        return try {
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                if (Build.VERSION.SDK_INT >= 29) {
                    saveScoped(input, name, mime, kind, folder)
                } else {
                    saveLegacy(input, name, kind, folder)
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun saveScoped(
        input: InputStream,
        name: String,
        mime: String,
        kind: String,
        folder: String
    ): Boolean {
        val resolver = appContext.contentResolver
        val collection = when (kind) {
            "photos" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "videos" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }
        var uri: Uri? = null
        return try {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime)
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, targetPath(kind, folder))
            values.put(MediaStore.MediaColumns.IS_PENDING, 1)
            uri = resolver.insert(collection, values) ?: return false
            val out = resolver.openOutputStream(uri) ?: return false
            out.use { o -> input.copyTo(o) }
            val done = ContentValues()
            done.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, done, null, null)
            true
        } catch (e: Exception) {
            try {
                if (uri != null) resolver.delete(uri, null, null)
            } catch (e2: Exception) {
            }
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(
        input: InputStream,
        name: String,
        kind: String,
        folder: String
    ): Boolean {
        val path = targetPath(kind, folder)
        val top = path.substringBefore('/')
        val rest = path.substringAfter('/', "")
        val base = Environment.getExternalStoragePublicDirectory(top)
        val dir = if (rest.isEmpty()) base else File(base, rest)
        dir.mkdirs()

        val stem = name.substringBeforeLast('.', name)
        val ext = if (name.contains('.')) "." + name.substringAfterLast('.') else ""
        var target = File(dir, name)
        var n = 1
        while (target.exists()) {
            target = File(dir, stem + "_" + n + ext)
            n++
        }
        FileOutputStream(target).use { o -> input.copyTo(o) }
        MediaScannerConnection.scanFile(appContext, arrayOf(target.absolutePath), null, null)
        return true
    }
}
