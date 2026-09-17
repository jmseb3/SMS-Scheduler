package com.wonddak.sms.ui

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("배터리 최적화", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (excluded) "예약 발송을 위한 최적화 제외가 적용되었습니다."
                    else "절전 중에도 안정적으로 발송하려면 최적화 제외를 허용하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!excluded) {
                TextButton(onClick = {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    )
                    runCatching { launcher.launch(intent) }
                        .onFailure { notify("배터리 설정 화면을 열 수 없습니다.") }
                }) {
                    Text("허용 요청")
                }
            }
        }
    }
}
