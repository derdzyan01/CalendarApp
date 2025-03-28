package com.example.calendarapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import com.example.calendarapp.data.EventEditViewModel
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar

class EventEditActivity : AppCompatActivity() {

    private lateinit var viewModel: EventEditViewModel
    private lateinit var editTextTitle: TextInputEditText
    private lateinit var buttonStartDate: Button
    private lateinit var buttonStartTime: Button
    private lateinit var buttonEndDate: Button
    private lateinit var buttonEndTime: Button
    private lateinit var buttonSave: Button
    private lateinit var buttonCancel: Button
    private lateinit var buttonDelete: Button
    private lateinit var colorPickerLayout: LinearLayout

    private var currentEventId: Long? = null
    private var initialDate: LocalDate? = null

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    companion object {
        const val EXTRA_EVENT_ID = "com.example.calendarapp.EXTRA_EVENT_ID"
        const val EXTRA_SELECTED_DATE = "com.example.calendarapp.EXTRA_SELECTED_DATE"
        const val NEW_EVENT_ID = -1L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_edit)

        viewModel = ViewModelProvider(this)[EventEditViewModel::class.java]

        findViews()

        currentEventId = intent.getLongExtra(EXTRA_EVENT_ID, NEW_EVENT_ID)
        val selectedEpochDay = intent.getLongExtra(EXTRA_SELECTED_DATE, -1L)
        if (selectedEpochDay != -1L) {
            initialDate = LocalDate.ofEpochDay(selectedEpochDay)
        }

        if (currentEventId == NEW_EVENT_ID) {
            title = "Create Event"
            setupNewEventDefaults()
            buttonDelete.visibility = View.GONE
        } else {
            title = "Edit Event"
            buttonDelete.visibility = View.VISIBLE
            currentEventId?.let { viewModel.loadEvent(it) }
        }

