package com.wonddak.sms.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wonddak.sms.data.AppSettings
import com.wonddak.sms.data.ThemeMode
import com.wonddak.sms.ui.theme.SMSSchedulerTheme

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    notify: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    val readiness by rememberSystemReadiness(refreshKey)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { refreshKey++ }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refreshKey++ }
    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val appName = remember {
        context.applicationInfo.loadLabel(context.packageManager).toString()
    }

    fun openSystemSettings(intent: Intent, failureMessage: String) {
        runCatching { settingsLauncher.launch(intent) }
            .onFailure { notify(failureMessage) }
    }

    BackHandler(onBack = onBack)
    SettingsContent(
        settings = settings,
        readiness = readiness,
        appName = appName,
        versionName = packageInfo.versionName.orEmpty(),
        onSettingsChange = onSettingsChange,
        onBack = onBack,
        onRequestSmsPermission = {
            permissionLauncher.launch(Manifest.permission.SEND_SMS)
        },
        onRequestNotificationPermission = {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        onOpenExactAlarmSettings = {
            openSystemSettings(
                Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${context.packageName}"),
                ),
                "정확한 알람 설정 화면을 열 수 없습니다.",
            )
        },
        onOpenBatterySettings = {
            openSystemSettings(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}"),
                ),
                "배터리 설정 화면을 열 수 없습니다.",
            )
        },
        modifier = modifier,
    )
}

@Composable
internal fun SettingsContent(
    settings: AppSettings,
    readiness: SystemReadiness,
    appName: String,
    versionName: String,
    onSettingsChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "뒤로")
                    }
                    ScreenHeader(
                        title = "설정",
                        description = "예약 발송 환경과 알림 방식을 관리합니다.",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                ToolSection(index = "01", title = "발송 준비") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ReadinessRow(
                            title = "문자 발송 권한",
                            description = "예약된 문자를 기기에서 발송합니다.",
                            ready = readiness.canSendSms,
                            onAction = onRequestSmsPermission,
                        )
                        ReadinessRow(
                            title = "알림 권한",
                            description = "사전 알림과 발송 결과를 표시합니다.",
                            ready = readiness.canNotify,
                            onAction = onRequestNotificationPermission,
                        )
                        ReadinessRow(
                            title = "정확한 알람",
                            description = "예약 시각에 최대한 정확하게 발송합니다.",
                            ready = readiness.canScheduleExactAlarms,
                            onAction = onOpenExactAlarmSettings,
                        )
                        ReadinessRow(
                            title = "배터리 최적화 제외",
                            description = "절전 중에도 예약 발송을 준비합니다.",
                            ready = readiness.ignoresBatteryOptimizations,
                            onAction = onOpenBatterySettings,
                        )
                    }
                }
            }
            item {
                ToolSection(index = "02", title = "알림") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsSwitchRow(
                            title = "1시간 전 사전 알림",
                            description = "예약 발송 한 시간 전에 알려드립니다.",
                            checked = settings.reminderEnabled,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(reminderEnabled = it))
                            },
                        )
                        SettingsSwitchRow(
                            title = "발송 결과 알림",
                            description = "발송 성공 또는 실패 결과를 알려드립니다.",
                            checked = settings.completionNotificationsEnabled,
                            onCheckedChange = {
                                onSettingsChange(settings.copy(completionNotificationsEnabled = it))
                            },
                        )
                    }
                }
            }
            item {
                ToolSection(index = "03", title = "화면") {
                    Column {
                        ThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> "시스템 설정"
                                ThemeMode.LIGHT -> "밝게"
                                ThemeMode.DARK -> "어둡게"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .selectable(
                                        selected = settings.themeMode == mode,
                                        role = Role.RadioButton,
                                        onClick = {
                                            onSettingsChange(settings.copy(themeMode = mode))
                                        },
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = settings.themeMode == mode,
                                    onClick = null,
                                )
                                Text(label, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
            item {
                ToolSection(index = "04", title = "앱 정보") {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(appName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "버전 ${versionName.ifBlank { "-" }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private val PreviewReadiness = SystemReadiness(
    canSendSms = true,
    canNotify = false,
    canScheduleExactAlarms = true,
    ignoresBatteryOptimizations = false,
)

@Preview(name = "설정 · 휴대폰", showBackground = true)
@Preview(name = "설정 · 태블릿", device = "spec:width=1280dp,height=800dp,dpi=240", showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    SMSSchedulerTheme(dynamicColor = false) {
        SettingsContent(
            settings = AppSettings(),
            readiness = PreviewReadiness,
            appName = "문자 예약 발송",
            versionName = "0.1.0",
            onSettingsChange = {},
            onBack = {},
            onRequestSmsPermission = {},
            onRequestNotificationPermission = {},
            onOpenExactAlarmSettings = {},
            onOpenBatterySettings = {},
        )
    }
}
