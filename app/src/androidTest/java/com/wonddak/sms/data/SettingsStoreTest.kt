package com.wonddak.sms.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStoreTest {
    private lateinit var store: SettingsStore

    @Before
    fun setUp() {
        store = SettingsStore(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        store.save(AppSettings())
    }

    @Test
    fun savesAndReloadsSettings() {
        val expected = AppSettings(
            reminderEnabled = false,
            completionNotificationsEnabled = false,
            themeMode = ThemeMode.DARK,
        )

        store.save(expected)

        assertEquals(expected, store.load())
    }
}
