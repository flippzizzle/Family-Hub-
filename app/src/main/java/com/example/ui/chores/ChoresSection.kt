package com.example.ui.chores

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Chore
import com.example.data.models.Member
import com.example.ui.components.NeonGlowCard
import com.example.ui.components.VaporwaveHeaderBadge
import com.google.firebase.Timestamp

val ChoreFilters = listOf("All", "Pending", "Daily", "Weekly", "Once", "Completed")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoresSection(
    chores: List<Chore>,
    members: List<Member>,
    onToggleChore: (Chore) -> Unit,
    onDeleteChore: (String) -> Unit,
    onAddChore: (title: String, description: String, assignedUid: String, assignedName: String, frequency: String, dueDate: Timestamp?, points: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredChores = remember(chores, selectedFilter) {
        when (selectedFilter) {
            "Pending" -> chores.filter { !it.isCompleted }
            "Daily" -> chores.filter { it.frequency.equals("daily", ignoreCase = true) }
            "Weekly" -> chores.filter { it.frequency.equals("weekly", ignoreCase = true) }
            "Once" -> chores.filter { it.frequency.equals("once", ignoreCase = true) }
            "Completed" -> chores.filter { it.isCompleted }
            else -> chores
        }
    }

    val pendingCount = chores.count { !it.isCompleted }
    val completedCount = chores.count { it.isCompleted }
    val totalPointsEarned = chores.filter { it.isCompleted }.sumOf { it.points }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VaporwaveHeaderBadge(
                        text = "$pendingCount PENDING",
                        glowColor = Color(0xFFB026FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    VaporwaveHeaderBadge(
                        text = "$completedCount DONE",
                        glowColor = Color(0xFF00FFB2)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFE600),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$totalPointsEarned pts earned",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFE600)
                    )
                }
            }

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ChoreFilters) { filter ->
                    val selected = filter == selectedFilter
                    FilterChip(
                        selected = selected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFB026FF).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFFB026FF),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("filter_chore_${filter.lowercase()}")
                    )
                }
            }

            // Chores List
            if (filteredChores.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Assignment,
                            contentDescription = null,
                            tint = Color(0xFFB026FF).copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (chores.isEmpty()) "No household chores scheduled" else "No matching chores",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+' to schedule daily, weekly, or one-time tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredChores, key = { it.id }) { chore ->
                        ChoreCard(
                            chore = chore,
                            onToggle = { onToggleChore(chore) },
                            onDelete = { onDeleteChore(chore.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Add Chore FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = Color(0xFFB026FF),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_chore_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add new chore")
        }
    }

    if (showAddDialog) {
        AddChoreDialog(
            members = members,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, assignedUid, assignedName, freq, points ->
                onAddChore(title, desc, assignedUid, assignedName, freq, null, points)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ChoreCard(
    chore: Chore,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = chore.isCompleted

    NeonGlowCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("chore_card_${chore.id}"),
        borderColor = if (isDone) Color(0xFF00FFB2).copy(alpha = 0.5f) else Color(0xFFB026FF).copy(alpha = 0.8f),
        containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isDone) "Mark pending" else "Mark completed",
                    tint = if (isDone) Color(0xFF00FFB2) else Color(0xFFB026FF)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chore.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Recurrence badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFB026FF).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (chore.frequency != "once") {
                                Icon(
                                    Icons.Default.Loop,
                                    contentDescription = null,
                                    tint = Color(0xFFB026FF),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = chore.frequency.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB026FF)
                            )
                        }
                    }

                    // Assigned to
                    Text(
                        text = "• ${chore.assignedToName.ifBlank { "Anyone" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Points badge
                    VaporwaveHeaderBadge(
                        text = "+${chore.points} pts",
                        glowColor = Color(0xFFFFE600)
                    )
                }

                if (chore.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = chore.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete chore",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChoreDialog(
    members: List<Member>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, assignedUid: String, assignedName: String, frequency: String, points: Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf("daily") }
    var points by remember { mutableLongStateOf(10L) }
    var assignedUid by remember { mutableStateOf("") }
    var assignedName by remember { mutableStateOf("Anyone") }

    var freqExpanded by remember { mutableStateOf(false) }
    var memberExpanded by remember { mutableStateOf(false) }

    val memberOptions = remember(members) {
        listOf(Pair("", "Anyone")) + members.map { Pair(it.uid, it.displayName.ifBlank { it.email }) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Household Chore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Chore Title * (e.g. Empty Dishwasher)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chore_title_input")
                )

                // Frequency Dropdown
                ExposedDropdownMenuBox(
                    expanded = freqExpanded,
                    onExpandedChange = { freqExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedFrequency.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = freqExpanded,
                        onDismissRequest = { freqExpanded = false }
                    ) {
                        listOf("once", "daily", "weekly").forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedFrequency = freq
                                    freqExpanded = false
                                }
                            )
                        }
                    }
                }

                // Member Dropdown
                ExposedDropdownMenuBox(
                    expanded = memberExpanded,
                    onExpandedChange = { memberExpanded = it }
                ) {
                    OutlinedTextField(
                        value = assignedName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assign To") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = memberExpanded,
                        onDismissRequest = { memberExpanded = false }
                    ) {
                        memberOptions.forEach { (uid, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    assignedUid = uid
                                    assignedName = name
                                    memberExpanded = false
                                }
                            )
                        }
                    }
                }

                // Points Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reward Points:")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5L, 10L, 20L, 50L).forEach { pt ->
                            val isSelected = points == pt
                            FilterChip(
                                selected = isSelected,
                                onClick = { points = pt },
                                label = { Text("$pt") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFE600).copy(alpha = 0.25f),
                                    selectedLabelColor = Color(0xFFFFE600)
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Instructions (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, description, assignedUid, assignedName, selectedFrequency, points)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_chore_button")
            ) {
                Text("Create Chore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
