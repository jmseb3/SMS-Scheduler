package com.wonddak.sms.scheduling

import com.wonddak.sms.data.AppSettings
import com.wonddak.sms.model.MessageStatus
import com.wonddak.sms.model.ScheduledMessage

internal fun syncReminderSchedules(
    previous: AppSettings,
    updated: AppSettings,
    messages: List<ScheduledMessage>,
    nowMillis: Long = System.currentTimeMillis(),
    schedule: (ScheduledMessage) -> Unit,
    cancel: (ScheduledMessage) -> Unit,
) {
    if (previous.reminderEnabled == updated.reminderEnabled) return

    messages
        .filter { it.status == MessageStatus.PENDING && it.sendAtMillis > nowMillis }
        .forEach { message ->
            if (updated.reminderEnabled) schedule(message) else cancel(message)
        }
}
