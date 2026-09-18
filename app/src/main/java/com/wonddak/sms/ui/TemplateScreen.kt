package com.wonddak.sms.ui

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wonddak.sms.model.MessageTemplate
import com.wonddak.sms.model.TemplateEngine

@Composable
fun TemplateScreen(
    appState: AppState,
    modifier: Modifier = Modifier,
    onDetailVisibilityChange: (Boolean) -> Unit = {},
) {
    var editing by remember { mutableStateOf<MessageTemplate?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    if (showEditor) {
        TemplateEditorScreen(
            initial = editing,
            modifier = modifier,
            onBack = {
                showEditor = false
                onDetailVisibilityChange(false)
            },
            onSave = { title, content ->
                appState.saveTemplate(editing?.id, title, content)
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
            title = "템플릿",
            description = "반복 문구와 치환 변수를 관리합니다.",
            trailing = { CountPill("${appState.templates.size}개") },
        )
        Button(
            onClick = {
                editing = null
                showEditor = true
                onDetailVisibilityChange(true)
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text("새 템플릿", maxLines = 1) }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = MaterialTheme.shapes.small,
        ) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    "VARIABLE SYNTAX",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "{{이름}}, {{예약일}}처럼 입력하면 예약 시 실제 값으로 치환합니다.",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (appState.templates.isEmpty()) {
            EmptyHint("등록된 템플릿이 없습니다.\n자주 쓰는 안내 문구를 저장해 보세요.", Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(appState.templates, key = { it.id }) { template ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                template.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(template.content, modifier = Modifier.padding(top = 8.dp))
                            val variables = TemplateEngine.variables(template.content)
                            if (variables.isNotEmpty()) {
                                Text(
                                    "변수 · ${variables.joinToString()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(
                                    onClick = {
                                        editing = template
                                        showEditor = true
                                        onDetailVisibilityChange(true)
                                    },
                                ) {
                                    Text("수정", maxLines = 1)
                                }
                                TextButton(onClick = { appState.deleteTemplate(template) }) {
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
