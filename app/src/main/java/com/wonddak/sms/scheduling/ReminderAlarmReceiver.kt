package com.wonddak.sms.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wonddak.sms.data.AppStore
import com.wonddak.sms.model.MessageStatus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var store: AppStore

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(MessageAlarmScheduler.EXTRA_MESSAGE_ID, -1L)
        if (messageId < 0) return
        val message = store.loadScheduledMessages()
            .firstOrNull { it.id == messageId && it.status == MessageStatus.PENDING }
            ?: return
        NotificationHelper.showReminder(context, message)
    }
}
