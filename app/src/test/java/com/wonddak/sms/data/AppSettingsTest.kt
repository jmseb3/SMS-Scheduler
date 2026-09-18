package com.wonddak.sms.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun `새 설정은 기존 알림 동작과 시스템 테마를 유지한다`() {
        val settings = AppSettings()

        assertTrue(settings.reminderEnabled)
        assertTrue(settings.completionNotificationsEnabled)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
    }

    @Test
    fun `알 수 없는 테마 값은 시스템 설정으로 복구한다`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored("UNKNOWN"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromStored("DARK"))
    }
}
