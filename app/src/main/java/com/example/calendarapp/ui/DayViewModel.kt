package com.example.calendarapp.ui

import android.app.Application
import androidx.lifecycle.*
import com.example.calendarapp.data.AppDatabase
import com.example.calendarapp.data.EventRepository
import com.example.calendarapp.model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class DayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EventRepository
    private val _selectedDate = MutableLiveData<LocalDate>()
    val selectedDate: LiveData<LocalDate> get() = _selectedDate

    val eventsForSelectedDay: LiveData<List<Event>> = _selectedDate.switchMap { date ->
        repository.getEventsForDay(date)
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = EventRepository(database.eventDao())
        _selectedDate.value = LocalDate.now()

        viewModelScope.launch(Dispatchers.IO) {
            repository.addSampleEventsIfNeeded()
        }
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun goToNextDay() {
        _selectedDate.value = _selectedDate.value?.plusDays(1)
    }

    fun goToPreviousDay() {
        _selectedDate.value = _selectedDate.value?.minusDays(1)
    }

    fun addEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insert(event)
        }
    }
}

