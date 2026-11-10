package com.frotta.transfer

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CallLog
import org.json.JSONObject

object DataImporter {

    fun restoreDataFromJson(context: Context, jsonObject: JSONObject) {
        // 1. استعادة سجل المكالمات
        if (jsonObject.has("call_logs")) {
            val callsArray = jsonObject.getJSONArray("call_logs")
            for (i in 0 until callsArray.length()) {
                val obj = callsArray.getJSONObject(i)
                val values = ContentValues().apply {
                    put(CallLog.Calls.NUMBER, obj.getString("number"))
                    put(CallLog.Calls.TYPE, obj.getInt("type"))
                    put(CallLog.Calls.DATE, obj.getLong("date"))
                    put(CallLog.Calls.DURATION, obj.getString("duration"))
                    put(CallLog.Calls.NEW, 1)
                }
                try {
                    context.contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 2. استعادة الرسائل النصية (SMS)
        if (jsonObject.has("sms_messages")) {
            val smsArray = jsonObject.getJSONArray("sms_messages")
            for (i in 0 until smsArray.length()) {
                val obj = smsArray.getJSONObject(i)
                val values = ContentValues().apply {
                    put("address", obj.getString("address"))
                    put("body", obj.getString("body"))
                    put("date", obj.getLong("date"))
                    put("type", 1) // 1 تعني رسالة واردة (Inbox)
                }
                try {
                    context.contentResolver.insert(Uri.parse("content://sms/"), values)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
