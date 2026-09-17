package com.wonddak.sms.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wonddak.sms.data.AppStore
import com.wonddak.sms.model.MessageStatus

class CancelScheduledMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(MessageAlarmScheduler.EXTRA_MESSAGE_ID, -1L)
        if (messageId < 0) return
        val store = AppStore(context)
        val messages = store.loadScheduledMessages()
        val message = messages.firstOrNull { it.id == messageId } ?: return
        if (message.status != MessageStatus.PENDING) return
        store.saveScheduledMessages(messages.filterNot { it.id == messageId })
        MessageAlarmScheduler(context).cancel(messageId)
        NotificationHelper.cancelReminder(context, messageId)
    }
}
