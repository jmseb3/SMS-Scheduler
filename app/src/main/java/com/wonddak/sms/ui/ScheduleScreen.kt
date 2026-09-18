package com.wonddak.sms.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import java.util.TimeZone

@Composable
fun ScheduleScreen(
    appState: AppState,
    scheduler: MessageAlarmScheduler,
    notify: (String) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
) {
    val context = LocalContext.current
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
        modifier = modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                title = "새 예약",
                description = "수신자, 메시지, 발송 시각을 순서대로 설정합니다.",
                modifier = Modifier.padding(top = 12.dp),
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CountPill("${appState.messages.count { it.status == MessageStatus.PENDING }}건 대기")
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "설정")
                        }
                    }
                },
            )
        }
        item { SystemReadinessBanner(onOpenSettings = onOpenSettings) }
        item {
            ToolSection(index = "01", title = "수신자") {
                Selector(
                    label = "받는 사람",
                    selectedText = selectedContact?.let { "${it.name} · ${it.phoneNumber}" },
                    emptyText = if (appState.contacts.isEmpty()) "연락처 탭에서 먼저 추가하세요" else "연락처 선택",
                    items = appState.contacts,
                    itemText = { "${it.name} · ${it.phoneNumber}" },
                    onSelected = {
                        selectedContact = it
                        variableValues.clear()
                        variableValues.putAll(contactTemplateValues(content, it))
                    },
                )
            }
        }
        item {
            ToolSection(index = "02", title = "메시지") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        selectedContact?.let { contact ->
                            variableValues.putAll(contactTemplateValues(it.content, contact))
                        }
                    },
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { updated ->
                        content = updated
                        val variablesInContent = TemplateEngine.variables(updated).toSet()
                        variableValues.keys.toList()
                            .filterNot(variablesInContent::contains)
                            .forEach(variableValues::remove)
                        selectedContact?.let { contact ->
                            contactTemplateValues(updated, contact).forEach { (key, value) ->
                                variableValues.putIfAbsent(key, value)
                            }
                        }
                    },
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
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "치환 미리보기",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            TemplateEngine.resolve(content, variableValues),
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                }
            }
        }
        item {
            ToolSection(index = "03", title = "발송 시각") {
                DateTimeChooser(sendAtMillis) { sendAtMillis = it }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusTag(
                    text = if (canSchedule) "예약 가능" else "필수 항목을 확인하세요",
                    color = if (canSchedule) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text("문자 발송 예약", maxLines = 1) }
            }
        }
    }
}

internal fun contactTemplateValues(content: String, contact: SmsContact): Map<String, String> =
    TemplateEngine.variables(content).mapNotNull { variable ->
        val value = if (variable == "이름") contact.name else contact.templateValues[variable]
        value?.takeIf(String::isNotBlank)?.let { variable to it }
    }.toMap()

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
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = selectedText ?: emptyText,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("↓", style = MaterialTheme.typography.labelMedium)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeChooser(value: Long, onChange: (Long) -> Unit) {
    val calendar = remember(value) { Calendar.getInstance().apply { timeInMillis = value } }
    val formatter = remember { SimpleDateFormat("yyyy년 M월 d일 (E) HH:mm", Locale.KOREAN) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("예약 발송 시각", style = MaterialTheme.typography.labelMedium)
        Text(
            formatter.format(Date(value)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        OutlinedButton(
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            onClick = { showDatePicker = true },
        ) { Text("날짜 변경", maxLines = 1) }
        OutlinedButton(
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            onClick = { showTimePicker = true },
        ) { Text("시간 변경", maxLines = 1) }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = pickerDateMillis(value))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selected ->
                            onChange(withSelectedDate(value, selected))
                        }
                        showDatePicker = false
                    },
                ) { Text("적용") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("시간 선택") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onChange(withSelectedTime(value, timePickerState.hour, timePickerState.minute))
                        showTimePicker = false
                    },
                ) { Text("적용") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("취소") }
            },
        )
    }
}

private fun pickerDateMillis(value: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = value }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun withSelectedDate(value: Long, selectedDateMillis: Long): Long {
    val selected = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = selectedDateMillis
    }
    return Calendar.getInstance().apply {
        timeInMillis = value
        set(
            selected.get(Calendar.YEAR),
            selected.get(Calendar.MONTH),
            selected.get(Calendar.DAY_OF_MONTH),
        )
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun withSelectedTime(value: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = value
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
