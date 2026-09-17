package com.wonddak.sms.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wonddak.sms.data.AppStore
import com.wonddak.sms.model.MessageStatus

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val now = System.currentTimeMillis()
        val scheduler = MessageAlarmScheduler(context)
        AppStore(context).loadScheduledMessages()
            .filter { it.status == MessageStatus.PENDING && it.sendAtMillis > now }
            .forEach {
                scheduler.schedule(it)
                scheduler.scheduleReminder(it)
            }
    }
}
