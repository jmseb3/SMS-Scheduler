package com.wonddak.sms.model

data class SmsContact(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val memo: String = "",
    val templateValues: Map<String, String> = emptyMap(),
)

data class MessageTemplate(
    val id: Long,
    val title: String,
    val content: String,
)

enum class MessageStatus {
    PENDING,
    SENT,
    FAILED,
}

data class ScheduledMessage(
    val id: Long,
    val contactName: String,
    val phoneNumber: String,
    val templateTitle: String?,
    val content: String,
    val sendAtMillis: Long,
    val status: MessageStatus = MessageStatus.PENDING,
)

data class DeliveryHistory(
    val id: Long,
    val scheduledMessageId: Long,
    val contactName: String,
    val phoneNumber: String,
    val content: String,
    val status: MessageStatus,
    val completedAtMillis: Long,
)
