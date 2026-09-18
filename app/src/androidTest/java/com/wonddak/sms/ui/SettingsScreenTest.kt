package com.wonddak.sms.ui

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.wonddak.sms.data.AppSettings
import com.wonddak.sms.data.ThemeMode
import com.wonddak.sms.ui.theme.SMSSchedulerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsReadinessAndHandlesThemeAndBackActions() {
        var changedSettings: AppSettings? = null
        var backClicked = false
        composeRule.setContent {
            SMSSchedulerTheme(dynamicColor = false) {
                SettingsContent(
                    settings = AppSettings(),
                    readiness = SystemReadiness(
                        canSendSms = true,
                        canNotify = false,
                        canScheduleExactAlarms = true,
                        ignoresBatteryOptimizations = false,
                    ),
                    appName = "문자 예약 발송",
                    versionName = "0.1.0",
                    onSettingsChange = { changedSettings = it },
                    onBack = { backClicked = true },
                    onRequestSmsPermission = {},
                    onRequestNotificationPermission = {},
                    onOpenExactAlarmSettings = {},
                    onOpenBatterySettings = {},
                )
            }
        }

        composeRule.onNodeWithText("알림 권한").assertExists()
        composeRule.onAllNodesWithText("설정 필요").onFirst().assertExists()
        composeRule.onNodeWithContentDescription("뒤로").performClick()
        composeRule.runOnIdle { assertTrue(backClicked) }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("어둡게"))
        composeRule.onNodeWithText("어둡게").performClick()
        composeRule.runOnIdle {
            assertEquals(ThemeMode.DARK, changedSettings?.themeMode)
        }
    }
}
