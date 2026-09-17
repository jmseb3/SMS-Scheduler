package com.wonddak.sms.scheduling

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.wonddak.sms.data.AppStore
import com.wonddak.sms.model.MessageStatus

class SmsAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(MessageAlarmScheduler.EXTRA_MESSAGE_ID, -1L)
        if (messageId < 0) return

        val store = AppStore(context)
        val message = store.loadScheduledMessages().firstOrNull { it.id == messageId } ?: return
        if (message.status != MessageStatus.PENDING) return

        val canSend = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (!canSend) {
            store.updateMessageStatus(messageId, MessageStatus.FAILED)
            NotificationHelper.showCompletion(context, message, sent = false)
            return
        }

        val status = runCatching {
            @Suppress("DEPRECATION")
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message.content)
            smsManager.sendMultipartTextMessage(message.phoneNumber, null, parts, null, null)
        }.fold(
            onSuccess = { MessageStatus.SENT },
            onFailure = { MessageStatus.FAILED },
        )
        store.updateMessageStatus(messageId, status)
        NotificationHelper.cancelReminder(context, messageId)
        NotificationHelper.showCompletion(context, message, status == MessageStatus.SENT)
    }
}
