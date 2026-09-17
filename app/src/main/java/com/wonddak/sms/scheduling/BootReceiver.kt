package com.wonddak.sms.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wonddak.sms.data.AppStore
import com.wonddak.sms.model.MessageStatus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var store: AppStore
    @Inject lateinit var scheduler: MessageAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val now = System.currentTimeMillis()
        store.loadScheduledMessages()
            .filter { it.status == MessageStatus.PENDING && it.sendAtMillis > now }
            .forEach {
                scheduler.schedule(it)
                scheduler.scheduleReminder(it)
            }
    }
}
