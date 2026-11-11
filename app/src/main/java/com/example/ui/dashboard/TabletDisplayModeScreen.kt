package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Appointment
import com.example.data.models.Chore
import com.example.data.models.GroceryItem
import com.example.data.models.HouseholdNote
import com.example.data.models.MealPlanDay
import com.example.data.models.TabletDisplayConfig
import com.example.ui.components.NeonGlowCard
import com.example.ui.components.VaporwaveHeaderBadge
import com.example.ui.components.VaporwaveStarfieldBackground
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TabletDisplayModeScreen(
    hubName: String,
    groceries: List<GroceryItem>,
    appointments: List<Appointment>,
    chores: List<Chore>,
    mealPlans: List<MealPlanDay>,
    notes: List<HouseholdNote>,
    displayConfig: TabletDisplayConfig,
    onUpdateDisplayConfig: (TabletDisplayConfig) -> Unit,
    onToggleGrocery: (GroceryItem) -> Unit,
    onToggleChore: (Chore) -> Unit,
    onExitDisplayMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Live clock ticker
    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        while (true) {
            val now = Date()
            currentTimeString = timeFormat.format(now)
            currentDateString = dateFormat.format(now)
            delay(1000L)
        }
    }

    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val tonightsDinner = remember(mealPlans, todayKey) {
        mealPlans.firstOrNull { it.dateString == todayKey }
    }
    val pendingGroceries = remember(groceries) { groceries.filter { !it.isCompleted } }

    VaporwaveStarfieldBackground(
        enabled = true,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Tablet Header: Glowing Clock & Hub Title & Kiosk Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Hub Name & Live Date
                Column {
                    Text(
                        text = hubName.ifBlank { "Cosmic Family Hub" }.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = currentDateString,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Center: Big Live Glowing Retro Digital Clock
                if (displayConfig.showClock) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF040A1A))
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currentTimeString,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00F5FF),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Right: Controls (Settings & Exit)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("display_mode_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Display Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onExitDisplayMode,
                        modifier = Modifier.testTag("exit_display_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FullscreenExit,
                            contentDescription = "Exit Display Mode",
                            tint = Color(0xFFFF007F)
                        )
                    }
                }
            }

            // Landscape Tablet Responsive Layout
            // Cards are arranged horizontally/in columns to fit Samsung Galaxy tablet screen without whole-page scroll.
            // Each card contains its own internal scroll if content overflows.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Column 1: Dinner & Notes
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (displayConfig.showDinner) {
                        TabletDinnerCard(
                            dinner = tonightsDinner,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (displayConfig.showNotes) {
                        TabletNotesCard(
                            notes = notes,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Column 2: Calendar Appointments
                if (displayConfig.showCalendar) {
                    TabletCalendarCard(
                        appointments = appointments,
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                    )
                }

                // Column 3: Chores
                if (displayConfig.showChores) {
                    TabletChoresCard(
                        chores = chores,
                        onToggle = onToggleChore,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }

                // Column 4: Groceries Needed
                if (displayConfig.showGroceries) {
                    TabletGroceriesCard(
                        groceries = pendingGroceries,
                        onToggle = onToggleGrocery,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    // Display Settings Dialog
    if (showSettingsDialog) {
        DisplaySettingsDialog(
            config = displayConfig,
            onDismiss = { showSettingsDialog = false },
            onSave = { newConfig ->
                onUpdateDisplayConfig(newConfig)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
private fun TabletDinnerCard(
    dinner: MealPlanDay?,
    modifier: Modifier = Modifier
) {
    NeonGlowCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = Color(0xFFFF007F),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Fastfood,
                        contentDescription = null,
                        tint = Color(0xFFFF007F),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "TONIGHT'S DINNER",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF007F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (dinner != null && dinner.mealName.isNotBlank()) {
                Text(
                    text = dinner.mealName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VaporwaveHeaderBadge(text = dinner.tier, glowColor = Color(0xFFFF7A00))
                    if (dinner.assignedCook.isNotBlank()) {
                        VaporwaveHeaderBadge(text = "Chef: ${dinner.assignedCook}", glowColor = Color(0xFF00F5FF))
                    }
                }
                if (dinner.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dinner.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = "No dinner set for tonight.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TabletCalendarCard(
    appointments: List<Appointment>,
    modifier: Modifier = Modifier
) {
    NeonGlowCard(
        modifier = modifier,
        borderColor = Color(0xFF00F5FF),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color(0xFF00F5FF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "CALENDAR (${appointments.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00F5FF)
                )
            }

            if (appointments.isEmpty()) {
                Text(
                    "No upcoming events",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(appointments, key = { it.id }) { appt ->
                        val timeStr = remember(appt.startTime) {
                            appt.startTime?.toDate()?.let {
                                SimpleDateFormat("EEE h:mm a", Locale.getDefault()).format(it)
                            } ?: "Scheduled"
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = appt.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (appt.syncedWithGoogle) {
                                    VaporwaveHeaderBadge(text = "G-Cal", glowColor = Color(0xFF00FFB2))
                                }
                            }
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletChoresCard(
    chores: List<Chore>,
    onToggle: (Chore) -> Unit,
    modifier: Modifier = Modifier
) {
    val pending = remember(chores) { chores.filter { !it.isCompleted } }

    NeonGlowCard(
        modifier = modifier,
        borderColor = Color(0xFFB026FF),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = null,
                    tint = Color(0xFFB026FF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "CHORES (${pending.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB026FF)
                )
            }

            if (pending.isEmpty()) {
                Text(
                    "All household chores completed!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(pending, key = { it.id }) { chore ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onToggle(chore) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chore.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = chore.assignedToName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletGroceriesCard(
    groceries: List<GroceryItem>,
    onToggle: (GroceryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NeonGlowCard(
        modifier = modifier,
        borderColor = Color(0xFFFF7A00),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Color(0xFFFF7A00),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "GROCERIES (${groceries.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF7A00)
                )
            }

            if (groceries.isEmpty()) {
                Text(
                    "Grocery list is empty",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(groceries, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { onToggle(item) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = Color(0xFFFF7A00),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.store,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletNotesCard(
    notes: List<HouseholdNote>,
    modifier: Modifier = Modifier
) {
    NeonGlowCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = Color(0xFFFFE600),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    Icons.Default.Note,
                    contentDescription = null,
                    tint = Color(0xFFFFE600),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "FAMILY BOARD",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFE600)
                )
            }

            if (notes.isEmpty()) {
                Text(
                    "No sticky notes posted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(notes.take(3), key = { it.id }) { note ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(6.dp)
                        ) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (note.content.isNotBlank()) {
                                Text(
                                    text = note.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DisplaySettingsDialog(
    config: TabletDisplayConfig,
    onDismiss: () -> Unit,
    onSave: (TabletDisplayConfig) -> Unit
) {
    var showClock by remember { mutableStateOf(config.showClock) }
    var showDinner by remember { mutableStateOf(config.showDinner) }
    var showCalendar by remember { mutableStateOf(config.showCalendar) }
    var showChores by remember { mutableStateOf(config.showChores) }
    var showGroceries by remember { mutableStateOf(config.showGroceries) }
    var showNotes by remember { mutableStateOf(config.showNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tablet Display Preferences") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Configure cards displayed on this device's landscape kiosk:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                DisplayCardToggleRow("Live Clock & Time", showClock) { showClock = it }
                DisplayCardToggleRow("Tonight's Dinner", showDinner) { showDinner = it }
                DisplayCardToggleRow("Calendar Appointments", showCalendar) { showCalendar = it }
                DisplayCardToggleRow("Household Chores", showChores) { showChores = it }
                DisplayCardToggleRow("Groceries Needed", showGroceries) { showGroceries = it }
                DisplayCardToggleRow("Family Notes Board", showNotes) { showNotes = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        config.copy(
                            showClock = showClock,
                            showDinner = showDinner,
                            showCalendar = showCalendar,
                            showChores = showChores,
                            showGroceries = showGroceries,
                            showNotes = showNotes
                        )
                    )
                }
            ) {
                Text("Save Preferences")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DisplayCardToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
