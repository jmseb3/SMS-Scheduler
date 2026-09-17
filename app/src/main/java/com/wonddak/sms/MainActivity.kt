package com.wonddak.sms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wonddak.sms.data.AppStore
import com.wonddak.sms.ui.SmsSchedulerApp
import com.wonddak.sms.ui.theme.SMSSchedulerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = AppStore(applicationContext)
        setContent {
            SMSSchedulerTheme(dynamicColor = false) {
                SmsSchedulerApp(store = store)
            }
        }
    }
}
