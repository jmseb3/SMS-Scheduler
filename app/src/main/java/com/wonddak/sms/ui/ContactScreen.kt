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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
fun ContactScreen(
    appState: AppState,
    notify: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDetailVisibilityChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<SmsContact?>(null) }
    var showEditor by remember { mutableStateOf(false) }
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

    if (showEditor) {
        ContactEditorScreen(
            initial = editing,
            modifier = modifier,
            onBack = {
                showEditor = false
                onDetailVisibilityChange(false)
            },
            onSave = { name, phone, memo, templateValues ->
                appState.saveContact(editing?.id, name, phone, memo, templateValues)
                showEditor = false
                onDetailVisibilityChange(false)
            },
        )
        return
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
                onClick = {
                    editing = null
                    showEditor = true
                    onDetailVisibilityChange(true)
                },
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
                                TextButton(
                                    onClick = {
                                        editing = contact
                                        showEditor = true
                                        onDetailVisibilityChange(true)
                                    },
                                ) {
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
}
