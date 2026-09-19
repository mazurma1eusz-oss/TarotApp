package com.mazur.tarot.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {

    @Insert
    suspend fun insert(reading: ReadingEntity): Long

    @Update
    suspend fun update(reading: ReadingEntity)

    @Delete
    suspend fun delete(reading: ReadingEntity)

    @Query("SELECT * FROM readings ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE timestampMillis >= :sinceMillis ORDER BY timestampMillis DESC")
    fun observeSince(sinceMillis: Long): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE spreadType = :spreadType AND timestampMillis >= :sinceMillis ORDER BY timestampMillis DESC LIMIT 1")
    suspend fun findLatestOfType(spreadType: String, sinceMillis: Long): ReadingEntity?

    @Query("UPDATE readings SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    @Query("UPDATE readings SET aiResponseText = :aiResponseText, followUpsCsv = :followUpsCsv WHERE id = :id")
    suspend fun updateAiResponseAndFollowUps(id: Long, aiResponseText: String?, followUpsCsv: String?)

    @Query("DELETE FROM readings")
    suspend fun deleteAll()
}
