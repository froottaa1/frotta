package com.frotta

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val time: Long,
    val kind: String,
    val peer: String,
    val files: Int,
    val failed: Int,
    val skipped: Int,
    val bytes: Long
)

object HistoryStore {
    private const val KEY = "history_v1"
    private const val MAX = 50

    fun load(prefs: SharedPreferences): List<HistoryEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<HistoryEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    HistoryEntry(
                        o.optLong("t", 0L),
                        o.optString("k", ""),
                        o.optString("p", ""),
                        o.optInt("f", 0),
                        o.optInt("x", 0),
                        o.optInt("s", 0),
                        o.optLong("b", 0L)
                    )
                )
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save(prefs: SharedPreferences, list: List<HistoryEntry>) {
        val arr = JSONArray()
        for (e in list) {
            val o = JSONObject()
            o.put("t", e.time)
            o.put("k", e.kind)
            o.put("p", e.peer)
            o.put("f", e.files)
            o.put("x", e.failed)
            o.put("s", e.skipped)
            o.put("b", e.bytes)
            arr.put(o)
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun add(prefs: SharedPreferences, entry: HistoryEntry): List<HistoryEntry> {
        val list = ArrayList<HistoryEntry>()
        list.add(entry)
        list.addAll(load(prefs))
        val trimmed = list.take(MAX)
        save(prefs, trimmed)
        return trimmed
    }

    fun clear(prefs: SharedPreferences) {
        prefs.edit().remove(KEY).apply()
    }
}
