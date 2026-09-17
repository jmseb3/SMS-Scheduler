package com.wonddak.sms.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.wonddak.sms.model.ScheduledMessage

class MessageAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Returns true when Android allowed an exact alarm. */
    fun schedule(message: ScheduledMessage): Boolean {
        return scheduleAt(message.id, message.sendAtMillis, SmsAlarmReceiver::class.java, SEND_REQUEST_OFFSET)
    }

    fun scheduleReminder(message: ScheduledMessage): Boolean {
        val reminderAt = maxOf(System.currentTimeMillis() + 1_000L, message.sendAtMillis - ONE_HOUR)
        return scheduleAt(message.id, reminderAt, ReminderAlarmReceiver::class.java, REMINDER_REQUEST_OFFSET)
    }

    private fun scheduleAt(messageId: Long, triggerAtMillis: Long, receiver: Class<*>, requestOffset: Int): Boolean {
        val operation = pendingIntent(messageId, receiver, PendingIntent.FLAG_UPDATE_CURRENT, requestOffset)
            ?: return false
        return if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                operation,
            )
            true
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                operation,
            )
            false
        }
    }

    fun cancel(messageId: Long) {
        cancel(messageId, SmsAlarmReceiver::class.java, SEND_REQUEST_OFFSET)
        cancel(messageId, ReminderAlarmReceiver::class.java, REMINDER_REQUEST_OFFSET)
    }

    private fun cancel(messageId: Long, receiver: Class<*>, requestOffset: Int) {
        pendingIntent(messageId, receiver, PendingIntent.FLAG_NO_CREATE, requestOffset)
            ?.let(alarmManager::cancel)
    }

    private fun pendingIntent(
        messageId: Long,
        receiver: Class<*>,
        updateFlag: Int,
        requestOffset: Int,
    ): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            messageId.hashCode() + requestOffset,
            Intent(context, receiver).putExtra(EXTRA_MESSAGE_ID, messageId),
            updateFlag or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val EXTRA_MESSAGE_ID = "message_id"
        private const val SEND_REQUEST_OFFSET = 0
        private const val REMINDER_REQUEST_OFFSET = 1_000_000
        private const val ONE_HOUR = 60 * 60 * 1000L
    }
}
