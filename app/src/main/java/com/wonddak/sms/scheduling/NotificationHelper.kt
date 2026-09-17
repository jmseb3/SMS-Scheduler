package com.wonddak.sms.scheduling

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wonddak.sms.R
import com.wonddak.sms.MainActivity
import com.wonddak.sms.model.ScheduledMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {
    private const val CHANNEL_ID = "scheduled_sms"
    private const val REMINDER_NOTIFICATION_OFFSET = 2_000_000
    private const val COMPLETION_NOTIFICATION_OFFSET = 3_000_000

    @SuppressLint("MissingPermission")
    fun showReminder(context: Context, message: ScheduledMessage) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val copy = reminderCopy(message)
        val openHistoryIntent = historyPendingIntent(context, message.id)
        if (android.os.Build.VERSION.SDK_INT >= 36) {
            showProgressReminder(context, message, openHistoryIntent, copy)
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(copy.title)
            .setContentText(copy.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.content))
            .setContentIntent(openHistoryIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(reminderNotificationId(message.id), notification)
    }

    @RequiresApi(36)
    @SuppressLint("MissingPermission")
    private fun showProgressReminder(
        context: Context,
        message: ScheduledMessage,
        openHistoryIntent: PendingIntent,
        copy: ReminderCopy,
    ) {
        val progress = copy.progress
        val progressStyle = Notification.ProgressStyle()
            .setStyledByProgress(false)
            .setProgress(progress)
            .setProgressSegments(
                listOf(Notification.ProgressStyle.Segment(100).setColor(Color.rgb(13, 107, 104))),
            )
            .setProgressPoints(
                listOf(
                    Notification.ProgressStyle.Point(1).setColor(Color.rgb(13, 107, 104)),
                    Notification.ProgressStyle.Point(100).setColor(Color.rgb(180, 94, 57)),
                ),
            )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(copy.title)
            .setContentText(copy.text)
            .setSubText(if (copy.isWithinHour) "곧 발송 → 내역에서 확인" else "예약됨 → 발송 예정")
            .setWhen(message.sendAtMillis)
            .setShowWhen(true)
            .setStyle(progressStyle)
            .setContentIntent(openHistoryIntent)
            .setAutoCancel(true)
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
            .setContentIntent(historyPendingIntent(context, message.id))
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

    private fun historyPendingIntent(context: Context, messageId: Long): PendingIntent =
        PendingIntent.getActivity(
            context,
            messageId.hashCode(),
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_OPEN_HISTORY, true)
                .putExtra(MessageAlarmScheduler.EXTRA_MESSAGE_ID, messageId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun reminderCopy(message: ScheduledMessage): ReminderCopy {
        val remainingMillis = message.sendAtMillis - System.currentTimeMillis()
        val isWithinHour = remainingMillis < ONE_HOUR
        val remainingMinutes = ((remainingMillis + MINUTE - 1) / MINUTE).coerceAtLeast(1)
        val time = SimpleDateFormat("HH:mm", Locale.KOREAN).format(Date(message.sendAtMillis))
        return if (isWithinHour) {
            ReminderCopy(
                title = "문자 발송 예정 · ${remainingMinutes}분 후",
                text = "${message.contactName}님에게 ${time}에 발송됩니다.",
                progress = ((ONE_HOUR - remainingMillis).coerceIn(0L, ONE_HOUR) * 100 / ONE_HOUR).toInt(),
                isWithinHour = true,
            )
        } else {
            ReminderCopy(
                title = "문자 발송 1시간 전",
                text = "${message.contactName}님에게 보낼 예약 문자가 있습니다.",
                progress = 0,
                isWithinHour = false,
            )
        }
    }

    private data class ReminderCopy(
        val title: String,
        val text: String,
        val progress: Int,
        val isWithinHour: Boolean,
    )

    private fun reminderNotificationId(id: Long): Int = id.hashCode() + REMINDER_NOTIFICATION_OFFSET
    private fun completionNotificationId(id: Long): Int = id.hashCode() + COMPLETION_NOTIFICATION_OFFSET

    private const val MINUTE = 60 * 1000L
    private const val ONE_HOUR = 60 * MINUTE
    const val EXTRA_OPEN_HISTORY = "open_history"
}
