package com.frotta

import android.Manifest
import android.app.role.RoleManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import androidx.core.content.ContextCompat
import java.io.File
import org.json.JSONObject

object SmsSync {

    private fun norm(s: String): String {
        val d = s.filter { it.isDigit() }.takeLast(9)
        return if (d.isEmpty()) s else d
    }

    private fun key(address: String, body: String, date: Long, type: Int): String {
        return date.toString() + "|" + norm(address) + "|" + type + "|" +
            body.length + "|" + body.hashCode()
    }

    fun hasRead(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun defaultPackage(context: Context): String? {
        return try {
            Telephony.Sms.getDefaultSmsPackage(context)
        } catch (e: Exception) {
            null
        }
    }

    fun isDefault(context: Context): Boolean {
        return defaultPackage(context) == context.packageName
    }

    @Suppress("DEPRECATION")
    fun label(context: Context, pkg: String?): String {
        if (pkg == null) return "تطبيق الرسائل الأصلي"
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: Exception) {
            "تطبيق الرسائل الأصلي"
        }
    }

    fun roleIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= 29) {
            val rm = context.getSystemService(RoleManager::class.java) ?: return null
            if (!rm.isRoleAvailable(RoleManager.ROLE_SMS)) return null
            return rm.createRequestRoleIntent(RoleManager.ROLE_SMS)
        }
        val i = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
        i.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
        return i
    }

    fun export(context: Context): File? {
        if (!hasRead(context)) return null
        return try {
            val file = File(context.cacheDir, "__frotta_sms.json")
            file.bufferedWriter(Charsets.UTF_8).use { w ->
                context.contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(
                        Telephony.Sms.ADDRESS,
                        Telephony.Sms.BODY,
                        Telephony.Sms.DATE,
                        Telephony.Sms.DATE_SENT,
                        Telephony.Sms.TYPE,
                        Telephony.Sms.READ
                    ),
                    null,
                    null,
                    Telephony.Sms.DATE + " ASC"
                )?.use { c ->
                    while (c.moveToNext()) {
                        val o = JSONObject()
                        o.put("a", c.getString(0) ?: "")
                        o.put("b", c.getString(1) ?: "")
                        o.put("d", c.getLong(2))
                        o.put("s", c.getLong(3))
                        o.put("t", c.getInt(4))
                        o.put("r", c.getInt(5))
                        w.write(o.toString())
                        w.newLine()
                    }
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }

    fun import(context: Context, file: File): Int {
        if (!isDefault(context)) return -1
        return try {
            val resolver = context.contentResolver
            val existing = HashSet<String>()
            resolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE
                ),
                null,
                null,
                null
            )?.use { c ->
                while (c.moveToNext()) {
                    existing.add(
                        key(c.getString(0) ?: "", c.getString(1) ?: "", c.getLong(2), c.getInt(3))
                    )
                }
            }

            var added = 0
            file.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    if (line.isBlank()) continue
                    try {
                        val o = JSONObject(line)
                        val type = o.optInt("t", 1)
                        if (type != 1 && type != 2 && type != 3) continue
                        val address = o.optString("a", "")
                        val body = o.optString("b", "")
                        val date = o.optLong("d", 0L)
                        val k = key(address, body, date, type)
                        if (k in existing) continue

                        val v = ContentValues()
                        v.put(Telephony.Sms.ADDRESS, address)
                        v.put(Telephony.Sms.BODY, body)
                        v.put(Telephony.Sms.DATE, date)
                        v.put(Telephony.Sms.DATE_SENT, o.optLong("s", 0L))
                        v.put(Telephony.Sms.TYPE, type)
                        v.put(Telephony.Sms.READ, o.optInt("r", 1))
                        v.put(Telephony.Sms.SEEN, 1)
                        if (resolver.insert(Telephony.Sms.CONTENT_URI, v) != null) {
                            added++
                            existing.add(k)
                        }
                    } catch (e: Exception) {
                    }
                }
            }
            added
        } catch (e: Exception) {
            -2
        }
    }
}
