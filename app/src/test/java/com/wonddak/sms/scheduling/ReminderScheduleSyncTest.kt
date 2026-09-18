package com.wonddak.sms.scheduling

import com.wonddak.sms.data.AppSettings
import com.wonddak.sms.model.MessageStatus
import com.wonddak.sms.model.ScheduledMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderScheduleSyncTest {
    private val now = 1_000L
    private val futurePending = message(id = 1L, sendAtMillis = 2_000L)
    private val pastPending = message(id = 2L, sendAtMillis = 500L)
    private val futureSent = message(id = 3L, sendAtMillis = 2_000L, status = MessageStatus.SENT)

    @Test
    fun `사전 알림을 켜면 미래 대기 예약만 등록한다`() {
        val scheduled = mutableListOf<Long>()

        syncReminderSchedules(
            previous = AppSettings(reminderEnabled = false),
            updated = AppSettings(reminderEnabled = true),
            messages = listOf(futurePending, pastPending, futureSent),
            nowMillis = now,
            schedule = { scheduled += it.id },
            cancel = {},
        )

        assertEquals(listOf(1L), scheduled)
    }

    @Test
    fun `사전 알림을 끄면 미래 대기 예약만 취소한다`() {
        val cancelled = mutableListOf<Long>()

        syncReminderSchedules(
            previous = AppSettings(reminderEnabled = true),
            updated = AppSettings(reminderEnabled = false),
            messages = listOf(futurePending, pastPending, futureSent),
            nowMillis = now,
            schedule = {},
            cancel = { cancelled += it.id },
        )

        assertEquals(listOf(1L), cancelled)
    }

    private fun message(
        id: Long,
        sendAtMillis: Long,
        status: MessageStatus = MessageStatus.PENDING,
    ) = ScheduledMessage(
        id = id,
        contactName = "테스트",
        phoneNumber = "01000000000",
        templateTitle = null,
        content = "테스트",
        sendAtMillis = sendAtMillis,
        status = status,
    )
}
