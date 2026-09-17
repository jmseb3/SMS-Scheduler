package com.wonddak.sms.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wonddak.sms.model.SmsContact

@Composable
fun ContactEditorScreen(
    initial: SmsContact?,
    onBack: () -> Unit,
    onSave: (String, String, String, Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
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
    val canSave = name.isNotBlank() && phone.isNotBlank() &&
        !hasInvalidTemplateValue && !hasDuplicateKey && !hasReservedKey

    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                TextButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    Text("연락처 목록")
                }
                ScreenHeader(
                    title = if (initial == null) "새 연락처" else "연락처 수정",
                    description = "예약 발송에 사용할 사람 정보를 입력합니다.",
                )
            }
        }
        item {
            ToolSection(index = "01", title = "기본 정보") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("이름") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("전화번호") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = memo,
                        onValueChange = { memo = it },
                        label = { Text("메모 (선택)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                }
            }
        }
        item {
            ToolSection(index = "02", title = "누름틀 기본값") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "이 사람을 선택하면 같은 이름의 누름틀에 자동으로 입력됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    templateValues.forEachIndexed { index, (key, value) ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = key,
                                onValueChange = { updated ->
                                    templateValues = templateValues.toMutableList().also {
                                        it[index] = updated to value
                                    }
                                },
                                label = { Text("누름틀 이름") },
                                placeholder = { Text("예: 회사명") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = value,
                                onValueChange = { updated ->
                                    templateValues = templateValues.toMutableList().also {
                                        it[index] = key to updated
                                    }
                                },
                                label = { Text("값") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            TextButton(
                                onClick = {
                                    templateValues = templateValues.toMutableList().also { it.removeAt(index) }
                                },
                            ) {
                                Text("누름틀 값 삭제", color = MaterialTheme.colorScheme.error)
                            }
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
            }
        }
        item {
            Button(
                onClick = {
                    onSave(name, phone, memo, templateValues.associate { (key, value) -> key to value })
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(if (initial == null) "연락처 저장" else "변경사항 저장", maxLines = 1)
            }
        }
    }
}
