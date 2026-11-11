package com.frotta

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    var status by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    val devices = remember { mutableStateListOf<Pair<String, String>>() }
    val transfers = remember { mutableStateListOf<TransferItem>() }
    val myName = remember { Build.MODEL }

    val manager = remember {
        NearbyManager(
            context = context,
            onStatus = { status = it },
            onDeviceFound = { id, name ->
                if (devices.none { it.first == id }) devices.add(id to name)
            },
            onConnected = { connected = true },
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            if (screen == "receive") manager.startReceiving(myName)
            if (screen == "send") manager.startSearching()
        } else {
            status = "لازم توافق على الصلاحيات عشان التطبيق يشتغل"
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) manager.sendFiles(uris)
    }

    fun goHome() {
        manager.stopAll()
        devices.clear()
        transfers.clear()
        connected = false
        status = ""
        screen = "home"
    }

    BackHandler(enabled = screen != "home") { goHome() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Frotta",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "انقل ملفاتك بدون إنترنت",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (screen == "home") {
            ActionCard("📤", "إرسال", "ابعت ملفات لجهاز قريب") {
                screen = "send"
                permissionLauncher.launch(requiredPermissions())
            }
            Spacer(modifier = Modifier.height(16.dp))
            ActionCard("📥", "استقبال", "خلّي جهازك جاهز لاستلام الملفات") {
                screen = "receive"
                permissionLauncher.launch(requiredPermissions())
            }
        } else {
            Text(
                text = if (screen == "send") "إرسال" else "استقبال",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (status.isNotEmpty()) {
                Text(text = status, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (screen == "send" && !connected) {
                devices.forEach { (id, name) ->
                    OutlinedButton(
                        onClick = { manager.connectTo(id, myName) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📱 $name")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (screen == "send" && connected) {
                Button(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("اختار ملفات وابعت")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(transfers, key = { it.id }) { item ->
                    TransferRow(item)
                }
            }

            TextButton(onClick = { goHome() }) {
                Text("رجوع")
            }
        }
    }
}

@Composable
private fun ActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun TransferRow(item: TransferItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = (if (item.sending) "📤 " else "📥 ") + item.name,
            maxLines = 1,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(4.dp))
        when {
            item.failed -> Text(
                text = "فشل النقل",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            item.done -> Text(
                text = "تم ✅  " + formatSize(item.total),
                style = MaterialTheme.typography.bodySmall
            )
            else -> {
                val fraction = if (item.total > 0) {
                    (item.transferred.toFloat() / item.total.toFloat()).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = fraction,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "${(fraction * 100).toInt()}%   " +
                        formatSize(item.transferred) + " / " + formatSize(item.total),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
