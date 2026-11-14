package com.frotta

import android.graphics.Color as AColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    private fun applyBars(dark: Boolean) {
        val style = if (dark) {
            SystemBarStyle.dark(AColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AColor.TRANSPARENT, AColor.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        window.setBackgroundDrawable(
            ColorDrawable(if (dark) 0xFF0E0E0E.toInt() else 0xFFFFD600.toInt())
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("frotta", MODE_PRIVATE)
        Pal.dark = prefs.getBoolean("dark_mode", true)
        applyBars(Pal.dark)
        setContent {
            val dark = Pal.dark
            LaunchedEffect(dark) { applyBars(dark) }
            FrottaTheme {
                AppBackground {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ) {
                        HomeScreen()
                    }
                }
            }
        }
    }
}
