package com.example.calendarapp.data

import androidx.lifecycle.LiveData
import com.example.calendarapp.model.Event
import java.time.LocalDate

class EventRepository(private val eventDao: EventDao) {

    fun getEventsForDay(date: LocalDate): LiveData<List<Event>> {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.plusDays(1).atStartOfDay()
        return eventDao.getEventsForDay(startOfDay, endOfDay)
    }

    fun getEventById(id: Long): LiveData<Event?> {
        return eventDao.getEventById(id)
    }

    suspend fun insert(event: Event) {
        eventDao.insert(event)
    }

    suspend fun update(event: Event) {
        eventDao.update(event)
    }

    suspend fun delete(event: Event) {
        eventDao.delete(event)
    }

    suspend fun deleteById(eventId: Long) {
        eventDao.deleteById(eventId)
    }

    suspend fun addSampleEventsIfNeeded() {
    }
}