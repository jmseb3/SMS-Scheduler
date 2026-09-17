package com.wonddak.sms.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wonddak.sms.data.AppStore
import com.wonddak.sms.model.MessageStatus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CancelScheduledMessageReceiver : BroadcastReceiver() {
    @Inject lateinit var store: AppStore
    @Inject lateinit var scheduler: MessageAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(MessageAlarmScheduler.EXTRA_MESSAGE_ID, -1L)
        if (messageId < 0) return
        val messages = store.loadScheduledMessages()
        val message = messages.firstOrNull { it.id == messageId } ?: return
        if (message.status != MessageStatus.PENDING) return
        store.saveScheduledMessages(messages.filterNot { it.id == messageId })
        scheduler.cancel(messageId)
        NotificationHelper.cancelReminder(context, messageId)
    }
}
