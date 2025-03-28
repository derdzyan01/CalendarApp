package com.example.calendarapp

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.calendarapp.model.Event
import com.example.calendarapp.ui.DayView
import com.example.calendarapp.ui.DayViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity(), DayView.OnEventClickListener {

    // --- ViewModel ---
    private lateinit var viewModel: DayViewModel

    // --- UI Elements ---
    private var dayView: DayView? = null
    private var textViewSelectedDate: TextView? = null
    private var buttonPreviousDay: Button? = null
    private var buttonNextDay: Button? = null
    private var fabAddEvent: FloatingActionButton? = null

    // --- Formatting ---
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    // --- Biometric ---
    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var isAppSetupComplete = false

    // --- Constants ---
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_ENROLLMENT = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate")

        executor = ContextCompat.getMainExecutor(this)
        setupBiometricPrompt()

        checkBiometricSupportAndAuthenticate()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")

        if (!isAppSetupComplete) {
            Log.d(TAG, "App not setup, re-checking biometric status in onResume.")
            checkBiometricSupportAndAuthenticate()
        }
    }


    // --- Biometric Implementation ---

    private fun setupBiometricPrompt() {
        Log.d(TAG, "setupBiometricPrompt")
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(TAG, "Biometric Authentication error: [$errorCode] $errString")
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        Toast.makeText(applicationContext, "Authentication cancelled", Toast.LENGTH_SHORT).show()
                        finishAffinity()
                    } else {
                        Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_LONG).show()
                        finishAffinity()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.i(TAG, "Biometric Authentication succeeded!")
                    Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                    runOnUiThread {
                        if (!isAppSetupComplete) {
                            Log.d(TAG, "Setting up App UI after successful authentication.")
                            setupApp()
                            isAppSetupComplete = true
                        } else {
                            Log.d(TAG, "App UI already setup, skipping setupApp().")
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "Biometric Authentication failed (e.g., fingerprint not recognized).")
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for Calendar App")
            .setSubtitle("Log in using your biometric credential or device screen lock")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
    }

    private fun checkBiometricSupportAndAuthenticate() {
        Log.d(TAG, "checkBiometricSupportAndAuthenticate")
        if (isAppSetupComplete) {
            Log.d(TAG, "App setup is already complete, skipping biometric check.")
            return
        }

        val biometricManager = BiometricManager.from(this)
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Log.i(TAG, "Biometric/Credential authentication available. Launching prompt.")
                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Log.e(TAG, "No biometric credentials enrolled.")
                showEnrollmentDialog()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Log.e(TAG, "No biometric features available.")
                Toast.makeText(this, "Biometric hardware not available. Access granted without lock.", Toast.LENGTH_LONG).show()
                if (!isAppSetupComplete) { setupApp(); isAppSetupComplete = true }
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Log.e(TAG, "Biometric features currently unavailable.")
                Toast.makeText(this, "Biometric features currently unavailable. Please try again later.", Toast.LENGTH_LONG).show()
                finishAffinity()
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                Log.e(TAG, "Biometric security update required.")
                Toast.makeText(this, "A security update is required for biometric authentication.", Toast.LENGTH_LONG).show()
                finishAffinity()
            }
            else -> {
                Log.e(TAG, "Unknown biometric availability status.")
                Toast.makeText(this, "Could not determine biometric availability.", Toast.LENGTH_LONG).show()
                finishAffinity()
            }
        }
    }

    private fun showEnrollmentDialog() {
        Log.d(TAG, "showEnrollmentDialog")
        AlertDialog.Builder(this)
            .setTitle("Security Enrollment Required")
            .setMessage("Please set up a screen lock or enroll biometrics in your device settings to use this app.")
            .setPositiveButton("Go to Settings") { dialog, _ ->
                Log.d(TAG, "Launching enrollment settings.")
                try {
                    val enrollIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                            putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                                BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                        }
                    } else {
                        Intent(Settings.ACTION_SECURITY_SETTINGS)
                    }
                    startActivityForResult(enrollIntent, REQUEST_CODE_ENROLLMENT)
                } catch (e: Exception) {
                    Log.e(TAG, "Could not launch enrollment settings", e)
                    Toast.makeText(this, "Could not open security settings. Please enroll manually.", Toast.LENGTH_LONG).show()
                    finishAffinity()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Authentication required to use the app.", Toast.LENGTH_LONG).show()
                finishAffinity()
            }
            .setCancelable(false)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENROLLMENT) {
            Log.d(TAG, "Returned from enrollment settings (onActivityResult). Will re-check in onResume.")
        }
    }


    private fun setupApp() {
        Log.d(TAG, "setupApp: Initializing ViewModel, Views, Observers, Listeners.")
        viewModel = ViewModelProvider(this)[DayViewModel::class.java]

        findViews()

        setupObservers()

        setupListeners()

        dayView?.setOnEventClickListener(this)
    }

    private fun findViews() {
        Log.d(TAG, "findViews")
        dayView = findViewById(R.id.dayView)
        textViewSelectedDate = findViewById(R.id.textViewSelectedDate)
        buttonPreviousDay = findViewById(R.id.buttonPreviousDay)
        buttonNextDay = findViewById(R.id.buttonNextDay)
        fabAddEvent = findViewById(R.id.fabAddEvent)
    }

    private fun setupObservers() {
        Log.d(TAG, "setupObservers")
        viewModel.selectedDate.observe(this) { date ->
            textViewSelectedDate?.text = date?.format(dateFormatter) ?: "Select Date"
        }

        viewModel.eventsForSelectedDay.observe(this) { events ->
            Log.d(TAG, "Observer: Updating DayView with ${events?.size ?: 0} events.")
            dayView?.setEvents(events ?: emptyList())
        }
    }

    private fun setupListeners() {
        Log.d(TAG, "setupListeners")
        buttonPreviousDay?.setOnClickListener {
            viewModel.goToPreviousDay()
        }
        buttonNextDay?.setOnClickListener {
            viewModel.goToNextDay()
        }
        fabAddEvent?.setOnClickListener {
            val intent = Intent(this, EventEditActivity::class.java).apply {
                putExtra(EventEditActivity.EXTRA_EVENT_ID, EventEditActivity.NEW_EVENT_ID)
                val selectedDate = viewModel.selectedDate.value
                if (selectedDate != null) {
                    putExtra(EventEditActivity.EXTRA_SELECTED_DATE, selectedDate.toEpochDay())
                }
            }
            startActivity(intent)
        }
    }

    // --- Implementation of DayView.OnEventClickListener ---
    override fun onEventClick(event: Event) {
        Log.d(TAG, "onEventClick: Event ID ${event.id}")
        val intent = Intent(this, EventEditActivity::class.java).apply {
            putExtra(EventEditActivity.EXTRA_EVENT_ID, event.id)
        }
        startActivity(intent)
    }
}