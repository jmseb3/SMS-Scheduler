package com.wonddak.sms.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateEditorScreenTest {
    @Test
    fun `누름틀을 커서 위치에 삽입하고 중괄호 사이로 이동한다`() {
        val result = insertPlaceholder(TextFieldValue("안녕하세요", TextRange(2)))

        assertEquals("안녕{{}}하세요", result.text)
        assertEquals(TextRange(4), result.selection)
    }

    @Test
    fun `선택한 텍스트를 누름틀로 교체한다`() {
        val result = insertPlaceholder(TextFieldValue("고객 이름", TextRange(3, 5)))

        assertEquals("고객 {{}}", result.text)
        assertEquals(TextRange(5), result.selection)
    }
}
