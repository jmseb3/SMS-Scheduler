package com.wonddak.sms.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

data class SystemReadiness(
    val canSendSms: Boolean,
    val canNotify: Boolean,
    val canScheduleExactAlarms: Boolean,
    val ignoresBatteryOptimizations: Boolean,
) {
    val isReady: Boolean
        get() = canSendSms && canNotify && canScheduleExactAlarms && ignoresBatteryOptimizations

    val issueCount: Int
        get() = listOf(
            canSendSms,
            canNotify,
            canScheduleExactAlarms,
            ignoresBatteryOptimizations,
        ).count { !it }
}

fun readSystemReadiness(context: Context): SystemReadiness {
    val alarmManager = context.getSystemService(AlarmManager::class.java)
    val powerManager = context.getSystemService(PowerManager::class.java)
    return SystemReadiness(
        canSendSms = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED,
        canNotify = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED,
        canScheduleExactAlarms = alarmManager.canScheduleExactAlarms(),
        ignoresBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName),
    )
}

@Composable
fun rememberSystemReadiness(refreshKey: Int = 0): State<SystemReadiness> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val readiness = remember { mutableStateOf(readSystemReadiness(context)) }

    fun refresh() {
        readiness.value = readSystemReadiness(context)
    }

    LaunchedEffect(refreshKey) { refresh() }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return readiness
}

@Composable
fun SystemReadinessBanner(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val readiness = rememberSystemReadiness().value
    if (readiness.isReady) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "발송 준비 점검 필요",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    "확인할 시스템 설정이 ${readiness.issueCount}개 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onOpenSettings) { Text("확인") }
        }
    }
}
