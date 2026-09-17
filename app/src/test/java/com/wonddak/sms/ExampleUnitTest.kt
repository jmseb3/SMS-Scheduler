package com.wonddak.sms

import com.wonddak.sms.model.TemplateEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateEngineTest {
    @Test
    fun extractsDistinctVariablesInOrder() {
        assertEquals(
            listOf("이름", "예약일"),
            TemplateEngine.variables("{{ 이름 }}님, {{예약일}} 예약입니다. {{이름}}님"),
        )
    }

    @Test
    fun replacesKnownVariablesAndKeepsUnknownOnes() {
        assertEquals(
            "홍길동님, {{예약일}} 예약입니다.",
            TemplateEngine.resolve("{{이름}}님, {{예약일}} 예약입니다.", mapOf("이름" to "홍길동")),
        )
    }

    @Test
    fun ignoresIncompleteBraces() {
        assertEquals(emptyList<String>(), TemplateEngine.variables("{이름} {{예약일}"))
    }
}
