package com.example.calendarapp.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.calendarapp.model.Event
import java.time.LocalDateTime

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: Event)

    @Update
    suspend fun update(event: Event)

    @Delete
    suspend fun delete(event: Event)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteById(eventId: Long)

    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    fun getEventById(eventId: Long): LiveData<Event?>

    @Query("SELECT * FROM events WHERE startTime >= :dayStart AND startTime < :dayEnd ORDER BY startTime ASC")
    fun getEventsForDay(dayStart: LocalDateTime, dayEnd: LocalDateTime): LiveData<List<Event>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<Event>)
}