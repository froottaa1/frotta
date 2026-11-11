package com.frotta

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import androidx.core.content.ContextCompat
import java.io.File
import java.util.TimeZone
import org.json.JSONArray
import org.json.JSONObject

object DataSync {

    private fun norm(s: String): String = s.filter { it.isDigit() }.takeLast(9)

    fun hasPermission(context: Context, kind: String, write: Boolean): Boolean {
        val perms = ArrayList<String>()
        when (kind) {
            "contacts" -> {
                perms.add(Manifest.permission.READ_CONTACTS)
                if (write) perms.add(Manifest.permission.WRITE_CONTACTS)
            }
            "calls" -> {
                perms.add(Manifest.permission.READ_CALL_LOG)
                if (write) perms.add(Manifest.permission.WRITE_CALL_LOG)
            }
            "calendar" -> {
                perms.add(Manifest.permission.READ_CALENDAR)
                if (write) perms.add(Manifest.permission.WRITE_CALENDAR)
            }
        }
        if (perms.isEmpty()) return false
        return perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun export(context: Context, kind: String): File? {
        return try {
            val array = when (kind) {
                "contacts" -> exportContacts(context)
                "calls" -> exportCalls(context)
                "calendar" -> exportCalendar(context)
                else -> return null
            }
            val file = File(context.cacheDir, "__frotta_$kind.json")
            file.writeText(array.toString(), Charsets.UTF_8)
            file
        } catch (e: Exception) {
            null
        }
    }

    fun import(context: Context, kind: String, file: File): Int {
        if (!hasPermission(context, kind, true)) return -1
        return try {
            val arr = JSONArray(file.readText(Charsets.UTF_8))
            when (kind) {
                "contacts" -> importContacts(context, arr)
                "calls" -> importCalls(context, arr)
                "calendar" -> importCalendar(context, arr)
                else -> 0
            }
        } catch (e: Exception) {
            -2
        }
    }

    private class ContactItem {
        var name: String = ""
        var org: String = ""
        val phones = ArrayList<Pair<String, Int>>()
        val emails = ArrayList<Pair<String, Int>>()
    }

    private fun exportContacts(context: Context): JSONArray {
        val map = LinkedHashMap<Long, ContactItem>()
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2
        )
        val selection = ContactsContract.Data.MIMETYPE + " IN (?,?,?,?)"
        val args = arrayOf(
            StructuredName.CONTENT_ITEM_TYPE,
            Phone.CONTENT_ITEM_TYPE,
            Email.CONTENT_ITEM_TYPE,
            Organization.CONTENT_ITEM_TYPE
        )
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI, projection, selection, args, null
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                val mime = c.getString(1) ?: continue
                val d1 = c.getString(2) ?: ""
                val d2 = if (mime == Phone.CONTENT_ITEM_TYPE || mime == Email.CONTENT_ITEM_TYPE) {
                    c.getInt(3)
                } else {
                    0
                }
                val item = map.getOrPut(id) { ContactItem() }
                when (mime) {
                    StructuredName.CONTENT_ITEM_TYPE -> {
                        if (item.name.isEmpty()) item.name = d1
                    }
                    Phone.CONTENT_ITEM_TYPE -> {
                        if (d1.isNotBlank() && item.phones.none { norm(it.first) == norm(d1) }) {
                            item.phones.add(d1 to d2)
                        }
                    }
                    Email.CONTENT_ITEM_TYPE -> {
                        if (d1.isNotBlank() && item.emails.none { it.first.equals(d1, true) }) {
                            item.emails.add(d1 to d2)
                        }
                    }
                    Organization.CONTENT_ITEM_TYPE -> {
                        if (item.org.isEmpty()) item.org = d1
                    }
                }
            }
        }

        val out = JSONArray()
        for (item in map.values) {
            if (item.name.isEmpty() && item.phones.isEmpty() && item.emails.isEmpty()) continue
            val o = JSONObject()
            o.put("name", item.name)
            o.put("org", item.org)
            val ph = JSONArray()
            for ((n, t) in item.phones) {
                val p = JSONObject()
                p.put("v", n)
                p.put("t", t)
                ph.put(p)
            }
            o.put("phones", ph)
            val em = JSONArray()
            for ((n, t) in item.emails) {
                val p = JSONObject()
                p.put("v", n)
                p.put("t", t)
                em.put(p)
            }
            o.put("emails", em)
            out.put(o)
        }
        return out
    }

    private fun exportCalls(context: Context): JSONArray {
        val out = JSONArray()
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.CACHED_NAME
        )
        context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DATE + " ASC"
        )?.use { c ->
            while (c.moveToNext()) {
                val o = JSONObject()
                o.put("n", c.getString(0) ?: "")
                o.put("t", c.getInt(1))
                o.put("d", c.getLong(2))
                o.put("l", c.getLong(3))
                o.put("c", c.getString(4) ?: "")
                out.put(o)
            }
        }
        return out
    }

    private fun exportCalendar(context: Context): JSONArray {
        val out = JSONArray()
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_TIMEZONE,
            CalendarContract.Events.RRULE,
            CalendarContract.Events.DURATION
        )
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            CalendarContract.Events.DELETED + " = 0",
            null,
            null
        )?.use { c ->
            while (c.moveToNext()) {
                val o = JSONObject()
                o.put("t", c.getString(0) ?: "")
                o.put("ds", c.getString(1) ?: "")
                o.put("l", c.getString(2) ?: "")
                o.put("s", c.getLong(3))
                o.put("e", if (c.isNull(4)) 0L else c.getLong(4))
                o.put("a", c.getInt(5))
                o.put("z", c.getString(6) ?: "")
                o.put("r", c.getString(7) ?: "")
                o.put("u", c.getString(8) ?: "")
                out.put(o)
            }
        }
        return out
    }

    private fun existingPhones(context: Context): HashSet<String> {
        val set = HashSet<String>()
        context.contentResolver.query(
            Phone.CONTENT_URI, arrayOf(Phone.NUMBER), null, null, null
        )?.use { c ->
            while (c.moveToNext()) {
                set.add(norm(c.getString(0) ?: ""))
            }
        }
        return set
    }

    private fun importContacts(context: Context, arr: JSONArray): Int {
        val existing = existingPhones(context)
        val resolver = context.contentResolver
        var ops = ArrayList<ContentProviderOperation>()
        var pending = 0
        var added = 0

        fun flush() {
            if (ops.isNotEmpty()) {
                try {
                    resolver.applyBatch(ContactsContract.AUTHORITY, ops)
                    added += pending
                } catch (e: Exception) {
                }
                ops = ArrayList()
                pending = 0
            }
        }

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val phones = o.optJSONArray("phones") ?: JSONArray()
            val emails = o.optJSONArray("emails") ?: JSONArray()
            val name = o.optString("name", "")
            val org = o.optString("org", "")

            if (phones.length() > 0) {
                var allExist = true
                for (j in 0 until phones.length()) {
                    val n = norm(phones.getJSONObject(j).optString("v", ""))
                    if (n.isEmpty() || n !in existing) allExist = false
                }
                if (allExist) continue
            }

            val raw = ops.size
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )
            if (name.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, raw)
                        .withValue(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(StructuredName.DISPLAY_NAME, name)
                        .build()
                )
            }
            if (org.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, raw)
                        .withValue(ContactsContract.Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
                        .withValue(Organization.COMPANY, org)
                        .build()
                )
            }
            for (j in 0 until phones.length()) {
                val p = phones.getJSONObject(j)
                val v = p.optString("v", "")
                if (v.isEmpty()) continue
                val t = p.optInt("t", Phone.TYPE_MOBILE)
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, raw)
                        .withValue(ContactsContract.Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                        .withValue(Phone.NUMBER, v)
                        .withValue(Phone.TYPE, if (t in 1..20) t else Phone.TYPE_MOBILE)
                        .build()
                )
                existing.add(norm(v))
            }
            for (j in 0 until emails.length()) {
                val p = emails.getJSONObject(j)
                val v = p.optString("v", "")
                if (v.isEmpty()) continue
                val t = p.optInt("t", Email.TYPE_OTHER)
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, raw)
                        .withValue(ContactsContract.Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                        .withValue(Email.ADDRESS, v)
                        .withValue(Email.TYPE, if (t in 1..4) t else Email.TYPE_OTHER)
                        .build()
                )
            }
            pending++
            if (ops.size >= 300) flush()
        }
        flush()
        return added
    }

    private fun importCalls(context: Context, arr: JSONArray): Int {
        val resolver = context.contentResolver
        val existing = HashSet<String>()
        resolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE),
            null,
            null,
            null
        )?.use { c ->
            while (c.moveToNext()) {
                existing.add(c.getLong(1).toString() + "|" + norm(c.getString(0) ?: ""))
            }
        }

        var added = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val number = o.optString("n", "")
            val date = o.optLong("d", 0L)
            val key = date.toString() + "|" + norm(number)
            if (key in existing) continue

            val v = ContentValues()
            v.put(CallLog.Calls.NUMBER, number)
            v.put(CallLog.Calls.TYPE, o.optInt("t", 1))
            v.put(CallLog.Calls.DATE, date)
            v.put(CallLog.Calls.DURATION, o.optLong("l", 0L))
            v.put(CallLog.Calls.NEW, 0)
            val cachedName = o.optString("c", "")
            if (cachedName.isNotEmpty()) v.put(CallLog.Calls.CACHED_NAME, cachedName)
            try {
                if (resolver.insert(CallLog.Calls.CONTENT_URI, v) != null) {
                    added++
                    existing.add(key)
                }
            } catch (e: Exception) {
            }
        }
        return added
    }

    private fun writableCalendarId(context: Context): Long? {
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= ?",
            arrayOf("500"),
            null
        )?.use { c ->
            if (c.moveToFirst()) return c.getLong(0)
        }
        return null
    }

    private fun importCalendar(context: Context, arr: JSONArray): Int {
        val resolver = context.contentResolver
        val calId = writableCalendarId(context) ?: return -3

        val existing = HashSet<String>()
        resolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART),
            CalendarContract.Events.DELETED + " = 0",
            null,
            null
        )?.use { c ->
            while (c.moveToNext()) {
                existing.add((c.getString(0) ?: "") + "|" + c.getLong(1))
            }
        }

        var added = 0
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val title = o.optString("t", "")
            val start = o.optLong("s", 0L)
            val key = title + "|" + start
            if (key in existing) continue

            val allDay = o.optInt("a", 0)
            val rrule = o.optString("r", "")
            val v = ContentValues()
            v.put(CalendarContract.Events.CALENDAR_ID, calId)
            v.put(CalendarContract.Events.TITLE, title)
            v.put(CalendarContract.Events.DESCRIPTION, o.optString("ds", ""))
            v.put(CalendarContract.Events.EVENT_LOCATION, o.optString("l", ""))
            v.put(CalendarContract.Events.DTSTART, start)
            v.put(CalendarContract.Events.ALL_DAY, allDay)
            val tz = if (allDay == 1) {
                "UTC"
            } else {
                o.optString("z", "").ifEmpty { TimeZone.getDefault().id }
            }
            v.put(CalendarContract.Events.EVENT_TIMEZONE, tz)
            if (rrule.isNotEmpty()) {
                v.put(CalendarContract.Events.RRULE, rrule)
                val dur = o.optString("u", "")
                v.put(
                    CalendarContract.Events.DURATION,
                    if (dur.isNotEmpty()) dur else if (allDay == 1) "P1D" else "PT1H"
                )
            } else {
                val end = o.optLong("e", 0L)
                v.put(
                    CalendarContract.Events.DTEND,
                    if (end > 0L) end else start + 3600000L
                )
            }
            try {
                if (resolver.insert(CalendarContract.Events.CONTENT_URI, v) != null) {
                    added++
                    existing.add(key)
                }
            } catch (e: Exception) {
            }
        }
        return added
    }
}
