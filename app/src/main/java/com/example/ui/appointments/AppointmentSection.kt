package com.example.ui.appointments

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Appointment
import com.example.ui.components.NeonGlowCard
import com.example.ui.components.VaporwaveHeaderBadge
import com.example.util.GoogleCalendarUtil
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff

val AppointmentCategories = listOf("Doctor", "School", "Sports", "Social", "Family", "Work", "Other")
val NeonColorTags = listOf(
    "#FF007F" to "Magenta",
    "#00F5FF" to "Cyan",
    "#B026FF" to "Purple",
    "#FF7A00" to "Sunset",
    "#00FFB2" to "Mint",
    "#FFE600" to "Yellow"
)

@Composable
fun AppointmentSection(
    appointments: List<Appointment>,
    onAddAppointment: (title: String, desc: String, loc: String, start: Timestamp, end: Timestamp, cat: String, colorTag: String, reminderMinutes: Long) -> Unit,
    onDeleteAppointment: (String) -> Unit,
    onMarkSynced: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    // Calendar state: month & year offset
    val todayCal = remember { Calendar.getInstance() }
    var displayedMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }
    var displayedYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var selectedDateString by remember { mutableStateOf<String?>(null) } // "yyyy-MM-dd"
    var isCalendarViewExpanded by remember { mutableStateOf(true) }

    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Group appointments by date string
    val appointmentsByDate = remember(appointments) {
        appointments.groupBy { appt ->
            appt.startTime?.toDate()?.let { dayFormat.format(it) } ?: "unknown"
        }
    }

    val filteredAppointments = remember(appointments, selectedDateString) {
        if (selectedDateString == null) {
            appointments
        } else {
            appointmentsByDate[selectedDateString] ?: emptyList()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VaporwaveHeaderBadge(
                        text = "${appointments.size} EVENTS",
                        glowColor = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { isCalendarViewExpanded = !isCalendarViewExpanded },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("toggle_calendar_grid_view")
                    ) {
                        Icon(
                            imageVector = if (isCalendarViewExpanded) Icons.Default.FormatListBulleted else Icons.Default.CalendarMonth,
                            contentDescription = "Toggle View",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { GoogleCalendarUtil.openGoogleCalendar(context) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("open_google_calendar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Google Cal", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Interactive Calendar Grid (when expanded)
            if (isCalendarViewExpanded) {
                CalendarMonthGrid(
                    year = displayedYear,
                    month = displayedMonth,
                    selectedDateString = selectedDateString,
                    appointmentsByDate = appointmentsByDate,
                    onPrevMonth = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, displayedYear)
                            set(Calendar.MONTH, displayedMonth)
                            add(Calendar.MONTH, -1)
                        }
                        displayedYear = cal.get(Calendar.YEAR)
                        displayedMonth = cal.get(Calendar.MONTH)
                    },
                    onNextMonth = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, displayedYear)
                            set(Calendar.MONTH, displayedMonth)
                            add(Calendar.MONTH, 1)
                        }
                        displayedYear = cal.get(Calendar.YEAR)
                        displayedMonth = cal.get(Calendar.MONTH)
                    },
                    onSelectDate = { dateStr ->
                        selectedDateString = if (selectedDateString == dateStr) null else dateStr
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Filter status banner
            if (selectedDateString != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Viewing: $selectedDateString (${filteredAppointments.size} events)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { selectedDateString = null },
                        modifier = Modifier.size(24.dp).testTag("clear_date_filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Show All",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Events List
            if (filteredAppointments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (selectedDateString != null) "No appointments on this date" else "No family appointments scheduled",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap + to add an event and sync with Google Calendar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredAppointments, key = { it.id }) { item ->
                        AppointmentCard(
                            item = item,
                            context = context,
                            onDelete = { onDeleteAppointment(item.id) },
                            onSyncGoogle = {
                                val success = GoogleCalendarUtil.syncAppointmentToGoogleCalendar(context, item)
                                if (success) {
                                    onMarkSynced(item.id, "google_cal_synced")
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Add Event Floating Button
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_appointment_fab"),
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Appointment")
        }
    }

    if (showAddDialog) {
        AddAppointmentDialog(
            initialDateString = selectedDateString,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, loc, start, end, cat, colorTag, reminderMinutes ->
                onAddAppointment(title, desc, loc, start, end, cat, colorTag, reminderMinutes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CalendarMonthGrid(
    year: Int,
    month: Int,
    selectedDateString: String?,
    appointmentsByDate: Map<String, List<Appointment>>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit
) {
    val monthCal = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthName = remember(monthCal) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)
    }

    val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK) - 1 // 0-based from Sunday
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val dayFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    NeonGlowCard(
        modifier = Modifier.fillMaxWidth().testTag("calendar_month_grid_card"),
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Month Header with Prev/Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(28.dp).testTag("prev_month_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = monthName.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = onNextMonth, modifier = Modifier.size(28.dp).testTag("next_month_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day of Week Header (S M T W T F S)
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { dayLabel ->
                    Text(
                        text = dayLabel,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Days Grid
            val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
            for (row in 0 until (totalCells / 7)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOfWeek + 1

                        if (dayNumber in 1..daysInMonth) {
                            val tempCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, dayNumber)
                            }
                            val dateStr = dayFormat.format(tempCal.time)
                            val isSelected = dateStr == selectedDateString
                            val isToday = dateStr == todayStr
                            val dayEvents = appointmentsByDate[dateStr] ?: emptyList()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = when {
                                            isSelected -> 1.5.dp
                                            isToday -> 1.dp
                                            else -> 0.dp
                                        },
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.secondary
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onSelectDate(dateStr) }
                                    .testTag("calendar_day_$dateStr"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNumber",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.secondary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    // Dots indicator for appointments
                                    if (dayEvents.isNotEmpty()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            dayEvents.take(3).forEach { ev ->
                                                val dotColor = try {
                                                    Color(android.graphics.Color.parseColor(ev.colorTag))
                                                } catch (e: Exception) {
                                                    MaterialTheme.colorScheme.primary
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).height(38.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentCard(
    item: Appointment,
    context: Context,
    onDelete: () -> Unit,
    onSyncGoogle: () -> Unit
) {
    val tagColor = remember(item.colorTag) {
        try {
            Color(android.graphics.Color.parseColor(item.colorTag))
        } catch (e: Exception) {
            Color(0xFFFF007F)
        }
    }

    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }
    val formattedTime = remember(item.startTime) {
        item.startTime?.toDate()?.let { dateFormatter.format(it) } ?: "Date TBD"
    }

    NeonGlowCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("appointment_card_${item.id}"),
        borderColor = tagColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(tagColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = tagColor
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp).testTag("delete_appointment_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete appointment",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time & Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (item.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Alarm reminder badge
            val reminderLabel = when (item.reminderMinutes) {
                0L -> "Alarm at event start"
                15L -> "Alarm 15m before"
                60L -> "Alarm 1h before"
                1440L -> "Alarm 1 day before"
                else -> "No alarm"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item.reminderMinutes >= 0) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                    contentDescription = null,
                    tint = if (item.reminderMinutes >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = reminderLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.reminderMinutes >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Google Calendar Sync Button & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.syncedWithGoogle) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF00FFB2),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Synced with Google Calendar",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00FFB2)
                        )
                    }
                } else {
                    Text(
                        text = "Device-cloud only",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Button(
                    onClick = onSyncGoogle,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.syncedWithGoogle) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                        contentColor = if (item.syncedWithGoogle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("sync_google_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (item.syncedWithGoogle) "Re-sync to Google" else "Sync to Google Calendar",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppointmentDialog(
    initialDateString: String?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, loc: String, start: Timestamp, end: Timestamp, cat: String, colorTag: String, reminderMinutes: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf("Doctor") }
    var catExpanded by remember { mutableStateOf(false) }
    var selectedColorTag by remember { mutableStateOf("#FF007F") }
    var reminderMinutes by remember { mutableStateOf(60L) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Start time hours
    var hourOfDay by remember { mutableIntStateOf(14) }
    var minuteOfHour by remember { mutableIntStateOf(0) }

    // Selected day offset from today
    var selectedDayOffset by remember {
        mutableIntStateOf(
            if (initialDateString != null) {
                try {
                    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(initialDateString)
                    val diff = (parsed?.time ?: Date().time) - Date().time
                    (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                } catch (e: Exception) {
                    0
                }
            } else 0
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "New Family Appointment",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Appointment Title") },
                    placeholder = { Text("e.g. Pediatrician checkup, Soccer game") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("appointment_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        AppointmentCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCat = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location / Clinic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Preparation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Day selection
                Text(
                    text = "Date:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "Today", 1 to "Tomorrow", 2 to "+2 Days", 7 to "+1 Wk").forEach { (d, label) ->
                        val isSelected = selectedDayOffset == d
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedDayOffset = d }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Time selection
                Text(
                    text = "Time:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(9 to "9 AM", 12 to "12 PM", 14 to "2 PM", 17 to "5 PM", 19 to "7 PM").forEach { (h, label) ->
                        val isSelected = hourOfDay == h
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { hourOfDay = h }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Alarm reminder selector
                Text(
                    text = "Alarm & Notification Reminder:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        0L to "At start",
                        15L to "15m",
                        60L to "1 hour",
                        1440L to "1 day",
                        -1L to "None"
                    ).forEach { (m, label) ->
                        val isSelected = reminderMinutes == m
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    reminderMinutes = m
                                    if (m >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Color accent picker
                Text(
                    text = "Neon Glow Tag:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonColorTags.forEach { (hex, _) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColorTag == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorTag = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, selectedDayOffset)
                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                            set(Calendar.MINUTE, minuteOfHour)
                            set(Calendar.SECOND, 0)
                        }
                        val startTime = Timestamp(cal.time)
                        cal.add(Calendar.HOUR_OF_DAY, 1)
                        val endTime = Timestamp(cal.time)

                        onConfirm(title, description, location, startTime, endTime, selectedCat, selectedColorTag, reminderMinutes)
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.testTag("confirm_add_appointment_button")
            ) {
                Text("Add Appointment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
