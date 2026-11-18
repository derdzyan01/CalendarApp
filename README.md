# CalendarApp

A day-view calendar for Android. Events are drawn on a custom timeline, the same way a system calendar shows your day, and the app is protected with a biometric lock.

## Screenshots

| Day view | New event | Edit event |
| :---: | :---: | :---: |
| <img width="240" alt="image" src="https://github.com/user-attachments/assets/13de844d-2b6a-4604-9131-bb6f2775942b" /> | <img width="240" alt="image" src="https://github.com/user-attachments/assets/048decff-9d3f-4753-81e2-f497d94b11d3" /> | <img width="240"  alt="image" src="https://github.com/user-attachments/assets/56452f24-c97e-4f15-9d76-2ebf52ac36cc" /> |


## Features

- **Custom day view**: a hand-written `View` that draws the 24-hour grid, colour-coded event blocks and a live current-time indicator on `Canvas`, with tap detection to open an event.
- **Day navigation**: move to the previous or next day.
- **Create and edit events**: title, start and end date/time pickers, and a choice of four colours.
- **Delete events** with a confirmation dialog.
- **Biometric lock**: fingerprint, face or device PIN is required on launch and when returning to the app.
- **Offline storage**: all events are saved locally in Room.

## How it works

```
ui/DayView.kt            → custom View: measures, lays out and draws events on Canvas
ui/DayViewModel.kt       → selected date and the events for that day
data/EventEditViewModel  → state of the create/edit screen
data/EventRepository.kt  → single source of truth over the DAO
data/EventDao.kt         → Room queries (events for a day, insert, update, delete)
data/Converters.kt       → Room type converters for LocalDateTime
```

- **MVVM**: Activities observe ViewModels through LiveData.
- **Data**: a repository wraps the Room DAO; database work runs in coroutines.
- **Security**: `BiometricPrompt` with `BIOMETRIC_STRONG` or `DEVICE_CREDENTIAL`.

## Tech stack

| Area | Libraries |
| --- | --- |
| Language | Kotlin |
| UI | Custom View (Canvas), XML layouts, Material Components |
| Architecture | MVVM, Repository pattern |
| Async | Kotlin Coroutines, LiveData |
| Storage | Room |
| Security | AndroidX Biometric |

## Getting started

Clone the repository, open it in Android Studio and run it on a device or emulator (min SDK 26). On the emulator, set up a PIN or fingerprint in Settings → Security first so the biometric lock can work.

## What I'd improve next

- Lay out overlapping events side by side instead of on top of each other.
- Move the UI to Jetpack Compose, keeping the day view as a custom component.
- Read events from the device calendar through `CalendarContract`.
- Add week and month views.
