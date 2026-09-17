package com.wonddak.sms.data

import android.content.Context
import com.wonddak.sms.model.DeliveryHistory
import com.wonddak.sms.model.MessageStatus
import com.wonddak.sms.model.MessageTemplate
import com.wonddak.sms.model.ScheduledMessage
import com.wonddak.sms.model.SmsContact
import org.json.JSONArray
import org.json.JSONObject

class AppStore(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val dao = database.appDao()
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyData()
    }

    fun loadContacts(): List<SmsContact> {
        val valuesByContact = dao.getContactTemplateValues()
            .groupBy { it.contactId }
            .mapValues { (_, values) -> values.associate { it.key to it.value } }
        return dao.getContacts().map { entity ->
            SmsContact(
                id = entity.id,
                name = entity.name,
                phoneNumber = entity.phoneNumber,
                memo = entity.memo,
                templateValues = valuesByContact[entity.id].orEmpty(),
            )
        }
    }

    fun saveContacts(items: List<SmsContact>) = database.runInTransaction {
        dao.deleteAllContactTemplateValues()
        dao.deleteAllContacts()
        if (items.isNotEmpty()) dao.insertContacts(items.map(SmsContact::toEntity))
        val values = items.flatMap { contact ->
            contact.templateValues.map { (key, value) ->
                ContactTemplateValueEntity(contact.id, key, value)
            }
        }
        if (values.isNotEmpty()) dao.insertContactTemplateValues(values)
    }

    fun loadTemplates(): List<MessageTemplate> = dao.getTemplates().map { entity ->
        MessageTemplate(entity.id, entity.title, entity.content)
    }

    fun saveTemplates(items: List<MessageTemplate>) = database.runInTransaction {
        dao.deleteAllTemplates()
        if (items.isNotEmpty()) dao.insertTemplates(items.map(MessageTemplate::toEntity))
    }

    fun loadScheduledMessages(): List<ScheduledMessage> = dao.getScheduledMessages().map(
        ScheduledMessageEntity::toModel,
    )

    fun saveScheduledMessages(items: List<ScheduledMessage>) = database.runInTransaction {
        dao.deleteAllScheduledMessages()
        if (items.isNotEmpty()) dao.insertScheduledMessages(items.map(ScheduledMessage::toEntity))
    }

    fun loadDeliveryHistory(): List<DeliveryHistory> = dao.getDeliveryHistory().map { entity ->
        DeliveryHistory(
            id = entity.id,
            scheduledMessageId = entity.scheduledMessageId,
            contactName = entity.contactName,
            phoneNumber = entity.phoneNumber,
            content = entity.content,
            status = entity.status.toMessageStatus(),
            completedAtMillis = entity.completedAtMillis,
        )
    }

    @Synchronized
    fun updateMessageStatus(id: Long, status: MessageStatus) = database.runInTransaction {
        val message = dao.getScheduledMessage(id) ?: return@runInTransaction
        dao.updateMessageStatus(id, status.name)
        if (status != MessageStatus.PENDING) {
            dao.insertDeliveryHistory(
                DeliveryHistoryEntity(
                    id = id,
                    scheduledMessageId = id,
                    contactName = message.contactName,
                    phoneNumber = message.phoneNumber,
                    content = message.content,
                    status = status.name,
                    completedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun migrateLegacyData() = synchronized(migrationLock) {
        if (preferences.getBoolean(KEY_ROOM_MIGRATED, false)) return@synchronized

        val contacts = readLegacyArray(KEY_CONTACTS) { json ->
            val templateValues = json.optJSONObject("templateValues")?.let { values ->
                values.keys().asSequence().associateWith { key -> values.optString(key) }
            }.orEmpty()
            SmsContact(
                id = json.getLong("id"),
                name = json.getString("name"),
                phoneNumber = json.getString("phoneNumber"),
                memo = json.optString("memo"),
                templateValues = templateValues,
            )
        }
        val templates = readLegacyArray(KEY_TEMPLATES) { json ->
            MessageTemplate(json.getLong("id"), json.getString("title"), json.getString("content"))
        }
        val messages = readLegacyArray(KEY_MESSAGES) { json ->
            ScheduledMessage(
                id = json.getLong("id"),
                contactName = json.getString("contactName"),
                phoneNumber = json.getString("phoneNumber"),
                templateTitle = json.optString("templateTitle").takeIf(String::isNotBlank),
                content = json.getString("content"),
                sendAtMillis = json.getLong("sendAtMillis"),
                status = json.optString("status").toMessageStatus(),
            )
        }

        database.runInTransaction {
            if (dao.getContacts().isEmpty() && contacts.isNotEmpty()) {
                dao.insertContacts(contacts.map(SmsContact::toEntity))
                val templateValues = contacts.flatMap { contact ->
                    contact.templateValues.map { (key, value) ->
                        ContactTemplateValueEntity(contact.id, key, value)
                    }
                }
                if (templateValues.isNotEmpty()) dao.insertContactTemplateValues(templateValues)
            }
            if (dao.getTemplates().isEmpty() && templates.isNotEmpty()) {
                dao.insertTemplates(templates.map(MessageTemplate::toEntity))
            }
            if (dao.getScheduledMessages().isEmpty() && messages.isNotEmpty()) {
                dao.insertScheduledMessages(messages.map(ScheduledMessage::toEntity))
                messages.filter { it.status != MessageStatus.PENDING }.forEach { message ->
                    dao.insertDeliveryHistory(
                        DeliveryHistoryEntity(
                            id = message.id,
                            scheduledMessageId = message.id,
                            contactName = message.contactName,
                            phoneNumber = message.phoneNumber,
                            content = message.content,
                            status = message.status.name,
                            completedAtMillis = message.sendAtMillis,
                        ),
                    )
                }
            }
        }
        preferences.edit().putBoolean(KEY_ROOM_MIGRATED, true).apply()
    }

    private fun <T> readLegacyArray(key: String, mapper: (JSONObject) -> T): List<T> {
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

    private companion object {
        const val PREFERENCES_NAME = "sms_scheduler"
        const val KEY_CONTACTS = "contacts"
        const val KEY_TEMPLATES = "templates"
        const val KEY_MESSAGES = "messages"
        const val KEY_ROOM_MIGRATED = "room_migrated_v1"
        val migrationLock = Any()
    }
}

private fun SmsContact.toEntity() = ContactEntity(id, name, phoneNumber, memo)

private fun MessageTemplate.toEntity() = MessageTemplateEntity(id, title, content)

private fun ScheduledMessage.toEntity() = ScheduledMessageEntity(
    id = id,
    contactName = contactName,
    phoneNumber = phoneNumber,
    templateTitle = templateTitle,
    content = content,
    sendAtMillis = sendAtMillis,
    status = status.name,
)

private fun ScheduledMessageEntity.toModel() = ScheduledMessage(
    id = id,
    contactName = contactName,
    phoneNumber = phoneNumber,
    templateTitle = templateTitle,
    content = content,
    sendAtMillis = sendAtMillis,
    status = status.toMessageStatus(),
)

private fun String.toMessageStatus(): MessageStatus = runCatching {
    MessageStatus.valueOf(this)
}.getOrDefault(MessageStatus.PENDING)