        observeViewModel()
        setupButtonClickListeners()
        setupColorPicker()
    }

    private fun findViews() {
        editTextTitle = findViewById(R.id.editTextEventTitle)
        buttonStartDate = findViewById(R.id.buttonStartDate)
        buttonStartTime = findViewById(R.id.buttonStartTime)
        buttonEndDate = findViewById(R.id.buttonEndDate)
        buttonEndTime = findViewById(R.id.buttonEndTime)
        buttonSave = findViewById(R.id.buttonSaveEvent)
        buttonCancel = findViewById(R.id.buttonCancelEvent)
        buttonDelete = findViewById(R.id.buttonDeleteEvent)
        colorPickerLayout = findViewById(R.id.colorPickerLayout)
    }

    private fun setupNewEventDefaults() {
        val defaultStart: LocalDateTime
        val defaultEnd: LocalDateTime

        if (initialDate != null) {
            val now = LocalTime.now()
            var startHour = now.hour
            if (now.minute > 0 || now.second > 0) {
                startHour += 1
            }
            if (startHour > 23) startHour = 23
            defaultStart = initialDate!!.atTime(startHour, 0)
            defaultEnd = defaultStart.plusMinutes(20)
        } else {
            defaultStart = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0)
            defaultEnd = defaultStart.plusMinutes(20)
        }

        viewModel.tempStartTime.value = defaultStart
        viewModel.tempEndTime.value = defaultEnd
        viewModel.tempColor.value = Color.parseColor("#4285F4")
    }


    private fun observeViewModel() {
        viewModel.eventData.observe(this) { event ->
            if (currentEventId != NEW_EVENT_ID && event != null) {
                editTextTitle.setText(event.title)
                if (viewModel.tempStartTime.value == null) {
                    viewModel.tempStartTime.value = event.startTime
                    viewModel.tempEndTime.value = event.endTime
                    viewModel.tempColor.value = event.color
                }
            }
            observeTempValues()
        }
        observeTempValues()
    }

    private fun observeTempValues(){
        viewModel.tempStartTime.observe(this) { dateTime ->
            dateTime?.let {
                buttonStartDate.text = it.format(dateFormatter)
                buttonStartTime.text = it.format(timeFormatter)
            }
        }
        viewModel.tempEndTime.observe(this) { dateTime ->
            dateTime?.let {
                buttonEndDate.text = it.format(dateFormatter)
                buttonEndTime.text = it.format(timeFormatter)
            }
        }
        viewModel.tempColor.observe(this) { color ->
            updateColorSelectionUI(color)
        }
    }

    private fun setupButtonClickListeners() {
        buttonStartDate.setOnClickListener { showDatePicker(true) }
        buttonStartTime.setOnClickListener { showTimePicker(true) }
        buttonEndDate.setOnClickListener { showDatePicker(false) }
        buttonEndTime.setOnClickListener { showTimePicker(false) }

        buttonSave.setOnClickListener {
            viewModel.saveEvent(
                if (currentEventId == NEW_EVENT_ID) null else currentEventId,
                editTextTitle.text.toString()
            )
            Toast.makeText(this, "Event Saved", Toast.LENGTH_SHORT).show()
            finish()
        }

        buttonCancel.setOnClickListener {
            finish()
        }

        buttonDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupColorPicker() {
        findViewById<View>(R.id.colorOptionBlue).setOnClickListener { viewModel.tempColor.value = Color.parseColor("#4285F4") }
        findViewById<View>(R.id.colorOptionGreen).setOnClickListener { viewModel.tempColor.value = Color.parseColor("#34A853") }
        findViewById<View>(R.id.colorOptionYellow).setOnClickListener { viewModel.tempColor.value = Color.parseColor("#FBBC05") }
        findViewById<View>(R.id.colorOptionRed).setOnClickListener { viewModel.tempColor.value = Color.parseColor("#EA4335") }
    }

    private fun updateColorSelectionUI(selectedColor: Int?) {
        colorPickerLayout.children.forEach { view ->
            val bgColor = (view.background as? ColorDrawable)?.color
            if (bgColor != null && bgColor == selectedColor) {
                view.alpha = 1.0f
            } else {
                view.alpha = 0.7f
            }
        }
    }

    private fun showDatePicker(isStart: Boolean) {
        val currentDateTime = if (isStart) viewModel.tempStartTime.value else viewModel.tempEndTime.value
        val calendar = Calendar.getInstance()
        currentDateTime?.let {
            calendar.timeInMillis = it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                val currentTime = currentDateTime?.toLocalTime() ?: LocalTime.MIDNIGHT
                val newDateTime = selectedDate.atTime(currentTime)
                if (isStart) {
                    viewModel.tempStartTime.value = newDateTime
                    val suggestedEndTime = newDateTime.plusMinutes(20)
                    viewModel.tempEndTime.value = suggestedEndTime
                } else {
                    if(viewModel.tempStartTime.value != null && newDateTime.isBefore(viewModel.tempStartTime.value)){
                        Toast.makeText(this, "End time cannot be before start time", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.tempEndTime.value = newDateTime
                    }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(isStart: Boolean) {
        val currentDateTime = if (isStart) viewModel.tempStartTime.value else viewModel.tempEndTime.value
        val calendar = Calendar.getInstance()
        currentDateTime?.let {
            calendar.timeInMillis = it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = LocalTime.of(hourOfDay, minute)
                val currentDate = currentDateTime?.toLocalDate() ?: (if(isStart) viewModel.tempStartTime.value?.toLocalDate() else viewModel.tempEndTime.value?.toLocalDate()) ?: LocalDate.now()
                val newDateTime = currentDate.atTime(selectedTime)

                if (isStart) {
                    viewModel.tempStartTime.value = newDateTime
                    val suggestedEndTime = newDateTime.plusMinutes(20)
                    viewModel.tempEndTime.value = suggestedEndTime
                } else {
                    if(viewModel.tempStartTime.value != null && newDateTime.isBefore(viewModel.tempStartTime.value)){
                        Toast.makeText(this, "End time cannot be before start time", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.tempEndTime.value = newDateTime
                    }
                }
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event?")
            .setPositiveButton("Delete") { _, _ ->
                currentEventId?.let {
                    viewModel.deleteEventById(it)
                    Toast.makeText(this, "Event Deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}