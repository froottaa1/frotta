package com.frotta

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

data class AppInfo(
    val pkg: String,
    val label: String,
    val paths: List<String>,
    val size: Long
)

object AppsReader {

    @Suppress("DEPRECATION")
    fun readApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val list = try {
            pm.getInstalledApplications(0)
        } catch (e: Exception) {
            emptyList<ApplicationInfo>()
        }
        val out = ArrayList<AppInfo>()
        for (ai in list) {
            val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val updated = (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            if (isSystem && !updated) continue
            if (ai.packageName == context.packageName) continue

            val paths = ArrayList<String>()
            if (!ai.sourceDir.isNullOrEmpty()) paths.add(ai.sourceDir)
            ai.splitSourceDirs?.forEach { paths.add(it) }
            val existing = paths.filter { File(it).exists() }
            if (existing.isEmpty()) continue

            val size = existing.sumOf { File(it).length() }
            val label = try {
                pm.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                ai.packageName
            }
            out.add(AppInfo(ai.packageName, label, existing, size))
        }
        return out.sortedBy { it.label.lowercase() }
    }

    fun toItems(apps: List<AppInfo>): List<MediaItem> {
        return apps.map {
            MediaItem(Uri.parse("app:" + it.pkg), it.label, it.pkg, it.size, 0L)
        }
    }

    fun exportMeta(context: Context, apps: List<AppInfo>): File? {
        return try {
            val arr = JSONArray()
            for (a in apps) {
                val o = JSONObject()
                o.put("p", a.pkg)
                o.put("l", a.label)
                o.put("n", a.paths.size)
                arr.put(o)
            }
            val f = File(context.cacheDir, "__frotta_apps.json")
            f.writeText(arr.toString(), Charsets.UTF_8)
            f
        } catch (e: Exception) {
            null
        }
    }

    fun parseMeta(file: File): Map<String, Pair<String, Int>> {
        val map = LinkedHashMap<String, Pair<String, Int>>()
        try {
            val arr = JSONArray(file.readText(Charsets.UTF_8))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val p = o.optString("p", "")
                if (p.isNotEmpty()) {
                    map[p] = Pair(o.optString("l", p), o.optInt("n", 1))
                }
            }
        } catch (e: Exception) {
        }
        return map
    }

    fun outFiles(apps: List<AppInfo>): List<OutFile> {
        val out = ArrayList<OutFile>()
        for (a in apps) {
            for (p in a.paths) {
                val f = File(p)
                out.add(OutFile(Uri.fromFile(f), f.name, "__frotta_apps/" + a.pkg))
            }
        }
        return out
    }
}
