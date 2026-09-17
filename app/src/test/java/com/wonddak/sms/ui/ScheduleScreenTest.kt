package com.wonddak.sms.ui

import com.wonddak.sms.model.SmsContact
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleScreenTest {
    @Test
    fun `사람 정보에서 이름과 임의 누름틀 기본값을 가져온다`() {
        val contact = SmsContact(
            id = 1L,
            name = "김민지",
            phoneNumber = "010-1234-5678",
            templateValues = mapOf("회사명" to "코덱스", "빈값" to ""),
        )

        val result = contactTemplateValues(
            "{{이름}}님, {{회사명}}에서 {{미등록}} 안내드립니다. {{빈값}}",
            contact,
        )

        assertEquals(mapOf("이름" to "김민지", "회사명" to "코덱스"), result)
    }
}
