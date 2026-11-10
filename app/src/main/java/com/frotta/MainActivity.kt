package com.frotta

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.frotta.transfer.DataExporter
import com.frotta.transfer.MediaOrganizer

class MainActivity : ComponentActivity() {

    // طلب الصلاحيات اللازمة (صور، رسائل، مكالمات)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "تم منح جميع الصلاحيات بنجاح", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "بعض الصلاحيات مطلوبة لعمل التطبيق", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // التحقق من الصلاحيات عند فتح التطبيق
        checkAndRequestPermissions()

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
                    Text(text = "تطبيق Frotta للنقل الذكي", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(onClick = {
                        // تجهيز سحب الصور والمكالمات والرسائل
                        val mediaList = MediaOrganizer.getDeviceMedia(applicationContext)
                        val backupFile = DataExporter.exportSmsAndCallsToJson(applicationContext)
                        Toast.makeText(this@MainActivity, "تم تجهيز ${mediaList.size} صورة وملف النسخة الاحتياطية", Toast.LENGTH_LONG).show()
                    }) {
                        Text(text = "تجهيز وفحص البيانات للإرسال")
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_SMS,
            Manifest.permission.WRITE_CALL_LOG
        )

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        }
    }
}
