package com.wonddak.sms.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromStored(value: String?): ThemeMode = entries
            .firstOrNull { it.name == value }
            ?: SYSTEM
    }
}

data class AppSettings(
    val reminderEnabled: Boolean = true,
    val completionNotificationsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        reminderEnabled = preferences.getBoolean(KEY_REMINDER_ENABLED, true),
        completionNotificationsEnabled = preferences.getBoolean(KEY_COMPLETION_ENABLED, true),
        themeMode = ThemeMode.fromStored(preferences.getString(KEY_THEME_MODE, null)),
    )

    fun save(settings: AppSettings) {
        preferences.edit()
            .putBoolean(KEY_REMINDER_ENABLED, settings.reminderEnabled)
            .putBoolean(KEY_COMPLETION_ENABLED, settings.completionNotificationsEnabled)
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "sms_scheduler_settings"
        const val KEY_REMINDER_ENABLED = "reminder_enabled"
        const val KEY_COMPLETION_ENABLED = "completion_notifications_enabled"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
