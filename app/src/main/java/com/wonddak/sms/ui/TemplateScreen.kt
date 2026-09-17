package com.wonddak.sms.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wonddak.sms.model.MessageTemplate
import com.wonddak.sms.model.TemplateEngine

@Composable
fun TemplateScreen(appState: AppState, modifier: Modifier = Modifier) {
    var editing by remember { mutableStateOf<MessageTemplate?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader(
            title = "템플릿",
            description = "반복해서 보내는 문장을 한 번 저장해 두세요.",
            trailing = { CountPill("${appState.templates.size}개") },
        )
        Button(onClick = { editing = null; showDialog = true }) { Text("템플릿 추가") }
        Text(
            "메시지에 {{이름}}, {{예약일}}처럼 입력하면 예약할 때 값을 채울 수 있습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (appState.templates.isEmpty()) {
            EmptyHint("등록된 템플릿이 없습니다.\n자주 쓰는 안내 문구를 저장해 보세요.", Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(appState.templates, key = { it.id }) { template ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(template.title, style = MaterialTheme.typography.titleMedium)
                            Text(template.content, modifier = Modifier.padding(top = 6.dp))
                            val variables = TemplateEngine.variables(template.content)
                            if (variables.isNotEmpty()) {
                                Text(
                                    "누름틀: ${variables.joinToString()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { editing = template; showDialog = true }) { Text("수정") }
                                TextButton(onClick = { appState.deleteTemplate(template) }) { Text("삭제") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        TemplateDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { title, content ->
                appState.saveTemplate(editing?.id, title, content)
                showDialog = false
            },
        )
    }
}

@Composable
private fun TemplateDialog(
    initial: MessageTemplate?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember(initial) { mutableStateOf(initial?.content.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "템플릿 추가" else "템플릿 수정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("제목") }, singleLine = true)
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("내용") },
                    placeholder = { Text("{{이름}}님, 예약 안내입니다.") },
                    minLines = 4,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && content.isNotBlank(),
                onClick = { onSave(title, content) },
            ) { Text("저장") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
