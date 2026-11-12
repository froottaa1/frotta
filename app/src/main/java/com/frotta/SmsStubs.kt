package com.frotta

import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.provider.Telephony

class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (parts == null || parts.isEmpty()) return

        val body = parts.joinToString("") { it.messageBody ?: "" }
        val first = parts[0]

        val v = ContentValues()
        v.put(Telephony.Sms.ADDRESS, first.originatingAddress ?: "")
        v.put(Telephony.Sms.BODY, body)
        v.put(Telephony.Sms.DATE, System.currentTimeMillis())
        v.put(Telephony.Sms.DATE_SENT, first.timestampMillis)
        v.put(Telephony.Sms.READ, 0)
        v.put(Telephony.Sms.SEEN, 0)
        try {
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, v)
        } catch (e: Exception) {
        }
    }
}

class MmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
    }
}

class ComposeSmsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}

class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
