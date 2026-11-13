package com.frotta

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object KeepAlive {
    const val CHANNEL = "frotta_transfer"
    const val NOTIF_ID = 4711

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm != null && nm.getNotificationChannel(CHANNEL) == null) {
                val ch = NotificationChannel(
                    CHANNEL,
                    "نقل الملفات",
                    NotificationManager.IMPORTANCE_LOW
                )
                nm.createNotificationChannel(ch)
            }
        }
    }

    fun build(context: Context, text: String): Notification {
        ensureChannel(context)
        val open = Intent(context, MainActivity::class.java)
        open.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val pi = PendingIntent.getActivity(
            context,
            0,
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Frotta")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .build()
    }

    fun start(context: Context, text: String) {
        try {
            val i = Intent(context, KeepAliveService::class.java)
            i.putExtra("text", text)
            ContextCompat.startForegroundService(context, i)
        } catch (e: Exception) {
        }
    }

    fun update(context: Context, text: String) {
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.notify(NOTIF_ID, build(context, text))
        } catch (e: Exception) {
        }
    }

    fun stop(context: Context) {
        try {
            context.stopService(Intent(context, KeepAliveService::class.java))
        } catch (e: Exception) {
        }
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.cancel(NOTIF_ID)
        } catch (e: Exception) {
        }
    }
}

class KeepAliveService : Service() {

    private var wake: PowerManager.WakeLock? = null
    private var wifi: WifiManager.WifiLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra("text") ?: "جاري النقل..."
        val n = KeepAlive.build(this, text)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(
                    KeepAlive.NOTIF_ID,
                    n,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(KeepAlive.NOTIF_ID, n)
            }
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            if (wake == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                val w = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "frotta:transfer")
                w.setReferenceCounted(false)
                w.acquire(4 * 60 * 60 * 1000L)
                wake = w
            }
            if (wifi == null) {
                val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val l = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "frotta:wifi")
                l.setReferenceCounted(false)
                l.acquire()
                wifi = l
            }
        } catch (e: Exception) {
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            wake?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
        }
        try {
            wifi?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
        }
        wake = null
        wifi = null
        super.onDestroy()
    }
}
