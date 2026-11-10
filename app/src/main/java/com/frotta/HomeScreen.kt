package com.frotta

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {
    var status by remember { mutableStateOf("اختار إرسال أو استقبال للبدء") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Frotta",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "انقل ملفاتك بدون إنترنت",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { status = "وضع الإرسال (قريباً)" },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("إرسال")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { status = "وضع الاستقبال (قريباً)" },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("استقبال")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(text = status, style = MaterialTheme.typography.bodyMedium)
    }
}
