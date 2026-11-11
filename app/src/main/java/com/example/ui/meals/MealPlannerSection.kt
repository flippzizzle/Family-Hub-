package com.example.ui.meals

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.MasterMealItem
import com.example.data.models.MealPlanDay
import com.example.data.models.Member
import com.example.ui.components.NeonGlowCard
import com.example.ui.components.VaporwaveHeaderBadge
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val MealTiers = listOf(
    "All",
    "Cheapest & Easiest",
    "Moderate Cost & Effort",
    "Higher Cost or More Complex"
)

data class DayInfo(
    val dateString: String, // yyyy-MM-dd
    val dayOfWeek: String, // Monday, Tuesday, ...
    val displayLabel: String, // "Mon, Nov 16"
    val isToday: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerSection(
    mealPlans: List<MealPlanDay>,
    masterMeals: List<MasterMealItem>,
    members: List<Member>,
    onSetMealPlanDay: (dateString: String, dayOfWeek: String, mealId: String, mealName: String, tier: String, assignedCook: String, notes: String) -> Unit,
    onClearMealPlanDay: (dateString: String) -> Unit,
    onAddCustomMeal: (name: String, tier: String, notes: String) -> Unit,
    onDeleteCustomMeal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var weekOffset by remember { mutableIntStateOf(0) }
    var selectedDayForPicker by remember { mutableStateOf<DayInfo?>(null) }
    var showAddCustomMealDialog by remember { mutableStateOf(false) }

    val todayDateString = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Generate 7 days of the selected week (Monday to Sunday)
    val daysOfWeek: List<DayInfo> = remember(weekOffset) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

        val list = mutableListOf<DayInfo>()
        for (i in 0 until 7) {
            val dateStr = dateFormat.format(cal.time)
            list.add(
                DayInfo(
                    dateString = dateStr,
                    dayOfWeek = dayOfWeekFormat.format(cal.time),
                    displayLabel = displayFormat.format(cal.time),
                    isToday = dateStr == todayDateString
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    val weekHeaderTitle = remember(daysOfWeek) {
        "${daysOfWeek.first().displayLabel} — ${daysOfWeek.last().displayLabel}"
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Week Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { weekOffset-- }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous week",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = weekHeaderTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (weekOffset != 0) {
                        TextButton(
                            onClick = { weekOffset = 0 },
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Jump to Current Week", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                IconButton(onClick = { weekOffset++ }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next week",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 7-Day Dinner Schedule List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(daysOfWeek) { dayInfo ->
                    val plan = mealPlans.firstOrNull { it.dateString == dayInfo.dateString }
                    DayMealPlanCard(
                        dayInfo = dayInfo,
                        mealPlan = plan,
                        onSelectMeal = { selectedDayForPicker = dayInfo },
                        onClearMeal = { onClearMealPlanDay(dayInfo.dateString) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // FAB to add a new custom meal to the master library
        FloatingActionButton(
            onClick = { showAddCustomMealDialog = true },
            containerColor = Color(0xFFFF007F),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_custom_meal_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add custom meal to library")
        }
    }

    // Modal BottomSheet to Select Meal from 72 Master Meals + Custom Library
    if (selectedDayForPicker != null) {
        val targetDay = selectedDayForPicker!!
        ModalBottomSheet(
            onDismissRequest = { selectedDayForPicker = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            MasterMealPickerSheet(
                targetDay = targetDay,
                masterMeals = masterMeals,
                members = members,
                onMealSelected = { meal, cook, notes ->
                    onSetMealPlanDay(
                        targetDay.dateString,
                        targetDay.dayOfWeek,
                        meal.id,
                        meal.name,
                        meal.costAndEffortTier,
                        cook,
                        notes
                    )
                    selectedDayForPicker = null
                },
                onAddNewCustomMeal = {
                    showAddCustomMealDialog = true
                },
                onClose = { selectedDayForPicker = null }
            )
        }
    }

    // Add Custom Meal Dialog
    if (showAddCustomMealDialog) {
        AddCustomMealDialog(
            onDismiss = { showAddCustomMealDialog = false },
            onConfirm = { name, tier, notes ->
                onAddCustomMeal(name, tier, notes)
                showAddCustomMealDialog = false
            }
        )
    }
}

@Composable
fun DayMealPlanCard(
    dayInfo: DayInfo,
    mealPlan: MealPlanDay?,
    onSelectMeal: () -> Unit,
    onClearMeal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasMeal = mealPlan != null && mealPlan.mealName.isNotBlank()

    NeonGlowCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = if (dayInfo.isToday) Color(0xFFFF007F) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        borderWidth = if (dayInfo.isToday) 2.dp else 1.dp,
        containerColor = if (dayInfo.isToday) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Day Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dayInfo.displayLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (dayInfo.isToday) Color(0xFFFF007F) else MaterialTheme.colorScheme.onSurface
                    )
                    if (dayInfo.isToday) {
                        Spacer(modifier = Modifier.width(8.dp))
                        VaporwaveHeaderBadge(text = "TODAY", glowColor = Color(0xFFFF007F))
                    }
                }

                if (hasMeal) {
                    IconButton(
                        onClick = onClearMeal,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear dinner",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (hasMeal) {
                Text(
                    text = mealPlan!!.mealName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tierColor = when (mealPlan.tier) {
                        "Cheapest & Easiest" -> Color(0xFF00FFB2)
                        "Higher Cost or More Complex" -> Color(0xFFFF2A6D)
                        else -> Color(0xFFFF7A00)
                    }
                    VaporwaveHeaderBadge(text = mealPlan.tier, glowColor = tierColor)

                    if (mealPlan.assignedCook.isNotBlank()) {
                        Text(
                            text = "Cook: ${mealPlan.assignedCook}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (mealPlan.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mealPlan.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = onSelectMeal,
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Change Meal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Button(
                    onClick = onSelectMeal,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Assign Dinner from 72 Master Meals", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun MasterMealPickerSheet(
    targetDay: DayInfo,
    masterMeals: List<MasterMealItem>,
    members: List<Member>,
    onMealSelected: (meal: MasterMealItem, cook: String, notes: String) -> Unit,
    onAddNewCustomMeal: () -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTier by remember { mutableStateOf("All") }
    var assignedCook by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val filteredMeals = remember(masterMeals, searchQuery, selectedTier) {
        masterMeals.filter { meal ->
            val matchTier = selectedTier == "All" || meal.costAndEffortTier.equals(selectedTier, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() || meal.name.contains(searchQuery, ignoreCase = true)
            matchTier && matchQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(580.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Assign Dinner: ${targetDay.displayLabel}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFFF007F),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select from 72 household master meals or custom favorites",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        // Cook and Notes Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = assignedCook,
                onValueChange = { assignedCook = it },
                label = { Text("Cook / Chef") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Sides") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search 72 master meals...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tier Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(MealTiers) { tier ->
                val selected = tier == selectedTier
                val count = if (tier == "All") masterMeals.size else masterMeals.count { it.costAndEffortTier.equals(tier, ignoreCase = true) }
                FilterChip(
                    selected = selected,
                    onClick = { selectedTier = tier },
                    label = { Text("$tier ($count)", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF007F).copy(alpha = 0.25f),
                        selectedLabelColor = Color(0xFFFF007F)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Meals List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredMeals) { meal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onMealSelected(meal, assignedCook, notes) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = meal.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val tierColor = when (meal.costAndEffortTier) {
                                "Cheapest & Easiest" -> Color(0xFF00FFB2)
                                "Higher Cost or More Complex" -> Color(0xFFFF2A6D)
                                else -> Color(0xFFFF7A00)
                            }
                            Text(
                                text = meal.costAndEffortTier,
                                style = MaterialTheme.typography.labelSmall,
                                color = tierColor
                            )
                            if (meal.isCustom) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Custom Meal",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { onMealSelected(meal, assignedCook, notes) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Select", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomMealDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, tier: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tier by remember { mutableStateOf("Moderate Cost & Effort") }
    var notes by remember { mutableStateOf("") }
    var tierExpanded by remember { mutableStateOf(false) }

    val options = listOf(
        "Cheapest & Easiest",
        "Moderate Cost & Effort",
        "Higher Cost or More Complex"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Meal to Library") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Meal Name * (e.g. Grandma's Lasagna)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = tierExpanded,
                    onExpandedChange = { tierExpanded = it }
                ) {
                    OutlinedTextField(
                        value = tier,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cost & Effort Tier") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tierExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = tierExpanded,
                        onDismissRequest = { tierExpanded = false }
                    ) {
                        options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    tier = opt
                                    tierExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Side dishes / notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, tier, notes)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add to Library")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
