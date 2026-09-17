package com.wonddak.sms.data

import android.content.Context
import com.wonddak.sms.model.MessageStatus
import com.wonddak.sms.model.MessageTemplate
import com.wonddak.sms.model.ScheduledMessage
import com.wonddak.sms.model.SmsContact
import org.json.JSONArray
import org.json.JSONObject

class AppStore(context: Context) {
    private val preferences = context.getSharedPreferences("sms_scheduler", Context.MODE_PRIVATE)

    fun loadContacts(): List<SmsContact> = readArray(KEY_CONTACTS) { json ->
        SmsContact(json.getLong("id"), json.getString("name"), json.getString("phoneNumber"))
    }

    fun saveContacts(items: List<SmsContact>) = writeArray(KEY_CONTACTS, items) { item ->
        JSONObject().put("id", item.id).put("name", item.name).put("phoneNumber", item.phoneNumber)
    }

    fun loadTemplates(): List<MessageTemplate> = readArray(KEY_TEMPLATES) { json ->
        MessageTemplate(json.getLong("id"), json.getString("title"), json.getString("content"))
    }

    fun saveTemplates(items: List<MessageTemplate>) = writeArray(KEY_TEMPLATES, items) { item ->
        JSONObject().put("id", item.id).put("title", item.title).put("content", item.content)
    }

    fun loadScheduledMessages(): List<ScheduledMessage> = readArray(KEY_MESSAGES) { json ->
        ScheduledMessage(
            id = json.getLong("id"),
            contactName = json.getString("contactName"),
            phoneNumber = json.getString("phoneNumber"),
            templateTitle = json.optString("templateTitle").takeIf { it.isNotBlank() },
            content = json.getString("content"),
            sendAtMillis = json.getLong("sendAtMillis"),
            status = runCatching { MessageStatus.valueOf(json.getString("status")) }
                .getOrDefault(MessageStatus.PENDING),
        )
    }

    fun saveScheduledMessages(items: List<ScheduledMessage>) = writeArray(KEY_MESSAGES, items) { item ->
        JSONObject()
            .put("id", item.id)
            .put("contactName", item.contactName)
            .put("phoneNumber", item.phoneNumber)
            .put("templateTitle", item.templateTitle.orEmpty())
            .put("content", item.content)
            .put("sendAtMillis", item.sendAtMillis)
            .put("status", item.status.name)
    }

    @Synchronized
    fun updateMessageStatus(id: Long, status: MessageStatus) {
        saveScheduledMessages(loadScheduledMessages().map { item ->
            if (item.id == id) item.copy(status = status) else item
        })
    }

    private fun <T> readArray(key: String, mapper: (JSONObject) -> T): List<T> {
        val raw = preferences.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    runCatching { mapper(array.getJSONObject(index)) }.getOrNull()?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun <T> writeArray(key: String, items: List<T>, mapper: (T) -> JSONObject) {
        val array = JSONArray()
        items.forEach { array.put(mapper(it)) }
        preferences.edit().putString(key, array.toString()).apply()
    }

    private companion object {
        const val KEY_CONTACTS = "contacts"
        const val KEY_TEMPLATES = "templates"
        const val KEY_MESSAGES = "messages"
    }
}
