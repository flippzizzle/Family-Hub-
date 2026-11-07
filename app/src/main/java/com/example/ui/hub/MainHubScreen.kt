package com.example.ui.hub

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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.ui.components.NeonGlowCard
import com.example.ui.components.VaporwaveStarfieldBackground
import com.example.ui.customization.ThemeSettingsSection
import com.example.ui.groceries.GrocerySection
import com.example.ui.theme.CosmicHubTheme
import com.example.ui.viewmodel.HubViewModel

import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import com.example.ui.components.ActivityLogSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHubScreen(
    viewModel: HubViewModel,
    onSignOut: () -> Unit
) {
    val activeHub by viewModel.activeHub.collectAsState()
    val userHubs by viewModel.userHubs.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val groceries by viewModel.groceries.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val members by viewModel.members.collectAsState()
    val activityLogs by viewModel.activityLogs.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val errorBanner by viewModel.errorBanner.collectAsState()

    var showThemeSheet by remember { mutableStateOf(false) }
    var showActivitySheet by remember { mutableStateOf(false) }

    val currentPalette = activeHub?.themePalette ?: "Neon Galaxy"
    val ambientStars = activeHub?.ambientStars ?: true

    CosmicHubTheme(paletteName = currentPalette) {
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
                            // Cosmic Header Banner with live status & activity launcher
                            CosmicHeader(
                                hubName = activeHub!!.name,
                                connectedCount = members.size,
                                isOnline = isOnline,
                                activityCount = activityLogs.size,
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

                            // Tab Body - Main 3 sections: Grocery, Calendar, Family Admin
                            Box(modifier = Modifier.weight(1f)) {
                                when (currentTab) {
                                    0 -> GrocerySection(
                                        groceries = groceries,
                                        members = members,
                                        onToggleGrocery = { viewModel.toggleGrocery(it) },
                                        onDeleteGrocery = { viewModel.deleteGrocery(it) },
                                        onAddGrocery = { t, c, q, u, a, n ->
                                            viewModel.addGrocery(t, c, q, u, a, n)
                                        }
                                    )
                                    1 -> AppointmentSection(
                                        appointments = appointments,
                                        onAddAppointment = { t, d, l, s, e, c, col, rem ->
                                            viewModel.addAppointment(t, d, l, s, e, c, col, rem)
                                        },
                                        onDeleteAppointment = { viewModel.deleteAppointment(it) },
                                        onMarkSynced = { id, evId ->
                                            viewModel.markAppointmentGoogleSynced(id, evId)
                                        }
                                    )
                                    2 -> AdminSection(
                                        hub = activeHub!!,
                                        members = members,
                                        currentUserId = viewModel.currentUserId,
                                        onWhitelistEmail = { viewModel.whitelistEmail(it) },
                                        onRemoveWhitelistedEmail = { viewModel.removeWhitelistedEmail(it) },
                                        onSetMemberRole = { uid, role -> viewModel.setMemberRole(uid, role) },
                                        onRemoveMember = { uid -> viewModel.removeMember(uid) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Theme & Customization Bottom Sheet
            if (showThemeSheet && activeHub != null) {
                ModalBottomSheet(
                    onDismissRequest = { showThemeSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrimColor = Color(0x99000000)
                ) {
                    ThemeSettingsSection(
                        hub = activeHub!!,
                        onUpdateTheme = { pal, glow, stars ->
                            viewModel.updateTheme(pal, glow, stars)
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

@Composable
fun CosmicHeader(
    hubName: String,
    connectedCount: Int,
    isOnline: Boolean,
    activityCount: Int,
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
                            "LIVE CLOUD SYNC • $connectedCount DEVICES CONNECTED"
                        else
                            "OFFLINE CACHE ACTIVE • SAVING TO DEVICE",
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

                // Network / Live Sync Status Indicator
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
            Triple(0, "Grocery", Icons.Default.ShoppingCart),
            Triple(1, "Calendar", Icons.Default.Event),
            Triple(2, "Family Admin", Icons.Default.AdminPanelSettings)
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
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.testTag("nav_tab_${label.lowercase().replace(' ', '_')}")
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
    var isCreating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_cosmic_hub_1794091280905),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "GOOGLE ACCOUNT CONNECTED",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Color(0xFF00FFB2)
            )
        )
        Text(
            text = userEmail,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Zero-Trust Family Isolation: Only Google accounts whitelisted by the primary family admin can join this hub. No unapproved users can access your lists or schedule.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        NeonGlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Launch As Primary Family Admin",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Create the hub and whitelist your family members' Google emails in the Admin panel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = hubName,
                    onValueChange = { hubName = it },
                    label = { Text("Family Hub Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("hub_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (hubName.isNotBlank()) {
                            isCreating = true
                            onCreateHub(hubName.trim())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("create_hub_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    enabled = !isCreating && hubName.isNotBlank()
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Create Central Family Hub",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
