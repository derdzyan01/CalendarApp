package com.example.calendarapp.data

import android.app.Application
import androidx.lifecycle.*
import com.example.calendarapp.model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class EventEditViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EventRepository
    private val _eventData = MediatorLiveData<Event?>()
    val eventData: LiveData<Event?> get() = _eventData

    val tempStartTime = MutableLiveData<LocalDateTime>()
    val tempEndTime = MutableLiveData<LocalDateTime>()
    val tempColor = MutableLiveData<Int>()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = EventRepository(database.eventDao())
    }

    fun loadEvent(eventId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val source = repository.getEventById(eventId)
            _eventData.addSource(source) { event ->
                _eventData.postValue(event)
                event?.let {
                    tempStartTime.postValue(it.startTime)
                    tempEndTime.postValue(it.endTime)
                    tempColor.postValue(it.color)
                }
            }
        }
    }

    fun saveEvent(id: Long?, title: String) {
        val start = tempStartTime.value
        val end = tempEndTime.value
        val color = tempColor.value

        if (title.isBlank() || start == null || end == null || color == null) {
            return
        }
        if (end.isBefore(start)) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (id == null || id == -1L) {
                val newEvent = Event(title = title, startTime = start, endTime = end, color = color)
                repository.insert(newEvent)
            } else {
                val updatedEvent = Event(id = id, title = title, startTime = start, endTime = end, color = color)
                repository.update(updatedEvent)
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(event)
        }
    }
    fun deleteEventById(eventId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteById(eventId)
        }
    }
}