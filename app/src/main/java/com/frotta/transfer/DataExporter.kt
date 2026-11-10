package com.frotta.transfer

import android.content.Context
import android.net.Uri
import android.provider.CallLog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter

object DataExporter {

    fun exportSmsAndCallsToJson(context: Context): File {
        val rootObj = JSONObject()
        
        // 1. استخراج الرسائل النصية (SMS)
        val smsArray = JSONArray()
        val smsUri = Uri.parse("content://sms/inbox")
        context.contentResolver.query(smsUri, arrayOf("address", "body", "date"), null, null, "date DESC")?.use { cursor ->
            val addressIndex = cursor.getColumnIndex("address")
            val bodyIndex = cursor.getColumnIndex("body")
            val dateIndex = cursor.getColumnIndex("date")
            
            while (cursor.moveToNext() && cursor.position < 1000) {
                val smsObj = JSONObject().apply {
                    put("address", cursor.getString(addressIndex))
                    put("body", cursor.getString(bodyIndex))
                    put("date", cursor.getLong(dateIndex))
                }
                smsArray.put(smsObj)
            }
        }
        rootObj.put("sms_messages", smsArray)

        // 2. استخراج سجل المكالمات (Call Logs)
        val callsArray = JSONArray()
        val callUri = CallLog.Calls.CONTENT_URI
        context.contentResolver.query(callUri, arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION), null, null, "${CallLog.Calls.DATE} DESC")?.use { cursor ->
            val numIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION)
            
            while (cursor.moveToNext() && cursor.position < 1000) {
                val callObj = JSONObject().apply {
                    put("number", cursor.getString(numIndex))
                    put("type", cursor.getInt(typeIndex))
                    put("date", cursor.getLong(dateIndex))
                    put("duration", cursor.getString(durationIndex))
                }
                callsArray.put(callObj)
            }
        }
        rootObj.put("call_logs", callsArray)

        // 3. حفظ البيانات في ملف JSON مؤقت للإرسال
        val exportFile = File(context.cacheDir, "frotta_backup.json")
        FileWriter(exportFile).use { writer ->
            writer.write(rootObj.toString(4))
        }
        
        return exportFile
    }
}
