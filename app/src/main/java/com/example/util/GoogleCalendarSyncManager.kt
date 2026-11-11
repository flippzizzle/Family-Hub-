package com.example.util

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.data.models.Appointment
import com.google.firebase.Timestamp
import java.util.Date
import java.util.TimeZone

data class GoogleCalendarSyncResult(
    val success: Boolean,
    val importedCount: Int = 0,
    val exportedCount: Int = 0,
    val accountName: String = "",
    val errorMessage: String? = null
)

object GoogleCalendarSyncManager {

    fun hasCalendarPermissions(context: Context): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    fun getPrimaryGoogleCalendarId(context: Context): Long? {
        if (!hasCalendarPermissions(context)) return null

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.VISIBLE
        )

        val uri: Uri = CalendarContract.Calendars.CONTENT_URI
        var primaryId: Long? = null
        var fallbackId: Long? = null

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                "(${CalendarContract.Calendars.ACCOUNT_TYPE} = ?)",
                arrayOf("com.google"),
                null
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val id = it.getLong(0)
                    val isPrimary = it.getInt(2) == 1
                    if (isPrimary) {
                        primaryId = id
                        break
                    }
                    if (fallbackId == null) {
                        fallbackId = id
                    }
                }
            }
        } catch (_: Exception) {
            // Permission or security exception
        }

        // If no google account calendar found, look for any visible calendar
        if (primaryId == null && fallbackId == null) {
            try {
                val cursor: Cursor? = context.contentResolver.query(
                    uri,
                    projection,
                    "${CalendarContract.Calendars.VISIBLE} = 1",
                    null,
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        fallbackId = it.getLong(0)
                    }
                }
            } catch (_: Exception) {}
        }

        return primaryId ?: fallbackId
    }

    fun getGoogleAccountName(context: Context): String {
        if (!hasCalendarPermissions(context)) return "Not connected"
        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars.ACCOUNT_NAME),
                "(${CalendarContract.Calendars.ACCOUNT_TYPE} = ?)",
                arrayOf("com.google"),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(0) ?: "Google Account"
                }
            }
        } catch (_: Exception) {}
        return "Device Calendar"
    }

    /**
     * Imports upcoming Google Calendar events from the device's master Google Calendar
     * within the specified date range (default: 7 days ago to 60 days ahead).
     */
    fun fetchGoogleCalendarEvents(
        context: Context,
        hubId: String,
        memberUids: List<String>,
        daysBack: Int = 7,
        daysAhead: Int = 60
    ): List<Appointment> {
        if (!hasCalendarPermissions(context)) return emptyList()

        val calId = getPrimaryGoogleCalendarId(context) ?: return emptyList()
        val now = System.currentTimeMillis()
        val startWindow = now - (daysBack * 86400000L)
        val endWindow = now + (daysAhead * 86400000L)

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )

        val selection = "(${CalendarContract.Events.CALENDAR_ID} = ?) AND " +
                "(${CalendarContract.Events.DTSTART} >= ?) AND " +
                "(${CalendarContract.Events.DTSTART} <= ?) AND " +
                "(${CalendarContract.Events.DELETED} = 0)"
        val selectionArgs = arrayOf(calId.toString(), startWindow.toString(), endWindow.toString())

        val imported = mutableListOf<Appointment>()

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val eventId = it.getLong(0).toString()
                    val title = it.getString(1)?.trim() ?: "Google Calendar Event"
                    val desc = it.getString(2) ?: ""
                    val loc = it.getString(3) ?: ""
                    val startMs = it.getLong(4)
                    val endMs = it.getLong(5).takeIf { ms -> ms > startMs } ?: (startMs + 3600000L)

                    val category = when {
                        title.contains("doctor", ignoreCase = true) || title.contains("dentist", ignoreCase = true) -> "Doctor"
                        title.contains("school", ignoreCase = true) || title.contains("class", ignoreCase = true) -> "School"
                        title.contains("soccer", ignoreCase = true) || title.contains("game", ignoreCase = true) -> "Sports"
                        title.contains("birthday", ignoreCase = true) || title.contains("party", ignoreCase = true) -> "Social"
                        else -> "Family"
                    }

                    imported.add(
                        Appointment(
                            hubId = hubId,
                            memberUids = memberUids,
                            title = title,
                            description = desc,
                            location = loc,
                            startTime = Timestamp(Date(startMs)),
                            endTime = Timestamp(Date(endMs)),
                            category = category,
                            colorTag = "#00F5FF",
                            googleCalendarEventId = eventId,
                            syncedWithGoogle = true,
                            createdBy = "Google Calendar Sync"
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Permission or querying exception
        }

        return imported
    }

    /**
     * Pushes or updates a Family Hub appointment in the master Google Calendar.
     * Returns the stable Google event ID or null on failure.
     */
    fun pushAppointmentToGoogleCalendar(context: Context, appointment: Appointment): String? {
        if (!hasCalendarPermissions(context)) return null
        val calendarId = getPrimaryGoogleCalendarId(context) ?: return null

        val startMs = appointment.startTime?.toDate()?.time ?: System.currentTimeMillis()
        val endMs = appointment.endTime?.toDate()?.time ?: (startMs + 3600000L)
        val timeZone = TimeZone.getDefault().id

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, appointment.title)
            put(
                CalendarContract.Events.DESCRIPTION,
                "${appointment.description}\n\n[Synced with Cosmic Family Hub • Category: ${appointment.category}]"
            )
            put(CalendarContract.Events.EVENT_LOCATION, appointment.location)
            put(CalendarContract.Events.DTSTART, startMs)
            put(CalendarContract.Events.DTEND, endMs)
            put(CalendarContract.Events.EVENT_TIMEZONE, timeZone)
            put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_DEFAULT)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            put(CalendarContract.Events.HAS_ALARM, if (appointment.reminderMinutes >= 0) 1 else 0)
        }

        return try {
            // If already has an event ID, attempt update
            if (appointment.googleCalendarEventId.isNotBlank()) {
                val eventUri = ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI,
                    appointment.googleCalendarEventId.toLong()
                )
                val rows = context.contentResolver.update(eventUri, values, null, null)
                if (rows > 0) {
                    return appointment.googleCalendarEventId
                }
            }

            // Insert new event
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment
        } catch (e: Exception) {
            null
        }
    }
}
