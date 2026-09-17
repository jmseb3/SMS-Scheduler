package com.wonddak.sms.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.wonddak.sms.model.MessageTemplate
import com.wonddak.sms.model.TemplateEngine

@Composable
fun TemplateEditorScreen(
    initial: MessageTemplate?,
    onBack: () -> Unit,
    onSave: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var content by remember(initial) {
        mutableStateOf(TextFieldValue(initial?.content.orEmpty()))
    }
    val contentFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val variables = TemplateEngine.variables(content.text)
    val isPlaceholderBeingEdited = EMPTY_PLACEHOLDER.containsMatchIn(content.text)
    val placeholderSummary = buildList {
        addAll(variables.map { "{{$it}}" })
        if (isPlaceholderBeingEdited) add("이름 입력 중")
    }

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
                    Text("템플릿 목록")
                }
                ScreenHeader(
                    title = if (initial == null) "새 템플릿" else "템플릿 수정",
                    description = "반복 문구와 누름틀을 작성합니다.",
                )
            }
        }
        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("템플릿 제목") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("메시지 내용", style = MaterialTheme.typography.labelMedium)
                    OutlinedButton(
                        modifier = Modifier.heightIn(min = 48.dp),
                        onClick = {
                            content = insertPlaceholder(content)
                            contentFocusRequester.requestFocus()
                            keyboardController?.show()
                        },
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text("누름틀", maxLines = 1)
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("템플릿 메시지") },
                    placeholder = { Text("{{이름}}님, 예약 안내입니다.") },
                    modifier = Modifier.fillMaxWidth().focusRequester(contentFocusRequester),
                    minLines = 8,
                )
                Text(
                    text = if (placeholderSummary.isNotEmpty()) {
                        "현재 누름틀 · ${placeholderSummary.joinToString(" · ")}"
                    } else {
                        "현재 누름틀 · 없음"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (placeholderSummary.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    "누름틀 버튼을 누른 뒤 중괄호 안에 이름을 입력하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Button(
                onClick = { onSave(title.trim(), content.text.trim()) },
                enabled = title.isNotBlank() && content.text.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(if (initial == null) "템플릿 저장" else "변경사항 저장", maxLines = 1)
            }
        }
    }
}

internal fun insertPlaceholder(value: TextFieldValue): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(start, value.text.length)
    val updated = value.text.replaceRange(start, end, "{{}}")
    return TextFieldValue(updated, TextRange(start + 2))
}

private val EMPTY_PLACEHOLDER = Regex("""\{\{\s*\}\}""")
