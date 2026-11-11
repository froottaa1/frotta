package com.frotta

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
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

data class TransferItem(
    val id: Long,
    val name: String,
    val sending: Boolean,
    val transferred: Long,
    val total: Long,
    val done: Boolean,
    val failed: Boolean
)

class NearbyManager(
    context: Context,
    private val onStatus: (String) -> Unit,
    private val onConnected: () -> Unit,
    private val onDisconnected: () -> Unit,
    private val onTransfer: (TransferItem) -> Unit
) {
    private val appContext = context.applicationContext
    private val client: ConnectionsClient = Nearby.getConnectionsClient(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceId = "com.frotta.share"
    private val strategy = Strategy.P2P_CLUSTER
    private var connectedEndpoint: String? = null
    private var expectedToken: String = ""
    private var myName: String = ""

    private val fileIds = HashSet<Long>()
    private val outgoingIds = HashSet<Long>()
    private val fileNames = HashMap<Long, String>()
    private val incoming = HashMap<Long, Payload>()

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
            val name = fileNames[id] ?: "ملف"
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
                onStatus("تم الاتصال ✅")
                onConnected()
            } else {
                onStatus("فشل الاتصال")
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoint = null
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
        val endpoint = connectedEndpoint ?: return
        for (uri in uris) {
            try {
                val pfd = appContext.contentResolver.openFileDescriptor(uri, "r") ?: continue
                val name = queryName(uri)
                val payload = Payload.fromFile(pfd)
                fileIds.add(payload.id)
                outgoingIds.add(payload.id)
                fileNames[payload.id] = name

                val header = "${payload.id}:$name".toByteArray(Charsets.UTF_8)
                client.sendPayload(endpoint, Payload.fromBytes(header))
                client.sendPayload(endpoint, payload)

                onTransfer(TransferItem(payload.id, name, true, 0L, pfd.statSize, false, false))
            } catch (e: Exception) {
                onStatus("تعذر قراءة ملف")
            }
        }
    }

    fun stopAll() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        connectedEndpoint = null
        expectedToken = ""
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

    private fun saveIncoming(id: Long) {
        val payload = incoming[id] ?: return
        val name = (fileNames[id] ?: "frotta_$id").substringAfterLast('/')
        val total = payload.asFile()?.size ?: 0L
        onTransfer(TransferItem(id, name, false, total, total, false, false))
        Thread {
            val ok = saveToDownloads(payload, name)
            mainHandler.post {
                onTransfer(TransferItem(id, name, false, total, total, ok, !ok))
                if (ok) onStatus("تم حفظ: $name")
            }
        }.start()
    }

    private fun saveToDownloads(payload: Payload, name: String): Boolean {
        val pfd = payload.asFile()?.asParcelFileDescriptor() ?: return false
        return try {
            val input = ParcelFileDescriptor.AutoCloseInputStream(pfd)
            input.use {
                if (Build.VERSION.SDK_INT >= 29) {
                    val values = ContentValues()
                    values.put(MediaStore.Downloads.DISPLAY_NAME, name)
                    values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/Frotta")
                    val uri = appContext.contentResolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                    ) ?: return false
                    val out = appContext.contentResolver.openOutputStream(uri) ?: return false
                    out.use { o -> input.copyTo(o) }
                } else {
                    val dir = File(
                        appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        "Frotta"
                    )
                    dir.mkdirs()
                    FileOutputStream(File(dir, name)).use { o -> input.copyTo(o) }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
