package com.wonddak.sms.ui

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun BatteryOptimizationCard(notify: (String) -> Unit) {
    val context = LocalContext.current
    val powerManager = remember { context.getSystemService(PowerManager::class.java) }
    fun isExcluded(): Boolean = powerManager.isIgnoringBatteryOptimizations(context.packageName)
    var excluded by remember { mutableStateOf(isExcluded()) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        excluded = isExcluded()
        notify(if (excluded) "배터리 최적화 제외가 허용되었습니다." else "배터리 최적화 제외가 허용되지 않았습니다.")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "SYSTEM / BATTERY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                StatusTag(
                    text = if (excluded) "최적화 제외 적용" else "설정 필요",
                    color = if (excluded) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                )
                Text(
                    if (excluded) "절전 상태에서도 예약 발송을 준비합니다."
                    else "절전 중 예약 정확도를 높이려면 제외를 허용하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!excluded) {
                TextButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}"),
                        )
                        runCatching { launcher.launch(intent) }
                            .onFailure { notify("배터리 설정 화면을 열 수 없습니다.") }
                    },
                ) {
                    Text("허용 요청")
                }
            }
        }
    }
}
