package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import com.example.data.models.Appointment

object GoogleCalendarUtil {

    fun syncAppointmentToGoogleCalendar(context: Context, appointment: Appointment): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, appointment.title)
                putExtra(
                    CalendarContract.Events.DESCRIPTION,
                    "${appointment.description}\n\n[Synced from Cosmic Family Hub | Category: ${appointment.category}]"
                )
                putExtra(CalendarContract.Events.EVENT_LOCATION, appointment.location)
                appointment.startTime?.let {
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it.toDate().time)
                }
                appointment.endTime?.let {
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it.toDate().time)
                } ?: appointment.startTime?.let {
                    // Default to 1 hour duration
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it.toDate().time + 3600000L)
                }
                putExtra(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_DEFAULT)
                putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open Google Calendar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun openGoogleCalendar(context: Context) {
        val uri = Uri.parse("content://com.android.calendar/time/${System.currentTimeMillis()}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://calendar.google.com/calendar/r"))
                context.startActivity(webIntent)
            } catch (err: Exception) {
                Toast.makeText(context, "Cannot open Google Calendar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
