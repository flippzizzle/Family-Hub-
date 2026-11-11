package com.example.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.models.ActivityLog
import com.example.data.models.Appointment
import com.example.data.models.CatalogGroceryItem
import com.example.data.models.Chore
import com.example.data.models.CustomThemeConfig
import com.example.data.models.GroceryItem
import com.example.data.models.HouseholdNote
import com.example.data.models.Hub
import com.example.data.models.MasterMealItem
import com.example.data.models.MealPlanDay
import com.example.data.models.Member
import com.example.data.models.TabletDisplayConfig
import com.example.data.repository.FamilyHubRepository
import com.example.util.AppointmentAlarmScheduler
import com.example.util.AssetDataLoader
import com.example.util.GoogleCalendarSyncManager
import com.example.util.NetworkMonitor
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class HubViewModel(
    private val context: Context,
    private val repository: FamilyHubRepository,
    val currentUserId: String
) : ViewModel() {

    private val auth = Firebase.auth
    val currentUserEmail: String = auth.currentUser?.email.orEmpty().lowercase()
    val currentDisplayName: String = auth.currentUser?.displayName ?: "Family Pilot"

    private val prefs = context.getSharedPreferences("cosmic_hub_prefs", Context.MODE_PRIVATE)

    private val networkMonitor = NetworkMonitor(context)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = true
    )

    private val _selectedHubId = MutableStateFlow<String?>(null)
    val selectedHubId: StateFlow<String?> = _selectedHubId.asStateFlow()

    // 0: Home/Dashboard, 1: Grocery, 2: Calendar, 3: Chores, 4: Meals, 5: Notes, 6: Admin/Settings
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _isDisplayModeActive = MutableStateFlow(false)
    val isDisplayModeActive: StateFlow<Boolean> = _isDisplayModeActive.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _googleSyncStatus = MutableStateFlow("Tap to sync with Google Calendar")
    val googleSyncStatus: StateFlow<String> = _googleSyncStatus.asStateFlow()

    private val _hasCalendarPermission = MutableStateFlow(GoogleCalendarSyncManager.hasCalendarPermissions(context))
    val hasCalendarPermission: StateFlow<Boolean> = _hasCalendarPermission.asStateFlow()

    // Tablet Display Configuration (stored in SharedPreferences)
    private val _tabletDisplayConfig = MutableStateFlow(loadTabletDisplayConfig())
    val tabletDisplayConfig: StateFlow<TabletDisplayConfig> = _tabletDisplayConfig.asStateFlow()

    // Custom Theme Studio Configuration (stored in SharedPreferences)
    private val _customThemeConfig = MutableStateFlow(loadCustomThemeConfig())
    val customThemeConfig: StateFlow<CustomThemeConfig> = _customThemeConfig.asStateFlow()

    // Static Assets: 318 Catalog Grocery Items & 72 Master Meals
    val groceryCatalog: List<CatalogGroceryItem> = AssetDataLoader.loadGroceryCatalog(context)
    private val staticMasterMeals: List<MasterMealItem> = AssetDataLoader.loadMasterMeals(context)

    // Accessible hubs: where user is already a member UID OR where user's Google Email is on the safe whitelist
    val userHubs: StateFlow<List<Hub>> = combine(
        repository.observeUserHubs(currentUserId),
        repository.observeWhitelistedHubs(currentUserEmail)
    ) { memberHubs, whitelistedHubs ->
        (memberHubs + whitelistedHubs).distinctBy { it.id }
    }.catch { e ->
        _errorBanner.value = "Failed to sync hubs: ${e.localizedMessage}"
        emit(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Current active hub
    val activeHub: StateFlow<Hub?> = _selectedHubId.flatMapLatest { hubId ->
        if (hubId != null) {
            repository.observeHub(hubId)
        } else {
            flowOf(null)
        }
    }.catch { e ->
        _errorBanner.value = "Hub sync error: ${e.localizedMessage}"
        emit(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    // Groceries in active hub
    val groceries: StateFlow<List<GroceryItem>> = _selectedHubId.flatMapLatest { hubId ->
        if (hubId != null) repository.observeGroceries(hubId) else flowOf(emptyList())
    }.map { list -> list.sortedWith(compareBy({ it.isCompleted }, { it.category }, { it.title })) }
        .catch { e ->
            _errorBanner.value = "Failed to load groceries: ${e.localizedMessage}"
            emit(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Appointments in active hub
    val appointments: StateFlow<List<Appointment>> = _selectedHubId.flatMapLatest { hubId ->
        if (hubId != null) repository.observeAppointments(hubId) else flowOf(emptyList())
    }.map { list -> list.sortedBy { it.startTime?.seconds ?: Long.MAX_VALUE } }
        .catch { e ->
            _errorBanner.value = "Failed to load appointments: ${e.localizedMessage}"
            emit(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Connected Members in active hub
    val members: StateFlow<List<Member>> = _selectedHubId.flatMapLatest { hubId ->
        if (hubId != null) repository.observeMembers(hubId) else flowOf(emptyList())
    }.catch { e ->
        _errorBanner.value = "Failed to load members: ${e.localizedMessage}"
        emit(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Activity Log / Audit Feed in active hub
    val activityLogs: StateFlow<List<ActivityLog>> = _selectedHubId.flatMapLatest { hubId ->
        if (hubId != null) repository.observeActivityLog(hubId) else flowOf(emptyList())
    }.catch { _ ->
        emit(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Chores in active hub
    val chores: StateFlow<List<Chore>> = _selectedHubId.flatMapLatest { hubId ->
        if (hubId != null) repository.observeChores(hubId) else flowOf(emptyList())
    }.map { list -> list.sortedWith(compareBy({ it.isCompleted }, { it.frequency }, { it.title })) }
        .catch { e ->
            _errorBanner.value = "Failed to load chores: ${e.localizedMessage}"
            emit(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // 7-Day Meal Plans in active hub
    val mealPlans: StateFlow<List<MealPlanDay>> = _selectedHubId.flatMapLatest { hubId ->
        if (hubId != null) repository.observeMealPlans(hubId) else flowOf(emptyList())
    }.map { list -> list.sortedBy { it.dateString } }
        .catch { e ->
            _errorBanner.value = "Failed to load meal plans: ${e.localizedMessage}"
            emit(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Custom Meals created by household
    val customMeals: StateFlow<List<MasterMealItem>> = _selectedHubId.flatMapLatest { hubId ->
        if (hubId != null) repository.observeCustomMeals(hubId) else flowOf(emptyList())
    }.catch { e ->
        emit(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // All Meals: 72 Master Meals + Household Custom Meals
    val allMasterMeals: StateFlow<List<MasterMealItem>> = customMeals.map { userCustomList ->
        staticMasterMeals + userCustomList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = staticMasterMeals
    )

    // Household Notes / Message Board
    val notes: StateFlow<List<HouseholdNote>> = _selectedHubId.flatMapLatest { hubId ->
        if (hubId != null) repository.observeNotes(hubId) else flowOf(emptyList())
    }.map { list -> list.sortedWith(compareByDescending<HouseholdNote> { it.isPinned }.thenByDescending { it.createdAt?.seconds ?: 0L }) }
        .catch { e ->
            _errorBanner.value = "Failed to load notes: ${e.localizedMessage}"
            emit(emptyList())
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        // Automatically discover whitelisted family hubs and register device presence
        viewModelScope.launch {
            userHubs.collect { hubs ->
                if (hubs.isNotEmpty()) {
                    val targetHub = if (_selectedHubId.value != null) {
                        hubs.firstOrNull { it.id == _selectedHubId.value } ?: hubs.first()
                    } else {
                        hubs.first()
                    }

                    _selectedHubId.value = targetHub.id

                    // Check if user's Google Email was whitelisted but their device isn't in memberUids yet
                    if (!targetHub.memberUids.contains(currentUserId)) {
                        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
                        val isHubOwner = targetHub.ownerId == currentUserId
                        val member = Member(
                            hubId = targetHub.id,
                            uid = currentUserId,
                            email = currentUserEmail,
                            displayName = currentDisplayName,
                            role = if (isHubOwner) "admin" else "member",
                            avatarIcon = "planet",
                            deviceInfo = deviceName
                        )
                        repository.joinHubIfWhitelisted(targetHub.id, member)
                    } else {
                        registerDevicePresence(targetHub.id, targetHub)
                    }
                }
            }
        }

        // Keep local alarm reminders synchronized for scheduled appointments
        viewModelScope.launch {
            appointments.collect { list ->
                list.forEach { appt ->
                    if (appt.reminderMinutes >= 0 && (appt.startTime?.toDate()?.time ?: 0) > System.currentTimeMillis()) {
                        AppointmentAlarmScheduler.scheduleReminder(context, appt)
                    }
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun setDisplayModeActive(active: Boolean) {
        _isDisplayModeActive.value = active
    }

    fun clearError() {
        _errorBanner.value = null
    }

    fun selectHub(hubId: String) {
        _selectedHubId.value = hubId
        val hub = userHubs.value.firstOrNull { it.id == hubId }
        if (hub != null) {
            registerDevicePresence(hubId, hub)
        }
    }

    private fun registerDevicePresence(hubId: String, hub: Hub) {
        viewModelScope.launch {
            try {
                val isOwner = hub.ownerId == currentUserId
                val isAdmin = isOwner || hub.adminUids.contains(currentUserId)
                val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
                val member = Member(
                    hubId = hubId,
                    uid = currentUserId,
                    email = currentUserEmail,
                    displayName = currentDisplayName,
                    role = if (isAdmin) "admin" else "member",
                    avatarIcon = "astronaut",
                    deviceInfo = deviceName
                )
                repository.registerOrUpdateMember(hubId, member)
            } catch (_: Exception) {
                // Non-fatal device presence update
            }
        }
    }

    fun createNewFamilyHub(name: String, theme: String = "Neon Galaxy") {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val newId = repository.createHub(name = name, themePalette = theme)
                _selectedHubId.value = newId
            } catch (e: Exception) {
                _errorBanner.value = "Failed to create family hub: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // --- GROCERY ACTIONS & CATALOG INTEGRATION ---

    fun isGroceryDuplicate(title: String): Boolean {
        val clean = title.trim().lowercase()
        return groceries.value.any { !it.isCompleted && it.title.trim().lowercase() == clean }
    }

    fun addGrocery(
        title: String,
        category: String,
        quantity: Long,
        unit: String,
        assignedTo: String,
        notes: String,
        store: String = "Kroger",
        source: String = "manual",
        isHistorical: Boolean = false
    ): Boolean {
        val hub = activeHub.value ?: return false
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return false

        viewModelScope.launch {
            try {
                repository.addGroceryItem(
                    hubId = hub.id,
                    memberUids = hub.memberUids,
                    title = cleanTitle,
                    category = category,
                    quantity = quantity,
                    unit = unit,
                    assignedTo = assignedTo,
                    notes = notes,
                    store = store,
                    source = source,
                    isHistorical = isHistorical
                )
            } catch (e: Exception) {
                _errorBanner.value = "Failed to add grocery: ${e.localizedMessage}"
            }
        }
        return true
    }

    fun toggleGrocery(item: GroceryItem) {
        val hub = activeHub.value
        val memberUids = hub?.memberUids ?: emptyList()
        viewModelScope.launch {
            try {
                repository.toggleGroceryCompleted(item, memberUids)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to update item: ${e.localizedMessage}"
            }
        }
    }

    fun deleteGrocery(itemId: String) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteGroceryItem(hub.id, itemId)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to delete item: ${e.localizedMessage}"
            }
        }
    }

    // --- CALENDAR & TWO-WAY GOOGLE CALENDAR SYNC ---

    fun refreshCalendarPermissionState() {
        _hasCalendarPermission.value = GoogleCalendarSyncManager.hasCalendarPermissions(context)
    }

    fun syncGoogleCalendar() {
        val hub = activeHub.value ?: return
        if (!GoogleCalendarSyncManager.hasCalendarPermissions(context)) {
            _googleSyncStatus.value = "Calendar permissions required for Google sync"
            return
        }

        viewModelScope.launch {
            _googleSyncStatus.value = "Connecting to Google Calendar..."
            _isSyncing.value = true
            try {
                val accountName = withContext(Dispatchers.IO) {
                    GoogleCalendarSyncManager.getGoogleAccountName(context)
                }

                // 1. Fetch events from Google Calendar (7 days back to 60 days ahead)
                val googleEvents = withContext(Dispatchers.IO) {
                    GoogleCalendarSyncManager.fetchGoogleCalendarEvents(
                        context = context,
                        hubId = hub.id,
                        memberUids = hub.memberUids,
                        daysBack = 7,
                        daysAhead = 60
                    )
                }

                var importedCount = 0
                for (event in googleEvents) {
                    val wasCreated = repository.upsertGoogleSyncedAppointment(hub.id, event)
                    if (wasCreated) importedCount++
                }

                // 2. Push hub appointments to Google Calendar if not yet synced
                var pushedCount = 0
                val hubAppointments = appointments.value
                for (appt in hubAppointments) {
                    if (appt.googleCalendarEventId.isBlank()) {
                        val googleId = withContext(Dispatchers.IO) {
                            GoogleCalendarSyncManager.pushAppointmentToGoogleCalendar(context, appt)
                        }
                        if (googleId != null) {
                            repository.updateAppointmentGoogleSync(hub.id, appt.id, googleId, true)
                            pushedCount++
                        }
                    }
                }

                _googleSyncStatus.value = "Synced with $accountName (Imported: $importedCount, Pushed: $pushedCount)"
            } catch (e: Exception) {
                _googleSyncStatus.value = "Sync error: ${e.localizedMessage}"
                _errorBanner.value = "Google Calendar sync error: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun addAppointment(
        title: String,
        description: String,
        location: String,
        startTime: Timestamp,
        endTime: Timestamp,
        category: String,
        colorTag: String,
        reminderMinutes: Long = 60L
    ) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                val docId = repository.addAppointment(
                    hubId = hub.id,
                    memberUids = hub.memberUids,
                    title = title,
                    description = description,
                    location = location,
                    startTime = startTime,
                    endTime = endTime,
                    category = category,
                    colorTag = colorTag,
                    reminderMinutes = reminderMinutes
                )

                // Schedule local alarm reminder immediately
                val tempAppt = Appointment(
                    id = docId,
                    hubId = hub.id,
                    title = title,
                    description = description,
                    location = location,
                    startTime = startTime,
                    endTime = endTime,
                    category = category,
                    colorTag = colorTag,
                    reminderMinutes = reminderMinutes
                )
                AppointmentAlarmScheduler.scheduleReminder(context, tempAppt)

                // If calendar permission is granted, sync to Google Calendar as well
                if (GoogleCalendarSyncManager.hasCalendarPermissions(context)) {
                    val googleEventId = withContext(Dispatchers.IO) {
                        GoogleCalendarSyncManager.pushAppointmentToGoogleCalendar(context, tempAppt)
                    }
                    if (googleEventId != null) {
                        repository.updateAppointmentGoogleSync(hub.id, docId, googleEventId, true)
                    }
                }
            } catch (e: Exception) {
                _errorBanner.value = "Failed to create appointment: ${e.localizedMessage}"
            }
        }
    }

    fun deleteAppointment(appointmentId: String) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                AppointmentAlarmScheduler.cancelReminder(context, appointmentId)
                repository.deleteAppointment(hub.id, appointmentId)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to delete appointment: ${e.localizedMessage}"
            }
        }
    }

    fun markAppointmentGoogleSynced(appointmentId: String, googleEventId: String) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.updateAppointmentGoogleSync(hub.id, appointmentId, googleEventId, true)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to record sync: ${e.localizedMessage}"
            }
        }
    }

    // --- CHORES ACTIONS ---

    fun addChore(
        title: String,
        description: String,
        assignedToUid: String,
        assignedToName: String,
        frequency: String,
        dueDate: Timestamp?,
        points: Long = 10L
    ) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.addChore(
                    hubId = hub.id,
                    memberUids = hub.memberUids,
                    title = title,
                    description = description,
                    assignedToUid = assignedToUid,
                    assignedToName = assignedToName,
                    frequency = frequency,
                    dueDate = dueDate,
                    points = points
                )
            } catch (e: Exception) {
                _errorBanner.value = "Failed to add chore: ${e.localizedMessage}"
            }
        }
    }

    fun toggleChore(chore: Chore) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.toggleChoreCompleted(chore, hub.memberUids)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to update chore: ${e.localizedMessage}"
            }
        }
    }

    fun deleteChore(choreId: String) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteChore(hub.id, choreId)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to delete chore: ${e.localizedMessage}"
            }
        }
    }

    // --- MEAL PLANNER ACTIONS ---

    fun setMealPlanDay(
        dateString: String,
        dayOfWeek: String,
        mealId: String,
        mealName: String,
        tier: String,
        assignedCook: String,
        notes: String
    ) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                val day = MealPlanDay(
                    id = dateString,
                    hubId = hub.id,
                    memberUids = hub.memberUids,
                    dateString = dateString,
                    dayOfWeek = dayOfWeek,
                    mealId = mealId,
                    mealName = mealName,
                    tier = tier,
                    assignedCook = assignedCook,
                    notes = notes
                )
                repository.setMealPlanDay(hub.id, hub.memberUids, day)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to update meal plan: ${e.localizedMessage}"
            }
        }
    }

    fun clearMealPlanDay(dateString: String) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.clearMealPlanDay(hub.id, dateString)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to clear meal: ${e.localizedMessage}"
            }
        }
    }

    fun addCustomMeal(name: String, tier: String, notes: String) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.addCustomMeal(
                    hubId = hub.id,
                    memberUids = hub.memberUids,
                    name = name.trim(),
                    costAndEffortTier = tier,
                    notes = notes
                )
            } catch (e: Exception) {
                _errorBanner.value = "Failed to save custom meal: ${e.localizedMessage}"
            }
        }
    }

    fun deleteCustomMeal(mealId: String) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteCustomMeal(hub.id, mealId)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to delete meal: ${e.localizedMessage}"
            }
        }
    }

    // --- FAMILY NOTES ACTIONS ---

    fun addNote(
        title: String,
        content: String,
        category: String,
        colorTag: String,
        isPinned: Boolean
    ) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.addNote(
                    hubId = hub.id,
                    memberUids = hub.memberUids,
                    title = title,
                    content = content,
                    category = category,
                    colorTag = colorTag,
                    isPinned = isPinned
                )
            } catch (e: Exception) {
                _errorBanner.value = "Failed to post note: ${e.localizedMessage}"
            }
        }
    }

    fun updateNote(
        noteId: String,
        title: String,
        content: String,
        category: String,
        colorTag: String,
        isPinned: Boolean
    ) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.updateNote(
                    hubId = hub.id,
                    noteId = noteId,
                    title = title,
                    content = content,
                    category = category,
                    colorTag = colorTag,
                    isPinned = isPinned
                )
            } catch (e: Exception) {
                _errorBanner.value = "Failed to update note: ${e.localizedMessage}"
            }
        }
    }

    fun deleteNote(noteId: String) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.deleteNote(hub.id, noteId)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to delete note: ${e.localizedMessage}"
            }
        }
    }

    // --- ADMIN & MEMBER ACTIONS ---

    fun whitelistEmail(email: String) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.whitelistEmail(hub.id, hub.memberUids, email)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to whitelist email: ${e.localizedMessage}"
            }
        }
    }

    fun removeWhitelistedEmail(email: String) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.removeWhitelistedEmail(hub.id, hub.memberUids, email)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to remove whitelisted email: ${e.localizedMessage}"
            }
        }
    }

    fun setMemberRole(memberUid: String, newRole: String) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.setMemberRole(hub.id, memberUid, newRole)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to update role: ${e.localizedMessage}"
            }
        }
    }

    fun removeMember(memberUid: String) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.removeMemberFromHub(hub.id, memberUid)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to remove member: ${e.localizedMessage}"
            }
        }
    }

    // --- THEME STUDIO & DISPLAY SETTINGS ---

    fun updateTabletDisplayConfig(config: TabletDisplayConfig) {
        _tabletDisplayConfig.value = config
        saveTabletDisplayConfig(config)
    }

    fun updateCustomThemeConfig(config: CustomThemeConfig) {
        _customThemeConfig.value = config
        saveCustomThemeConfig(config)
    }

    fun updateTheme(themePalette: String, glowIntensity: String, ambientStars: Boolean) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.updateHubTheme(hub.id, themePalette, glowIntensity, ambientStars)
                updateCustomThemeConfig(
                    _customThemeConfig.value.copy(
                        activePaletteName = themePalette,
                        glowLevel = glowIntensity,
                        showStars = ambientStars
                    )
                )
            } catch (e: Exception) {
                _errorBanner.value = "Failed to update theme: ${e.localizedMessage}"
            }
        }
    }

    private fun loadTabletDisplayConfig(): TabletDisplayConfig {
        val showClock = prefs.getBoolean("tablet_show_clock", true)
        val showDinner = prefs.getBoolean("tablet_show_dinner", true)
        val showCalendar = prefs.getBoolean("tablet_show_calendar", true)
        val showChores = prefs.getBoolean("tablet_show_chores", true)
        val showGroceries = prefs.getBoolean("tablet_show_groceries", true)
        val showNotes = prefs.getBoolean("tablet_show_notes", true)
        val orderString = prefs.getString("tablet_card_order", "dinner,calendar,chores,groceries,notes") ?: "dinner,calendar,chores,groceries,notes"
        val orderList = orderString.split(",").filter { it.isNotBlank() }

        return TabletDisplayConfig(
            showClock = showClock,
            showDinner = showDinner,
            showCalendar = showCalendar,
            showChores = showChores,
            showGroceries = showGroceries,
            showNotes = showNotes,
            cardOrder = if (orderList.isNotEmpty()) orderList else listOf("dinner", "calendar", "chores", "groceries", "notes")
        )
    }

    private fun saveTabletDisplayConfig(config: TabletDisplayConfig) {
        prefs.edit()
            .putBoolean("tablet_show_clock", config.showClock)
            .putBoolean("tablet_show_dinner", config.showDinner)
            .putBoolean("tablet_show_calendar", config.showCalendar)
            .putBoolean("tablet_show_chores", config.showChores)
            .putBoolean("tablet_show_groceries", config.showGroceries)
            .putBoolean("tablet_show_notes", config.showNotes)
            .putString("tablet_card_order", config.cardOrder.joinToString(","))
            .apply()
    }

    private fun loadCustomThemeConfig(): CustomThemeConfig {
        val enabled = prefs.getBoolean("theme_custom_enabled", false)
        val palette = prefs.getString("theme_palette", "Neon Galaxy") ?: "Neon Galaxy"
        val primary = prefs.getString("theme_primary", "#00F5FF") ?: "#00F5FF"
        val secondary = prefs.getString("theme_secondary", "#FF007F") ?: "#FF007F"
        val tertiary = prefs.getString("theme_tertiary", "#B026FF") ?: "#B026FF"
        val bg = prefs.getString("theme_bg", "#080114") ?: "#080114"
        val surface = prefs.getString("theme_surface", "#120826") ?: "#120826"
        val scale = prefs.getFloat("theme_scale", 1.0f)
        val radius = prefs.getInt("theme_radius", 16)
        val glow = prefs.getString("theme_glow", "High") ?: "High"
        val stars = prefs.getBoolean("theme_stars", true)
        val deviceOnly = prefs.getBoolean("theme_device_only", false)

        return CustomThemeConfig(
            isCustomEnabled = enabled,
            activePaletteName = palette,
            primaryColorHex = primary,
            secondaryColorHex = secondary,
            tertiaryColorHex = tertiary,
            backgroundColorHex = bg,
            surfaceColorHex = surface,
            textScale = scale,
            cornerRadiusDp = radius,
            glowLevel = glow,
            showStars = stars,
            isDeviceOnly = deviceOnly
        )
    }

    private fun saveCustomThemeConfig(config: CustomThemeConfig) {
        prefs.edit()
            .putBoolean("theme_custom_enabled", config.isCustomEnabled)
            .putString("theme_palette", config.activePaletteName)
            .putString("theme_primary", config.primaryColorHex)
            .putString("theme_secondary", config.secondaryColorHex)
            .putString("theme_tertiary", config.tertiaryColorHex)
            .putString("theme_bg", config.backgroundColorHex)
            .putString("theme_surface", config.surfaceColorHex)
            .putFloat("theme_scale", config.textScale)
            .putInt("theme_radius", config.cornerRadiusDp)
            .putString("theme_glow", config.glowLevel)
            .putBoolean("theme_stars", config.showStars)
            .putBoolean("theme_device_only", config.isDeviceOnly)
            .apply()
    }

    // --- BACKUP & RESTORE ACTIONS ---

    suspend fun exportHouseholdBackup(): String {
        val hub = activeHub.value ?: throw IllegalStateException("No active family hub selected")
        return repository.exportHubDataAsJson(hub.id)
    }

    suspend fun importHouseholdBackup(jsonString: String): Map<String, Int> {
        val hub = activeHub.value ?: throw IllegalStateException("No active family hub selected")
        return repository.importHubDataFromJson(hub.id, hub.memberUids, jsonString)
    }

    class Factory(
        private val context: Context,
        private val repository: FamilyHubRepository,
        private val currentUserId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HubViewModel(context.applicationContext, repository, currentUserId) as T
        }
    }
}
