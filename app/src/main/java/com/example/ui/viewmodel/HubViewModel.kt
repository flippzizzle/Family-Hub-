package com.example.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.models.ActivityLog
import com.example.data.models.Appointment
import com.example.data.models.GroceryItem
import com.example.data.models.Hub
import com.example.data.models.Member
import com.example.data.repository.FamilyHubRepository
import com.example.util.AppointmentAlarmScheduler
import com.example.util.NetworkMonitor
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
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

class HubViewModel(
    private val context: Context,
    private val repository: FamilyHubRepository,
    val currentUserId: String
) : ViewModel() {

    private val auth = Firebase.auth
    val currentUserEmail: String = auth.currentUser?.email.orEmpty().lowercase()
    val currentDisplayName: String = auth.currentUser?.displayName ?: "Family Pilot"

    private val networkMonitor = NetworkMonitor(context)
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = true
    )

    private val _selectedHubId = MutableStateFlow<String?>(null)
    val selectedHubId: StateFlow<String?> = _selectedHubId.asStateFlow()

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

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
    }.catch { e ->
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
            } catch (e: Exception) {
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

    fun addGrocery(
        title: String,
        category: String,
        quantity: Long,
        unit: String,
        assignedTo: String,
        notes: String
    ) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.addGroceryItem(
                    hubId = hub.id,
                    memberUids = hub.memberUids,
                    title = title,
                    category = category,
                    quantity = quantity,
                    unit = unit,
                    assignedTo = assignedTo,
                    notes = notes
                )
            } catch (e: Exception) {
                _errorBanner.value = "Failed to add grocery: ${e.localizedMessage}"
            }
        }
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
            } catch (e: Exception) {
                _errorBanner.value = "Failed to create appointment: ${e.localizedMessage}"
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

    fun updateTheme(themePalette: String, glowIntensity: String, ambientStars: Boolean) {
        val hub = activeHub.value ?: return
        viewModelScope.launch {
            try {
                repository.updateHubTheme(hub.id, themePalette, glowIntensity, ambientStars)
            } catch (e: Exception) {
                _errorBanner.value = "Failed to update theme: ${e.localizedMessage}"
            }
        }
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
