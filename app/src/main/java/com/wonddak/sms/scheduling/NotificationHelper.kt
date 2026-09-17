package com.wonddak.sms.scheduling

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wonddak.sms.R
import com.wonddak.sms.model.ScheduledMessage

object NotificationHelper {
    private const val CHANNEL_ID = "scheduled_sms"
    private const val REMINDER_NOTIFICATION_OFFSET = 2_000_000
    private const val COMPLETION_NOTIFICATION_OFFSET = 3_000_000

    @SuppressLint("MissingPermission")
    fun showReminder(context: Context, message: ScheduledMessage) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val cancelIntent = PendingIntent.getBroadcast(
            context,
            message.id.hashCode(),
            Intent(context, CancelScheduledMessageReceiver::class.java)
                .putExtra(MessageAlarmScheduler.EXTRA_MESSAGE_ID, message.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("문자 발송 1시간 전")
            .setContentText("${message.contactName}님에게 보낼 예약 문자가 있습니다.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "예약 취소", cancelIntent)
            .build()
        NotificationManagerCompat.from(context).notify(reminderNotificationId(message.id), notification)
    }

    @SuppressLint("MissingPermission")
    fun showCompletion(context: Context, message: ScheduledMessage, sent: Boolean) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val title = if (sent) "문자 발송 완료" else "문자 발송 실패"
        val body = if (sent) "${message.contactName}님에게 문자를 발송했습니다." else "${message.contactName}님에게 문자를 보내지 못했습니다."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.content))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(completionNotificationId(message.id), notification)
    }

    fun cancelReminder(context: Context, messageId: Long) {
        NotificationManagerCompat.from(context).cancel(reminderNotificationId(messageId))
    }

    private fun canNotify(context: Context): Boolean =
        android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT < 26) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "예약 문자",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "예약 문자 사전 알림과 발송 결과" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun reminderNotificationId(id: Long): Int = id.hashCode() + REMINDER_NOTIFICATION_OFFSET
    private fun completionNotificationId(id: Long): Int = id.hashCode() + COMPLETION_NOTIFICATION_OFFSET
}
