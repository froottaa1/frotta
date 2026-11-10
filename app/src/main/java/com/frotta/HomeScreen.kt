package com.frotta

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp

private fun requiredPermissions(): Array<String> {
    val list = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION
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

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("اختار إرسال أو استقبال للبدء") }
    var mode by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    val devices = remember { mutableStateListOf<Pair<String, String>>() }
    val myName = remember { "${Build.MODEL}" }

    val manager = remember {
        NearbyManager(
            context = context,
            onStatus = { status = it },
            onDeviceFound = { id, name ->
                if (devices.none { it.first == id }) devices.add(id to name)
            },
            onConnected = { connected = true }
        )
    }

    DisposableEffect(Unit) {
        onDispose { manager.stopAll() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            if (mode == "receive") manager.startReceiving(myName)
            if (mode == "send") manager.startSearching()
        } else {
            status = "لازم توافق على الصلاحيات عشان التطبيق يشتغل"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Frotta", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "انقل ملفاتك بدون إنترنت",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                mode = "send"
                devices.clear()
                permissionLauncher.launch(requiredPermissions())
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("إرسال")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                mode = "receive"
                permissionLauncher.launch(requiredPermissions())
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("استقبال")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(text = status, style = MaterialTheme.typography.bodyMedium)

        if (mode == "send" && !connected) {
            Spacer(modifier = Modifier.height(16.dp))
            devices.forEach { (id, name) ->
                OutlinedButton(
                    onClick = { manager.connectTo(id, myName) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("اتصل بـ $name")
                }
            }
        }
    }
}
