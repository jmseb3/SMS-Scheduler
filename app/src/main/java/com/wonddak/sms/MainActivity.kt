package com.wonddak.sms

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import com.wonddak.sms.data.AppStore
import com.wonddak.sms.scheduling.MessageAlarmScheduler
import com.wonddak.sms.scheduling.NotificationHelper
import com.wonddak.sms.ui.SmsSchedulerApp
import com.wonddak.sms.ui.theme.SMSSchedulerTheme

class MainActivity : ComponentActivity() {
    private var openHistoryRequest by mutableIntStateOf(0)
    private var openHistoryMessageId by mutableLongStateOf(-1L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_HISTORY, false)) {
            openHistoryMessageId = intent.getLongExtra(MessageAlarmScheduler.EXTRA_MESSAGE_ID, -1L)
            openHistoryRequest++
        }
        enableEdgeToEdge()
        val store = AppStore(applicationContext)
        setContent {
            SMSSchedulerTheme(dynamicColor = false) {
                SmsSchedulerApp(
                    store = store,
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
