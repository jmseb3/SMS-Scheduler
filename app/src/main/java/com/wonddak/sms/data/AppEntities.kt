package com.wonddak.sms.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "contacts", primaryKeys = ["id"])
data class ContactEntity(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val memo: String,
)

@Entity(
    tableName = "contact_template_values",
    primaryKeys = ["contactId", "key"],
    indices = [Index("contactId")],
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ContactTemplateValueEntity(
    val contactId: Long,
    val key: String,
    val value: String,
)

@Entity(tableName = "message_templates", primaryKeys = ["id"])
data class MessageTemplateEntity(
    val id: Long,
    val title: String,
    val content: String,
)

@Entity(
    tableName = "scheduled_messages",
    primaryKeys = ["id"],
    indices = [Index("sendAtMillis"), Index("status")],
)
data class ScheduledMessageEntity(
    val id: Long,
    val contactName: String,
    val phoneNumber: String,
    val templateTitle: String?,
    val content: String,
    val sendAtMillis: Long,
    val status: String,
)

@Entity(
    tableName = "delivery_history",
    primaryKeys = ["id"],
    indices = [Index(value = ["scheduledMessageId"], unique = true), Index("completedAtMillis")],
)
data class DeliveryHistoryEntity(
    val id: Long,
    val scheduledMessageId: Long,
    val contactName: String,
    val phoneNumber: String,
    val content: String,
    val status: String,
    val completedAtMillis: Long,
)
