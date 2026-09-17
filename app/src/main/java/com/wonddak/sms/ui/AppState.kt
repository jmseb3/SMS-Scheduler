package com.wonddak.sms.ui

import androidx.compose.runtime.mutableStateListOf
import com.wonddak.sms.data.AppStore
import com.wonddak.sms.model.MessageStatus
import com.wonddak.sms.model.MessageTemplate
import com.wonddak.sms.model.ScheduledMessage
import com.wonddak.sms.model.SmsContact

class AppState(private val store: AppStore) {
    val contacts = mutableStateListOf<SmsContact>()
    val templates = mutableStateListOf<MessageTemplate>()
    val messages = mutableStateListOf<ScheduledMessage>()

    init {
        reload()
    }

    fun reload() {
        contacts.replaceWith(store.loadContacts())
        templates.replaceWith(store.loadTemplates())
        messages.replaceWith(store.loadScheduledMessages().sortedByDescending { it.sendAtMillis })
    }

    fun saveContact(id: Long?, name: String, phoneNumber: String) {
        val item = SmsContact(id ?: nextId(), name.trim(), phoneNumber.trim())
        contacts.upsert(item) { it.id == item.id }
        store.saveContacts(contacts)
    }

    fun deleteContact(contact: SmsContact) {
        contacts.remove(contact)
        store.saveContacts(contacts)
    }

    fun saveTemplate(id: Long?, title: String, content: String) {
        val item = MessageTemplate(id ?: nextId(), title.trim(), content.trim())
        templates.upsert(item) { it.id == item.id }
        store.saveTemplates(templates)
    }

    fun deleteTemplate(template: MessageTemplate) {
        templates.remove(template)
        store.saveTemplates(templates)
    }

    fun addMessage(message: ScheduledMessage) {
        messages.add(0, message)
        store.saveScheduledMessages(messages)
    }

    fun removePendingMessage(message: ScheduledMessage) {
        if (message.status == MessageStatus.PENDING) {
            messages.remove(message)
            store.saveScheduledMessages(messages)
        }
    }

    fun newId(): Long = nextId()

    private fun nextId(): Long {
        val used = contacts.map { it.id } + templates.map { it.id } + messages.map { it.id }
        return maxOf(System.currentTimeMillis(), (used.maxOrNull() ?: 0L) + 1L)
    }
}

private fun <T> androidx.compose.runtime.snapshots.SnapshotStateList<T>.replaceWith(items: List<T>) {
    clear()
    addAll(items)
}

private fun <T> androidx.compose.runtime.snapshots.SnapshotStateList<T>.upsert(
    item: T,
    predicate: (T) -> Boolean,
) {
    val index = indexOfFirst(predicate)
    if (index >= 0) this[index] = item else add(item)
}
