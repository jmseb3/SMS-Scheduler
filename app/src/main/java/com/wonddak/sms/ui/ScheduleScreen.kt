package com.wonddak.sms.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wonddak.sms.model.MessageStatus
import com.wonddak.sms.model.MessageTemplate
import com.wonddak.sms.model.ScheduledMessage
import com.wonddak.sms.model.SmsContact
import com.wonddak.sms.model.TemplateEngine
import com.wonddak.sms.scheduling.MessageAlarmScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleScreen(
    appState: AppState,
    notify: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scheduler = remember { MessageAlarmScheduler(context.applicationContext) }
    var selectedContact by remember { mutableStateOf<SmsContact?>(null) }
    var selectedTemplate by remember { mutableStateOf<MessageTemplate?>(null) }
    var content by remember { mutableStateOf("") }
    var sendAtMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 60 * 60 * 1000) }
    val variableValues = remember { mutableStateMapOf<String, String>() }
    var pendingSchedule by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        if (permissions[Manifest.permission.SEND_SMS] == true) {
            pendingSchedule?.invoke()
            if (Build.VERSION.SDK_INT >= 33 && permissions[Manifest.permission.POST_NOTIFICATIONS] != true) {
                notify("SMS는 예약했지만 알림 권한이 없어 사전 알림을 표시할 수 없습니다.")
            }
        } else {
            notify("문자를 발송하려면 SMS 권한이 필요합니다.")
        }
        pendingSchedule = null
    }

    fun createSchedule() {
        val contact = selectedContact ?: return
        val resolved = TemplateEngine.resolve(content.trim(), variableValues)
        val message = ScheduledMessage(
            id = appState.newId(),
            contactName = contact.name,
            phoneNumber = contact.phoneNumber,
            templateTitle = selectedTemplate?.title,
            content = resolved,
            sendAtMillis = sendAtMillis,
        )
        appState.addMessage(message)
        val exact = scheduler.schedule(message)
        scheduler.scheduleReminder(message)
        notify(if (exact) "문자 발송을 예약했습니다." else "예약했습니다. 시스템 설정에 따라 시간이 조금 늦어질 수 있습니다.")
        content = ""
        selectedTemplate = null
        variableValues.clear()
    }

    val variables = TemplateEngine.variables(content)
    val canSchedule = selectedContact != null && content.isNotBlank() &&
        sendAtMillis > System.currentTimeMillis() && variables.all { !variableValues[it].isNullOrBlank() }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ScreenHeader(
                    title = "새 예약",
                    description = "받는 사람과 내용을 정한 뒤, 발송 시간을 선택하세요.",
                    trailing = {
                        CountPill("${appState.messages.count { it.status == MessageStatus.PENDING }}건 대기")
                    },
                )
                BatteryOptimizationCard(notify = notify)
                Text("발송 정보", style = MaterialTheme.typography.titleMedium)
                Selector(
                    label = "받는 사람",
                    selectedText = selectedContact?.let { "${it.name} · ${it.phoneNumber}" },
                    emptyText = if (appState.contacts.isEmpty()) "연락처 탭에서 먼저 추가하세요" else "연락처 선택",
                    items = appState.contacts,
                    itemText = { "${it.name} · ${it.phoneNumber}" },
                    onSelected = {
                        selectedContact = it
                        if (TemplateEngine.variables(content).contains("이름")) {
                            variableValues["이름"] = it.name
                        }
                    },
                )
                Selector(
                    label = "템플릿 (선택)",
                    selectedText = selectedTemplate?.title,
                    emptyText = "직접 입력",
                    items = appState.templates,
                    itemText = { it.title },
                    onSelected = {
                        selectedTemplate = it
                        content = it.content
                        variableValues.clear()
                        if (TemplateEngine.variables(it.content).contains("이름")) {
                            variableValues["이름"] = selectedContact?.name.orEmpty()
                        }
                    },
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("메시지") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                )
                variables.forEach { variable ->
                    OutlinedTextField(
                        value = variableValues[variable].orEmpty(),
                        onValueChange = { variableValues[variable] = it },
                        label = { Text("{{$variable}} 값") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                if (variables.isNotEmpty()) {
                    Text(
                        "미리보기: ${TemplateEngine.resolve(content, variableValues)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DateTimeChooser(sendAtMillis) { sendAtMillis = it }
                Button(
                    onClick = {
                        val smsGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.SEND_SMS,
                        ) == PackageManager.PERMISSION_GRANTED
                        val notificationsGranted = Build.VERSION.SDK_INT < 33 ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                        if (smsGranted && notificationsGranted) {
                            createSchedule()
                        } else {
                            pendingSchedule = ::createSchedule
                            permissionLauncher.launch(
                                buildList {
                                    add(Manifest.permission.SEND_SMS)
                                    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                                }.toTypedArray(),
                            )
                        }
                    },
                    enabled = canSchedule,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("발송 예약") }
                Text("예약 내역", style = MaterialTheme.typography.titleLarge)
                Text(
                    "등록된 예약은 발송 전까지 취소할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (appState.messages.isEmpty()) {
            item { Text("아직 예약된 문자가 없습니다.", modifier = Modifier.padding(vertical = 24.dp)) }
        } else {
            items(appState.messages, key = { it.id }) { message ->
                ScheduledMessageCard(
                    message = message,
                    onCancel = {
                        scheduler.cancel(message.id)
                        com.wonddak.sms.scheduling.NotificationHelper.cancelReminder(context, message.id)
                        appState.removePendingMessage(message)
                        notify("예약을 취소했습니다.")
                    },
                )
            }
        }
        item { Box(Modifier.padding(bottom = 8.dp)) }
    }
}

@Composable
private fun <T> Selector(
    label: String,
    selectedText: String?,
    emptyText: String,
    items: List<T>,
    itemText: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(selectedText ?: emptyText) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(itemText(item)) },
                        onClick = { onSelected(item); expanded = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun DateTimeChooser(value: Long, onChange: (Long) -> Unit) {
    val context = LocalContext.current
    val calendar = remember(value) { Calendar.getInstance().apply { timeInMillis = value } }
    val formatter = remember { SimpleDateFormat("yyyy년 M월 d일 (E) HH:mm", Locale.KOREAN) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    onChange(Calendar.getInstance().apply {
                        timeInMillis = value
                        set(year, month, day)
                    }.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
            ).show()
        }) { Text("날짜") }
        OutlinedButton(onClick = {
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    onChange(Calendar.getInstance().apply {
                        timeInMillis = value
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true,
            ).show()
        }) { Text("시간") }
        Text(formatter.format(Date(value)), modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ScheduledMessageCard(message: ScheduledMessage, onCancel: () -> Unit) {
    val formatter = remember { SimpleDateFormat("M월 d일 HH:mm", Locale.KOREAN) }
    val statusText = when (message.status) {
        MessageStatus.PENDING -> "대기 중"
        MessageStatus.SENT -> "발송됨"
        MessageStatus.FAILED -> "발송 실패"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(
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
