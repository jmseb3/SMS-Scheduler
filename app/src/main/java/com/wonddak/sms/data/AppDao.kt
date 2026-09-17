package com.wonddak.sms.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppDao {
    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE")
    fun getContacts(): List<ContactEntity>

    @Query("SELECT * FROM contact_template_values ORDER BY key COLLATE NOCASE")
    fun getContactTemplateValues(): List<ContactTemplateValueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertContacts(items: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertContactTemplateValues(items: List<ContactTemplateValueEntity>)

    @Query("DELETE FROM contact_template_values")
    fun deleteAllContactTemplateValues()

    @Query("DELETE FROM contacts")
    fun deleteAllContacts()

    @Query("SELECT * FROM message_templates ORDER BY title COLLATE NOCASE")
    fun getTemplates(): List<MessageTemplateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTemplates(items: List<MessageTemplateEntity>)

    @Query("DELETE FROM message_templates")
    fun deleteAllTemplates()

    @Query("SELECT * FROM scheduled_messages ORDER BY sendAtMillis DESC")
    fun getScheduledMessages(): List<ScheduledMessageEntity>

    @Query("SELECT * FROM scheduled_messages WHERE id = :id LIMIT 1")
    fun getScheduledMessage(id: Long): ScheduledMessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertScheduledMessages(items: List<ScheduledMessageEntity>)

    @Query("DELETE FROM scheduled_messages")
    fun deleteAllScheduledMessages()

    @Query("UPDATE scheduled_messages SET status = :status WHERE id = :id")
    fun updateMessageStatus(id: Long, status: String)

    @Query("SELECT * FROM delivery_history ORDER BY completedAtMillis DESC")
    fun getDeliveryHistory(): List<DeliveryHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDeliveryHistory(item: DeliveryHistoryEntity)
}
