package com.frotta

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TransferScreen(modifier: Modifier = Modifier) {
    var statusText by remember { mutableStateOf("جاهز للعمليات") }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "تطبيق Frotta لنقل البيانات", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = statusText, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                statusText = "تم فحص الوسائط بنجاح"
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(text = "فحص وتجهيز الصور والألبومات")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                statusText = "تم تصدير سجل المكالمات والرسائل بنجاح"
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(text = "تصدير المكالمات والرسائل (JSON)")
        }
    }
}
