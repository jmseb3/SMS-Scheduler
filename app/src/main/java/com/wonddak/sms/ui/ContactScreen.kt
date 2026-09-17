package com.wonddak.sms.ui

import android.app.Activity
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wonddak.sms.model.SmsContact

@Composable
fun ContactScreen(appState: AppState, notify: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<SmsContact?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val contactPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            context.contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0).orEmpty()
                    val phone = cursor.getString(1).orEmpty()
                    appState.saveContact(null, name.ifBlank { phone }, phone)
                    notify("기기 연락처를 추가했습니다.")
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = "연락처",
            description = "예약 발송 대상과 전화번호를 관리합니다.",
            trailing = { CountPill("${appState.contacts.size}명") },
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { editing = null; showDialog = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("연락처 직접 추가", maxLines = 1) }
            OutlinedButton(
                onClick = {
                    contactPicker.launch(
                        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                    )
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("기기 연락처 가져오기", maxLines = 1) }
        }
        if (appState.contacts.isEmpty()) {
            EmptyHint(
                "등록된 연락처가 없습니다.\n직접 추가하거나 기기 연락처에서 선택하세요.",
                Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(appState.contacts, key = { it.id }) { contact ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                contact.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                contact.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (contact.memo.isNotBlank()) {
                                Text(
                                    contact.memo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (contact.templateValues.isNotEmpty()) {
                                Text(
                                    "누름틀 값 · " + contact.templateValues.entries.joinToString(" · ") {
                                        (key, value) -> "$key: $value"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { editing = contact; showDialog = true }) {
                                    Text("수정", maxLines = 1)
                                }
                                TextButton(onClick = { appState.deleteContact(contact) }) {
                                    Text("삭제", maxLines = 1, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ContactDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { name, phone, memo, templateValues ->
                appState.saveContact(editing?.id, name, phone, memo, templateValues)
                showDialog = false
            },
        )
    }
}

@Composable
private fun ContactDialog(
    initial: SmsContact?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Map<String, String>) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var phone by remember(initial) { mutableStateOf(initial?.phoneNumber.orEmpty()) }
    var memo by remember(initial) { mutableStateOf(initial?.memo.orEmpty()) }
    var templateValues by remember(initial) {
        mutableStateOf(initial?.templateValues?.entries?.map { it.key to it.value }.orEmpty())
    }
    val hasInvalidTemplateValue = templateValues.any { (key, value) ->
        key.isBlank() || value.isBlank()
    }
    val hasDuplicateKey = templateValues.map { it.first.trim() }.distinct().size != templateValues.size
    val hasReservedKey = templateValues.any { it.first.trim() == "이름" }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "연락처 추가" else "연락처 수정") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("이름") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("전화번호") }, singleLine = true)
                OutlinedTextField(
                    memo,
                    { memo = it },
                    label = { Text("메모 (선택)") },
                    minLines = 2,
                )
                Text("누름틀 기본값", style = MaterialTheme.typography.labelMedium)
                Text(
                    "이 사람을 선택하면 같은 이름의 누름틀에 자동으로 입력됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                templateValues.forEachIndexed { index, (key, value) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            key,
                            { updated ->
                                templateValues = templateValues.toMutableList().also {
                                    it[index] = updated to value
                                }
                            },
                            label = { Text("누름틀 이름") },
                            placeholder = { Text("예: 회사명") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value,
                            { updated ->
                                templateValues = templateValues.toMutableList().also {
                                    it[index] = key to updated
                                }
                            },
                            label = { Text("값") },
                            singleLine = true,
                        )
                        TextButton(
                            onClick = {
                                templateValues = templateValues.toMutableList().also { it.removeAt(index) }
                            },
                        ) { Text("누름틀 값 삭제", color = MaterialTheme.colorScheme.error) }
                    }
                }
                OutlinedButton(onClick = { templateValues = templateValues + ("" to "") }) {
                    Text("누름틀 값 추가")
                }
                if (hasDuplicateKey) {
                    Text(
                        "누름틀 이름은 중복될 수 없습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (hasReservedKey) {
                    Text(
                        "{{이름}} 값은 위 이름 항목에서 자동으로 가져옵니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && phone.isNotBlank() &&
                    !hasInvalidTemplateValue && !hasDuplicateKey && !hasReservedKey,
                onClick = {
                    onSave(name, phone, memo, templateValues.associate { (key, value) -> key to value })
                },
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
