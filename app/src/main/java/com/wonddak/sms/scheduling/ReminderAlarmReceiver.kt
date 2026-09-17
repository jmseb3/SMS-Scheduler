package com.wonddak.sms.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wonddak.sms.data.AppStore
import com.wonddak.sms.model.MessageStatus

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(MessageAlarmScheduler.EXTRA_MESSAGE_ID, -1L)
        if (messageId < 0) return
        val message = AppStore(context).loadScheduledMessages()
            .firstOrNull { it.id == messageId && it.status == MessageStatus.PENDING }
            ?: return
        NotificationHelper.showReminder(context, message)
    }
}
