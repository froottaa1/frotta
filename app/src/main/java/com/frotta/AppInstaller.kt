package com.frotta

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap

data class InstallResult(val ok: Boolean, val message: String)

object InstallLog {
    private val map = ConcurrentHashMap<String, InstallResult>()

    fun put(pkg: String, ok: Boolean, message: String) {
        map[pkg] = InstallResult(ok, message)
    }

    fun take(pkg: String): InstallResult? = map.remove(pkg)
}

object AppInstaller {

    fun canInstall(context: Context): Boolean {
        return Build.VERSION.SDK_INT < 26 ||
            context.packageManager.canRequestPackageInstalls()
    }

    @Suppress("DEPRECATION")
    fun isInstalled(context: Context, pkg: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun install(context: Context, pkg: String, files: List<File>): String? {
        return try {
            val installer = context.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )
            params.setAppPackageName(pkg)
            val id = installer.createSession(params)
            val session = installer.openSession(id)
            try {
                for (f in files.sortedBy { it.name }) {
                    FileInputStream(f).use { input ->
                        session.openWrite(f.name, 0, f.length()).use { out ->
                            input.copyTo(out)
                            session.fsync(out)
                        }
                    }
                }
                val intent = Intent(context, InstallResultReceiver::class.java)
                intent.action = "com.frotta.INSTALL_RESULT"
                intent.putExtra("pkg", pkg)
                var flags = PendingIntent.FLAG_UPDATE_CURRENT
                if (Build.VERSION.SDK_INT >= 31) {
                    flags = flags or PendingIntent.FLAG_MUTABLE
                }
                val pi = PendingIntent.getBroadcast(context, id, intent, flags)
                session.commit(pi.intentSender)
            } finally {
                session.close()
            }
            null
        } catch (e: Exception) {
            e.message ?: "تعذر بدء التثبيت"
        }
    }
}

class InstallResultReceiver : BroadcastReceiver() {

    @Suppress("DEPRECATION")
    private fun confirmIntent(intent: Intent): Intent? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_INTENT)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )
        val pkg = intent.getStringExtra("pkg")
            ?: intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
            ?: ""
        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: ""

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = confirmIntent(intent)
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirm)
                    } catch (e: Exception) {
                        InstallLog.put(pkg, false, "تعذر فتح نافذة التأكيد")
                    }
                }
            }
            PackageInstaller.STATUS_SUCCESS -> InstallLog.put(pkg, true, "")
            PackageInstaller.STATUS_FAILURE_ABORTED ->
                InstallLog.put(pkg, false, "تم إلغاء التثبيت")
            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                InstallLog.put(pkg, false, "الهاتف منع التثبيت")
            PackageInstaller.STATUS_FAILURE_CONFLICT ->
                InstallLog.put(pkg, false, "تعارض مع نسخة مثبّتة (توقيع أو إصدار مختلف)")
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                InstallLog.put(pkg, false, "التطبيق غير متوافق مع الهاتف ده")
            PackageInstaller.STATUS_FAILURE_INVALID ->
                InstallLog.put(pkg, false, "ملف التطبيق غير صالح")
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                InstallLog.put(pkg, false, "مساحة التخزين مش كفاية")
            else -> InstallLog.put(pkg, false, if (msg.isEmpty()) "فشل التثبيت" else msg)
        }
    }
}
