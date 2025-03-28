package com.example.calendarapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val color: Int
)