package com.wonddak.sms

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wonddak.sms.data.AppSettings
import com.wonddak.sms.data.AppStore
import com.wonddak.sms.data.SettingsStore
import com.wonddak.sms.data.ThemeMode
import com.wonddak.sms.scheduling.MessageAlarmScheduler
import com.wonddak.sms.scheduling.NotificationHelper
import com.wonddak.sms.ui.SmsSchedulerApp
import com.wonddak.sms.ui.theme.SMSSchedulerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var store: AppStore
    @Inject lateinit var scheduler: MessageAlarmScheduler
    @Inject lateinit var settingsStore: SettingsStore

    private var openHistoryRequest by mutableIntStateOf(0)
    private var openHistoryMessageId by mutableLongStateOf(-1L)
    private var appSettings by mutableStateOf(AppSettings())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_HISTORY, false)) {
            openHistoryMessageId = intent.getLongExtra(MessageAlarmScheduler.EXTRA_MESSAGE_ID, -1L)
            openHistoryRequest++
        }
        appSettings = settingsStore.load()
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        setContent {
            val darkTheme = when (appSettings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SMSSchedulerTheme(darkTheme = darkTheme, dynamicColor = false) {
                SmsSchedulerApp(
                    store = store,
                    scheduler = scheduler,
                    settings = appSettings,
                    onSettingsChange = { updated ->
                        appSettings = updated
                        settingsStore.save(updated)
                    },
                    openHistoryRequest = openHistoryRequest,
                    openHistoryMessageId = openHistoryMessageId,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_HISTORY, false)) {
            openHistoryMessageId = intent.getLongExtra(MessageAlarmScheduler.EXTRA_MESSAGE_ID, -1L)
            openHistoryRequest++
        }
    }
}
