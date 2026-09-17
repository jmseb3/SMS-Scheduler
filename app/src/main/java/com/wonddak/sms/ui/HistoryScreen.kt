package com.wonddak.sms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.wonddak.sms.model.MessageStatus
import com.wonddak.sms.model.ScheduledMessage
import com.wonddak.sms.scheduling.MessageAlarmScheduler
import com.wonddak.sms.scheduling.NotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    appState: AppState,
    notify: (String) -> Unit,
    modifier: Modifier = Modifier,
    targetMessageId: Long = -1L,
    navigationRequest: Int = 0,
) {
    val context = LocalContext.current
    val scheduler = remember { MessageAlarmScheduler(context.applicationContext) }
    val pendingCount = appState.messages.count { it.status == MessageStatus.PENDING }
    val listState = rememberLazyListState()

    LaunchedEffect(targetMessageId, navigationRequest, appState.messages.size) {
        val targetIndex = appState.messages.indexOfFirst { it.id == targetMessageId }
        if (targetIndex >= 0) listState.scrollToItem(targetIndex)
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = "예약 내역",
            description = "발송 대기와 처리 결과를 한곳에서 확인하세요.",
            modifier = Modifier.padding(top = 12.dp),
            trailing = { CountPill("${pendingCount}건 대기") },
        )
        if (appState.messages.isEmpty()) {
            EmptyHint("아직 예약된 문자가 없습니다.", Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(appState.messages, key = { it.id }) { message ->
                    ScheduledMessageCard(
                        message = message,
                        highlighted = message.id == targetMessageId,
                        onCancel = {
                            scheduler.cancel(message.id)
                            NotificationHelper.cancelReminder(context, message.id)
                            appState.removePendingMessage(message)
                            notify("예약을 취소했습니다.")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduledMessageCard(
    message: ScheduledMessage,
    highlighted: Boolean,
    onCancel: () -> Unit,
) {
    val formatter = remember { SimpleDateFormat("M월 d일 HH:mm", Locale.KOREAN) }
    val statusText = when (message.status) {
        MessageStatus.PENDING -> "대기 중"
        MessageStatus.SENT -> "발송됨"
        MessageStatus.FAILED -> "발송 실패"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        border = if (highlighted) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = when (message.status) {
                MessageStatus.PENDING -> MaterialTheme.colorScheme.surface
                MessageStatus.SENT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                MessageStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
            },
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${message.contactName} · ${message.phoneNumber}")
                Text(statusText, color = MaterialTheme.colorScheme.primary)
            }
            Text(formatter.format(Date(message.sendAtMillis)), style = MaterialTheme.typography.labelMedium)
            Text(message.content, style = MaterialTheme.typography.bodyMedium)
            if (message.status == MessageStatus.PENDING) {
                TextButton(onClick = onCancel) { Text("예약 취소") }
            }
        }
    }
}
