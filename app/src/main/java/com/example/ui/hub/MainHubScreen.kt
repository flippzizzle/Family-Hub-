package com.example.ui.hub

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.admin.AdminSection
import com.example.ui.appointments.AppointmentSection
import com.example.ui.chores.ChoresSection
import com.example.ui.components.ActivityLogSheet
import com.example.ui.components.NeonGlowCard
import com.example.ui.components.VaporwaveStarfieldBackground
import com.example.ui.customization.ThemeSettingsSection
import com.example.ui.dashboard.HomeDashboardSection
import com.example.ui.dashboard.TabletDisplayModeScreen
import com.example.ui.groceries.GrocerySection
import com.example.ui.meals.MealPlannerSection
import com.example.ui.notes.NotesSection
import com.example.ui.theme.CosmicHubTheme
import com.example.ui.viewmodel.HubViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHubScreen(
    viewModel: HubViewModel,
    onSignOut: () -> Unit
) {
    val activeHub by viewModel.activeHub.collectAsState()
    val userHubs by viewModel.userHubs.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val isDisplayModeActive by viewModel.isDisplayModeActive.collectAsState()
    val tabletDisplayConfig by viewModel.tabletDisplayConfig.collectAsState()
    val customThemeConfig by viewModel.customThemeConfig.collectAsState()

    val groceries by viewModel.groceries.collectAsState()
    val groceryFavorites by viewModel.groceryFavorites.collectAsState()
    val groceryCatalog = viewModel.groceryCatalog
    val appointments by viewModel.appointments.collectAsState()
    val members by viewModel.members.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val chores by viewModel.chores.collectAsState()
    val mealPlans by viewModel.mealPlans.collectAsState()
    val allMasterMeals by viewModel.allMasterMeals.collectAsState()
    val notes by viewModel.notes.collectAsState()

    val googleSyncStatus by viewModel.googleSyncStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val errorBanner by viewModel.errorBanner.collectAsState()

    var showThemeSheet by remember { mutableStateOf(false) }
    var showActivitySheet by remember { mutableStateOf(false) }

    val effectivePalette = if (customThemeConfig.isCustomEnabled) {
        customThemeConfig.activePaletteName
    } else {
        activeHub?.themePalette ?: "Neon Galaxy"
    }
    val ambientStars = if (customThemeConfig.isCustomEnabled) customThemeConfig.showStars else (activeHub?.ambientStars ?: true)

    // Handle back button when in a sub-tab or in full-screen display mode
    if (isDisplayModeActive) {
        BackHandler {
            viewModel.setDisplayModeActive(false)
        }
    } else if (currentTab != 0) {
        BackHandler {
            viewModel.selectTab(0)
        }
    }

    CosmicHubTheme(
        paletteName = effectivePalette,
        customThemeConfig = customThemeConfig
    ) {
        if (isDisplayModeActive && activeHub != null) {
            // Full-screen Tablet Kiosk Display Mode
            TabletDisplayModeScreen(
                hubName = activeHub!!.name,
                groceries = groceries,
                appointments = appointments,
                chores = chores,
                mealPlans = mealPlans,
                notes = notes,
                displayConfig = tabletDisplayConfig,
                onUpdateDisplayConfig = { viewModel.updateTabletDisplayConfig(it) },
                onToggleGrocery = { viewModel.toggleGrocery(it) },
                onToggleChore = { viewModel.toggleChore(it) },
                onExitDisplayMode = { viewModel.setDisplayModeActive(false) }
            )
        } else {
            VaporwaveStarfieldBackground(enabled = ambientStars) {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        if (activeHub != null) {
                            HubBottomBar(
                                selectedTab = currentTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }
                    },
                    modifier = Modifier.testTag("main_hub_scaffold")
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (activeHub == null) {
                            NoHubScreen(
                                userEmail = viewModel.currentUserEmail,
                                onCreateHub = { name -> viewModel.createNewFamilyHub(name) }
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Cosmic Header Banner
                                CosmicHeader(
                                    hubName = activeHub!!.name,
                                    connectedCount = members.size,
                                    isOnline = isOnline,
                                    activityCount = activityLogs.size,
                                    onOpenDisplayMode = { viewModel.setDisplayModeActive(true) },
                                    onOpenThemeSettings = { showThemeSheet = true },
                                    onOpenActivityFeed = { showActivitySheet = true }
                                )

                                // Error Toast / Banner if present
                                if (errorBanner != null) {
                                    Snackbar(
                                        modifier = Modifier.padding(16.dp),
                                        action = {
                                            TextButton(onClick = { viewModel.clearError() }) {
                                                Text("Dismiss", color = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Text(errorBanner!!)
                                    }
                                }

                                // Navigation Tab Routing
                                Box(modifier = Modifier.weight(1f)) {
                                    when (currentTab) {
                                        // 0: Real Household Dashboard Overview
                                        0 -> HomeDashboardSection(
                                            hubName = activeHub!!.name,
                                            currentUserDisplayName = viewModel.currentDisplayName,
                                            groceries = groceries,
                                            appointments = appointments,
                                            chores = chores,
                                            mealPlans = mealPlans,
                                            notes = notes,
                                            onToggleGrocery = { viewModel.toggleGrocery(it) },
                                            onToggleChore = { viewModel.toggleChore(it) },
                                            onNavigateToTab = { viewModel.selectTab(it) },
                                            onLaunchDisplayMode = { viewModel.setDisplayModeActive(true) }
                                        )

                                        // 1: Upgraded Grocery Catalog (318 Items, Favorites, Receipts & Export)
                                        1 -> GrocerySection(
                                            groceries = groceries,
                                            catalog = groceryCatalog,
                                            favorites = groceryFavorites,
                                            members = members,
                                            onToggleGrocery = { viewModel.toggleGrocery(it) },
                                            onRestoreGrocery = { viewModel.restoreGrocery(it) },
                                            onDeleteGrocery = { viewModel.deleteGrocery(it) },
                                            onClearHistory = { viewModel.clearGroceryHistory() },
                                            onToggleFavorite = { name, cat, store, src ->
                                                viewModel.toggleGroceryFavorite(name, cat, store, src)
                                            },
                                            onAddGrocery = { title, cat, qty, unit, assigned, notes, store, src, isHist ->
                                                viewModel.addGrocery(title, cat, qty, unit, assigned, notes, store, src, isHist)
                                            },
                                            isDuplicateCheck = { viewModel.isGroceryDuplicate(it) },
                                            isFavorite = { viewModel.isGroceryFavorite(it) },
                                            hubName = activeHub!!.name
                                        )

                                        // 2: Genuine Two-Way Google Calendar Sync
                                        2 -> AppointmentSection(
                                            appointments = appointments,
                                            syncStatus = googleSyncStatus,
                                            isSyncing = isSyncing,
                                            onSyncGoogleCalendar = { viewModel.syncGoogleCalendar() },
                                            onAddAppointment = { t, d, l, s, e, c, col, rem ->
                                                viewModel.addAppointment(t, d, l, s, e, c, col, rem)
                                            },
                                            onDeleteAppointment = { viewModel.deleteAppointment(it) },
                                            onMarkSynced = { id, evId ->
                                                viewModel.markAppointmentGoogleSynced(id, evId)
                                            }
                                        )

                                        // 3: Household Chores (Daily & Weekly Recurrence)
                                        3 -> ChoresSection(
                                            chores = chores,
                                            members = members,
                                            onToggleChore = { viewModel.toggleChore(it) },
                                            onDeleteChore = { viewModel.deleteChore(it) },
                                            onAddChore = { t, d, uid, name, f, due, pts ->
                                                viewModel.addChore(t, d, uid, name, f, due, pts)
                                            }
                                        )

                                        // 4: Meal Planner (7-Day Dinner Schedule & 72 Master Meals)
                                        4 -> MealPlannerSection(
                                            mealPlans = mealPlans,
                                            masterMeals = allMasterMeals,
                                            members = members,
                                            onSetMealPlanDay = { dateStr, dow, mealId, mealName, tier, cook, n ->
                                                viewModel.setMealPlanDay(dateStr, dow, mealId, mealName, tier, cook, n)
                                            },
                                            onClearMealPlanDay = { viewModel.clearMealPlanDay(it) },
                                            onAddCustomMeal = { name, tier, n ->
                                                viewModel.addCustomMeal(name, tier, n)
                                            },
                                            onDeleteCustomMeal = { viewModel.deleteCustomMeal(it) }
                                        )

                                        // 5: Family Notes / Message Board
                                        5 -> NotesSection(
                                            notes = notes,
                                            currentUserId = viewModel.currentUserId,
                                            onAddNote = { t, c, cat, col, pin ->
                                                viewModel.addNote(t, c, cat, col, pin)
                                            },
                                            onUpdateNote = { id, t, c, cat, col, pin ->
                                                viewModel.updateNote(id, t, c, cat, col, pin)
                                            },
                                            onDeleteNote = { viewModel.deleteNote(it) }
                                        )

                                        // 6: Family Admin & JSON Backup / Safe Merge Restore
                                        6 -> AdminSection(
                                            hub = activeHub!!,
                                            members = members,
                                            currentUserId = viewModel.currentUserId,
                                            onWhitelistEmail = { viewModel.whitelistEmail(it) },
                                            onRemoveWhitelistedEmail = { viewModel.removeWhitelistedEmail(it) },
                                            onSetMemberRole = { uid, role -> viewModel.setMemberRole(uid, role) },
                                            onRemoveMember = { uid -> viewModel.removeMember(uid) },
                                            onExportBackup = { viewModel.exportHouseholdBackup() },
                                            onImportBackup = { jsonString -> viewModel.importHouseholdBackup(jsonString) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Theme Studio Bottom Sheet (35 Presets + Custom Studio)
                if (showThemeSheet && activeHub != null) {
                    ModalBottomSheet(
                        onDismissRequest = { showThemeSheet = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrimColor = Color(0x99000000)
                    ) {
                        ThemeSettingsSection(
                            hub = activeHub!!,
                            customThemeConfig = customThemeConfig,
                            onUpdateCustomTheme = { newConfig ->
                                viewModel.updateCustomThemeConfig(newConfig)
                            },
                            onSignOut = {
                                showThemeSheet = false
                                onSignOut()
                            }
                        )
                    }
                }

                // Household Activity Log Sheet
                if (showActivitySheet) {
                    ActivityLogSheet(
                        activityLogs = activityLogs,
                        onDismiss = { showActivitySheet = false }
                    )
                }
            }
        }
    }
}

@Composable
fun CosmicHeader(
    hubName: String,
    connectedCount: Int,
    isOnline: Boolean,
    activityCount: Int,
    onOpenDisplayMode: () -> Unit,
    onOpenThemeSettings: () -> Unit,
    onOpenActivityFeed: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
    ) {
        // Hero background image
        Image(
            painter = painterResource(id = R.drawable.cosmic_hero_banner_1794091310409),
            contentDescription = "Cosmic Galaxy Banner",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xBB080114),
                            Color(0xE6120826)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hubName.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = if (isOnline) Color(0xFF00FFB2) else Color(0xFFFFB800)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOnline)
                            "LIVE SYNC • $connectedCount DEVICES"
                        else
                            "OFFLINE CACHE ACTIVE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp
                        ),
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Tablet Display Mode Kiosk Button
                IconButton(
                    onClick = onOpenDisplayMode,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .testTag("header_display_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Tablet Display Mode",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Activity Log Feed Button
                IconButton(
                    onClick = onOpenActivityFeed,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .testTag("open_activity_feed_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Recent Household Activity Feed",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Theme Customization Action Button
                IconButton(
                    onClick = onOpenThemeSettings,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .testTag("open_theme_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Customize Vaporwave Galaxy Theme",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Cloud Status
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .border(
                            1.dp,
                            if (isOnline) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color(0xFFFFB800).copy(alpha = 0.6f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = if (isOnline) "Connected to Firestore Cloud" else "Offline Cache Mode",
                        tint = if (isOnline) MaterialTheme.colorScheme.primary else Color(0xFFFFB800),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HubBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .testTag("main_navigation_bar")
    ) {
        val navItems = listOf(
            Triple(0, "Home", Icons.Default.Home),
            Triple(1, "Grocery", Icons.Default.ShoppingCart),
            Triple(2, "Calendar", Icons.Default.Event),
            Triple(3, "Chores", Icons.Default.Assignment),
            Triple(4, "Meals", Icons.Default.Fastfood),
            Triple(5, "Notes", Icons.Default.Note),
            Triple(6, "Admin", Icons.Default.AdminPanelSettings)
        )

        navItems.forEach { (index, label, icon) ->
            val isSelected = selectedTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.testTag("nav_tab_${label.lowercase()}")
            )
        }
    }
}

@Composable
fun NoHubScreen(
    userEmail: String,
    onCreateHub: (String) -> Unit
) {
    var hubName by remember { mutableStateOf("Cosmic Family Hub") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        NeonGlowCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = MaterialTheme.colorScheme.primary
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome to Cosmic Family Hub",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Logged in as $userEmail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You are not currently connected to an active family hub. Create your family hub below or ask an admin to whitelist your Google email address ($userEmail).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = hubName,
                    onValueChange = { hubName = it },
                    label = { Text("Family Hub Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_hub_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (hubName.isNotBlank()) onCreateHub(hubName.trim())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("create_hub_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Launch Family Hub", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
