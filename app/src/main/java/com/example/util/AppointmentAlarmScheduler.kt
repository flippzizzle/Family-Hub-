package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.models.Appointment

class AppointmentAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Upcoming Family Event"
        val category = intent.getStringExtra("EXTRA_CATEGORY") ?: "Appointment"
        val location = intent.getStringExtra("EXTRA_LOCATION") ?: ""
        val apptId = intent.getStringExtra("EXTRA_ID") ?: ""

        val channelId = "family_appointments_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Family Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled household appointments and events"
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            apptId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = buildString {
            if (location.isNotBlank()) append("$location • ")
            append("Category: $category")
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_cosmic_hub_1794091280905)
            .setContentTitle("Cosmic Hub: $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Upcoming household appointment: $title\n$contentText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(android.graphics.Color.parseColor("#00F5FF"))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(apptId.hashCode(), notification)
        } catch (e: SecurityException) {
            // In case notification permission was revoked
        }
    }
}

object AppointmentAlarmScheduler {

    fun scheduleReminder(context: Context, appointment: Appointment) {
        if (appointment.reminderMinutes < 0) return
        val startTimeMs = appointment.startTime?.toDate()?.time ?: return
        val triggerTimeMs = startTimeMs - (appointment.reminderMinutes * 60 * 1000L)

        // Only schedule if in future
        if (triggerTimeMs <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AppointmentAlarmReceiver::class.java).apply {
            putExtra("EXTRA_ID", appointment.id)
            putExtra("EXTRA_TITLE", appointment.title)
            putExtra("EXTRA_CATEGORY", appointment.category)
            putExtra("EXTRA_LOCATION", appointment.location)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointment.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
        } catch (e: Exception) {
            // Inexact fallback
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, appointmentId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AppointmentAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
