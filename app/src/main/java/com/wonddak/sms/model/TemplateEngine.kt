package com.wonddak.sms.model

object TemplateEngine {
    private val placeholder = Regex("""\{\{\s*([^{}]+?)\s*\}\}""")

    fun variables(content: String): List<String> = placeholder
        .findAll(content)
        .map { it.groupValues[1].trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .toList()

    fun resolve(content: String, values: Map<String, String>): String =
        placeholder.replace(content) { match ->
            values[match.groupValues[1].trim()] ?: match.value
        }
}
