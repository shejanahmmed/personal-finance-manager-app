package com.shejan.financebuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsSenderMappingDao {
    @Query("SELECT * FROM sms_sender_mappings")
    fun getAllMappingsOnce(): List<SmsSenderMappingEntity>

    @Query("SELECT * FROM sms_sender_mappings")
    fun getAllMappingsFlow(): Flow<List<SmsSenderMappingEntity>>

    @Query("SELECT * FROM sms_sender_mappings WHERE senderAddress = :senderAddress LIMIT 1")
    fun getMappingForSenderOnce(senderAddress: String): SmsSenderMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: SmsSenderMappingEntity)

    @Update
    suspend fun updateMapping(mapping: SmsSenderMappingEntity)

    @Delete
    suspend fun deleteMapping(mapping: SmsSenderMappingEntity)
}
