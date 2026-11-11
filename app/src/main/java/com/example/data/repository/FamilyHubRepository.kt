package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.models.Appointment
import com.example.data.models.GroceryFavorite
import com.example.data.models.GroceryItem
import com.example.data.models.Hub
import com.example.data.models.Member
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.example.data.models.ActivityLog
import com.example.data.models.Chore
import com.example.data.models.MealPlanDay
import com.example.data.models.MasterMealItem
import com.example.data.models.HouseholdNote
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FamilyHubRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ).apply {
            try {
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
            } catch (e: Exception) {
                // If settings were already applied, continue
            }
        }
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun currentUserEmail(): String {
        return auth.currentUser?.email.orEmpty()
    }

    // Observe all hubs where user is owner or member
    fun observeUserHubs(userId: String): Flow<List<Hub>> = flow {
        val path = "hubs"
        emitAll(
            db.collection("hubs")
                .whereArrayContains("memberUids", userId)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Hub::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // Observe hubs where user's Google email is on the safe whitelist
    fun observeWhitelistedHubs(userEmail: String): Flow<List<Hub>> = flow {
        val email = userEmail.trim().lowercase()
        if (email.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "hubs"
        emitAll(
            db.collection("hubs")
                .whereArrayContains("whitelistedEmails", email)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Hub::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // Observe specific Hub
    fun observeHub(hubId: String): Flow<Hub?> = flow {
        val uid = requireUserId()
        val path = "hubs/$hubId"
        emitAll(
            db.collection("hubs").document(hubId)
                .snapshots()
                .map { snapshot -> snapshot.toObject(Hub::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createHub(
        name: String,
        themePalette: String = "Neon Galaxy",
        glowIntensity: String = "High",
        ambientStars: Boolean = true
    ): String {
        val uid = requireUserId()
        val email = currentUserEmail()
        val docRef = db.collection("hubs").document()
        val payload = mapOf(
            "name" to name,
            "ownerId" to uid,
            "adminUids" to listOf(uid),
            "memberUids" to listOf(uid),
            "whitelistedEmails" to if (email.isNotBlank()) listOf(email) else emptyList<String>(),
            "themePalette" to themePalette,
            "glowIntensity" to glowIntensity,
            "ambientStars" to ambientStars,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            recordActivity(
                hubId = docRef.id,
                memberUids = listOf(uid),
                actionType = "hub_created",
                itemTitle = name,
                description = "Cosmic Family Hub founded and launched"
            )
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateHubTheme(
        hubId: String,
        themePalette: String,
        glowIntensity: String,
        ambientStars: Boolean
    ) {
        val docRef = db.collection("hubs").document(hubId)
        val updates = mapOf(
            "themePalette" to themePalette,
            "glowIntensity" to glowIntensity,
            "ambientStars" to ambientStars,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun whitelistEmail(hubId: String, memberUids: List<String>, email: String) {
        val docRef = db.collection("hubs").document(hubId)
        val normalized = email.trim().lowercase()
        try {
            docRef.update(
                "whitelistedEmails", FieldValue.arrayUnion(normalized),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            recordActivity(
                hubId = hubId,
                memberUids = memberUids,
                actionType = "whitelist_added",
                itemTitle = normalized,
                description = "Invited and whitelisted for family hub sync"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun removeWhitelistedEmail(hubId: String, memberUids: List<String>, email: String) {
        val docRef = db.collection("hubs").document(hubId)
        val normalized = email.trim().lowercase()
        try {
            docRef.update(
                "whitelistedEmails", FieldValue.arrayRemove(normalized),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            recordActivity(
                hubId = hubId,
                memberUids = memberUids,
                actionType = "whitelist_removed",
                itemTitle = normalized,
                description = "Access revoked from household whitelist"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun joinHubIfWhitelisted(hubId: String, member: Member) {
        val uid = requireUserId()
        val hubRef = db.collection("hubs").document(hubId)
        try {
            hubRef.update(
                "memberUids", FieldValue.arrayUnion(uid),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            registerOrUpdateMember(hubId, member)

            recordActivity(
                hubId = hubId,
                memberUids = listOf(uid),
                actionType = "member_joined",
                itemTitle = member.displayName.ifBlank { "Family Member" },
                description = "Connected device to the central hub (${member.deviceInfo})"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, hubRef.path)
            throw e
        }
    }

    // Members
    fun observeMembers(hubId: String): Flow<List<Member>> = flow {
        val uid = requireUserId()
        val path = "hubs/$hubId/members"
        emitAll(
            db.collection("hubs").document(hubId).collection("members")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Member::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun registerOrUpdateMember(hubId: String, member: Member) {
        val uid = requireUserId()
        val memberDocRef = db.collection("hubs").document(hubId).collection("members").document(uid)
        val payload = mapOf(
            "hubId" to hubId,
            "uid" to uid,
            "email" to (member.email.ifBlank { currentUserEmail() }),
            "displayName" to (member.displayName.ifBlank { "Starlight Member" }),
            "role" to member.role,
            "avatarIcon" to member.avatarIcon,
            "deviceInfo" to member.deviceInfo,
            "lastActive" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            memberDocRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, memberDocRef.path)
            throw e
        }
    }

    suspend fun setMemberRole(hubId: String, targetUid: String, newRole: String) {
        val hubRef = db.collection("hubs").document(hubId)
        val memberDocRef = hubRef.collection("members").document(targetUid)
        try {
            memberDocRef.update(
                "role", newRole,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            if (newRole == "admin") {
                hubRef.update("adminUids", FieldValue.arrayUnion(targetUid), "updatedAt", FieldValue.serverTimestamp()).await()
            } else {
                hubRef.update("adminUids", FieldValue.arrayRemove(targetUid), "updatedAt", FieldValue.serverTimestamp()).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, memberDocRef.path)
            throw e
        }
    }

    suspend fun removeMemberFromHub(hubId: String, targetUid: String) {
        val hubRef = db.collection("hubs").document(hubId)
        val memberDocRef = hubRef.collection("members").document(targetUid)
        try {
            memberDocRef.delete().await()
            hubRef.update(
                "memberUids", FieldValue.arrayRemove(targetUid),
                "adminUids", FieldValue.arrayRemove(targetUid),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, memberDocRef.path)
            throw e
        }
    }

    // Grocery Items
    fun observeGroceries(hubId: String): Flow<List<GroceryItem>> = flow {
        val uid = requireUserId()
        val path = "hubs/$hubId/groceries"
        emitAll(
            db.collection("hubs").document(hubId).collection("groceries")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(GroceryItem::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addGroceryItem(
        hubId: String,
        memberUids: List<String>,
        title: String,
        category: String,
        quantity: Long,
        unit: String,
        assignedTo: String,
        notes: String,
        store: String = "Kroger",
        source: String = "manual",
        isHistorical: Boolean = false
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("hubs").document(hubId).collection("groceries").document()
        val payload = mapOf(
            "hubId" to hubId,
            "memberUids" to (if (memberUids.contains(uid)) memberUids else memberUids + uid),
            "title" to title.trim(),
            "category" to category,
            "quantity" to quantity,
            "unit" to unit,
            "store" to store,
            "source" to source,
            "isHistorical" to isHistorical,
            "isCompleted" to false,
            "addedBy" to (auth.currentUser?.displayName ?: "Family"),
            "addedByUid" to uid,
            "assignedTo" to assignedTo,
            "notes" to notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            recordActivity(
                hubId = hubId,
                memberUids = memberUids,
                actionType = "grocery_added",
                itemTitle = title.trim(),
                description = "Added $quantity $unit ($store) to $category"
            )
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleGroceryCompleted(hubId: String, item: GroceryItem, memberUids: List<String> = emptyList()) {
        val targetHubId = item.hubId.ifBlank { hubId }
        val docRef = db.collection("hubs").document(targetHubId).collection("groceries").document(item.id)
        val newStatus = !item.isCompleted
        try {
            docRef.update(
                "isCompleted", newStatus,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            recordActivity(
                hubId = targetHubId,
                memberUids = if (memberUids.isNotEmpty()) memberUids else item.memberUids,
                actionType = if (newStatus) "grocery_completed" else "grocery_reopened",
                itemTitle = item.title,
                description = if (newStatus) "Marked as purchased / completed" else "Returned to active shopping list"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun restoreGroceryItem(hubId: String, item: GroceryItem, memberUids: List<String> = emptyList()) {
        val targetHubId = item.hubId.ifBlank { hubId }
        val docRef = db.collection("hubs").document(targetHubId).collection("groceries").document(item.id)
        try {
            docRef.update(
                "isCompleted", false,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            recordActivity(
                hubId = targetHubId,
                memberUids = if (memberUids.isNotEmpty()) memberUids else item.memberUids,
                actionType = "grocery_reopened",
                itemTitle = item.title,
                description = "Restored '${item.title}' to active shopping list"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun clearCompletedGroceries(hubId: String) {
        val groceriesRef = db.collection("hubs").document(hubId).collection("groceries")
        try {
            val snapshot = groceriesRef.whereEqualTo("isCompleted", true).get().await()
            val batch = db.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, groceriesRef.path)
            throw e
        }
    }

    suspend fun deleteGroceryItem(hubId: String, itemId: String) {
        val docRef = db.collection("hubs").document(hubId).collection("groceries").document(itemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- Grocery Favorites (Permanent Household Collection) ---

    fun observeGroceryFavorites(hubId: String): Flow<List<GroceryFavorite>> = flow {
        val uid = requireUserId()
        val path = "hubs/$hubId/grocery_favorites"
        emitAll(
            db.collection("hubs").document(hubId).collection("grocery_favorites")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(GroceryFavorite::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun toggleGroceryFavorite(
        hubId: String,
        name: String,
        category: String,
        preferredStore: String = "Kroger",
        source: String = "catalog"
    ): Boolean {
        val uid = requireUserId()
        val cleanName = name.trim()
        val docId = cleanName.lowercase().replace(Regex("[^a-z0-9]"), "_").take(60)
        val docRef = db.collection("hubs").document(hubId).collection("grocery_favorites").document(docId)

        try {
            val existing = docRef.get().await()
            return if (existing.exists()) {
                docRef.delete().await()
                false // now unfavorited
            } else {
                val payload = mapOf(
                    "hubId" to hubId,
                    "name" to cleanName,
                    "category" to category,
                    "preferredStore" to preferredStore,
                    "source" to source,
                    "favoritedByUid" to uid,
                    "favoritedByName" to (auth.currentUser?.displayName ?: "Family"),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                true // now favorited
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun removeGroceryFavorite(hubId: String, favoriteId: String) {
        val docRef = db.collection("hubs").document(hubId).collection("grocery_favorites").document(favoriteId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // Appointments
    fun observeAppointments(hubId: String): Flow<List<Appointment>> = flow {
        val uid = requireUserId()
        val path = "hubs/$hubId/appointments"
        emitAll(
            db.collection("hubs").document(hubId).collection("appointments")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Appointment::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addAppointment(
        hubId: String,
        memberUids: List<String>,
        title: String,
        description: String,
        location: String,
        startTime: Timestamp,
        endTime: Timestamp,
        category: String,
        colorTag: String,
        reminderMinutes: Long = 60L
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("hubs").document(hubId).collection("appointments").document()
        val payload = mapOf(
            "hubId" to hubId,
            "memberUids" to (if (memberUids.contains(uid)) memberUids else memberUids + uid),
            "title" to title.trim(),
            "description" to description,
            "location" to location,
            "startTime" to startTime,
            "endTime" to endTime,
            "category" to category,
            "colorTag" to colorTag,
            "googleCalendarEventId" to "",
            "syncedWithGoogle" to false,
            "reminderMinutes" to reminderMinutes,
            "createdBy" to (auth.currentUser?.displayName ?: "Family"),
            "createdByUid" to uid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            val reminderLabel = when (reminderMinutes) {
                0L -> "at event start"
                15L -> "15m before"
                60L -> "1h before"
                1440L -> "1 day before"
                else -> "no alarm"
            }
            recordActivity(
                hubId = hubId,
                memberUids = memberUids,
                actionType = "appointment_added",
                itemTitle = title.trim(),
                description = "Scheduled for $category (Alarm: $reminderLabel)"
            )
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateAppointmentGoogleSync(
        hubId: String,
        appointmentId: String,
        googleCalendarEventId: String,
        synced: Boolean
    ) {
        val docRef = db.collection("hubs").document(hubId).collection("appointments").document(appointmentId)
        try {
            docRef.update(
                "googleCalendarEventId", googleCalendarEventId,
                "syncedWithGoogle", synced,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteAppointment(hubId: String, appointmentId: String) {
        val docRef = db.collection("hubs").document(hubId).collection("appointments").document(appointmentId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // Activity Log Subcollection
    fun observeActivityLog(hubId: String): Flow<List<ActivityLog>> = flow {
        val uid = requireUserId()
        val path = "hubs/$hubId/activity"
        emitAll(
            db.collection("hubs").document(hubId).collection("activity")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(40)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(ActivityLog::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun recordActivity(
        hubId: String,
        memberUids: List<String>,
        actionType: String,
        itemTitle: String,
        description: String
    ) {
        val uid = requireUserId()
        val docRef = db.collection("hubs").document(hubId).collection("activity").document()
        val userName = auth.currentUser?.displayName?.ifBlank { "Family Member" } ?: "Family Member"
        val userEmail = currentUserEmail()
        val payload = mapOf(
            "hubId" to hubId,
            "memberUids" to (if (memberUids.contains(uid)) memberUids else memberUids + uid),
            "userName" to userName,
            "userEmail" to userEmail,
            "actionType" to actionType,
            "itemTitle" to itemTitle,
            "description" to description,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: Exception) {
            // Non-fatal activity log
        }
    }

    // Appointments - Two-Way Google Sync Upsert
    suspend fun upsertGoogleSyncedAppointment(hubId: String, appointment: Appointment): Boolean {
        val uid = requireUserId()
        val appointmentsCol = db.collection("hubs").document(hubId).collection("appointments")

        try {
            // Check if appointment with matching googleCalendarEventId exists
            val existingByGoogleId = if (appointment.googleCalendarEventId.isNotBlank()) {
                appointmentsCol
                    .whereEqualTo("googleCalendarEventId", appointment.googleCalendarEventId)
                    .limit(1)
                    .get()
                    .await()
            } else null

            if (existingByGoogleId != null && !existingByGoogleId.isEmpty) {
                val doc = existingByGoogleId.documents.first()
                val updates = mapOf(
                    "title" to appointment.title,
                    "description" to appointment.description,
                    "location" to appointment.location,
                    "startTime" to appointment.startTime,
                    "endTime" to appointment.endTime,
                    "syncedWithGoogle" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                doc.reference.update(updates).await()
                return false // updated, not newly created
            }

            // Also check by exact title and startTime to prevent duplicates
            if (appointment.startTime != null) {
                val existingByTitleAndTime = appointmentsCol
                    .whereEqualTo("title", appointment.title)
                    .whereEqualTo("startTime", appointment.startTime)
                    .limit(1)
                    .get()
                    .await()

                if (!existingByTitleAndTime.isEmpty) {
                    val doc = existingByTitleAndTime.documents.first()
                    doc.reference.update(
                        mapOf(
                            "googleCalendarEventId" to appointment.googleCalendarEventId,
                            "syncedWithGoogle" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    return false
                }
            }

            // Insert new appointment
            val docRef = appointmentsCol.document()
            val payload = mapOf(
                "hubId" to hubId,
                "memberUids" to (if (appointment.memberUids.contains(uid)) appointment.memberUids else appointment.memberUids + uid),
                "title" to appointment.title.trim(),
                "description" to appointment.description,
                "location" to appointment.location,
                "startTime" to appointment.startTime,
                "endTime" to appointment.endTime,
                "category" to appointment.category,
                "colorTag" to appointment.colorTag,
                "googleCalendarEventId" to appointment.googleCalendarEventId,
                "syncedWithGoogle" to true,
                "reminderMinutes" to appointment.reminderMinutes,
                "createdBy" to (auth.currentUser?.displayName ?: "Google Calendar"),
                "createdByUid" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    // Chores Subcollection
    fun observeChores(hubId: String): Flow<List<Chore>> = flow {
        val uid = requireUserId()
        val path = "hubs/$hubId/chores"
        emitAll(
            db.collection("hubs").document(hubId).collection("chores")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Chore::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addChore(
        hubId: String,
        memberUids: List<String>,
        title: String,
        description: String,
        assignedToUid: String,
        assignedToName: String,
        frequency: String,
        dueDate: Timestamp?,
        points: Long = 10L
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("hubs").document(hubId).collection("chores").document()
        val payload = mapOf(
            "hubId" to hubId,
            "memberUids" to (if (memberUids.contains(uid)) memberUids else memberUids + uid),
            "title" to title.trim(),
            "description" to description,
            "assignedToUid" to assignedToUid,
            "assignedToName" to assignedToName.ifBlank { "Anyone" },
            "frequency" to frequency,
            "dueDate" to dueDate,
            "isCompleted" to false,
            "points" to points,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            recordActivity(
                hubId = hubId,
                memberUids = memberUids,
                actionType = "chore_added",
                itemTitle = title.trim(),
                description = "Assigned to $assignedToName ($frequency, $points pts)"
            )
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleChoreCompleted(chore: Chore, memberUids: List<String> = emptyList()) {
        val docRef = db.collection("hubs").document(chore.hubId).collection("chores").document(chore.id)
        val uid = requireUserId()
        val displayName = auth.currentUser?.displayName ?: "Family Member"
        val newStatus = !chore.isCompleted

        val updates = if (newStatus) {
            mapOf(
                "isCompleted" to true,
                "completedAt" to FieldValue.serverTimestamp(),
                "completedByUid" to uid,
                "completedByName" to displayName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        } else {
            mapOf(
                "isCompleted" to false,
                "completedAt" to null,
                "completedByUid" to "",
                "completedByName" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }

        try {
            docRef.update(updates).await()
            recordActivity(
                hubId = chore.hubId,
                memberUids = if (memberUids.isNotEmpty()) memberUids else chore.memberUids,
                actionType = if (newStatus) "chore_completed" else "chore_reopened",
                itemTitle = chore.title,
                description = if (newStatus) "Completed by $displayName (+${chore.points} pts)" else "Marked incomplete"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteChore(hubId: String, choreId: String) {
        val docRef = db.collection("hubs").document(hubId).collection("chores").document(choreId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // Meal Plans Subcollection
    fun observeMealPlans(hubId: String): Flow<List<MealPlanDay>> = flow {
        val uid = requireUserId()
        val path = "hubs/$hubId/meal_plans"
        emitAll(
            db.collection("hubs").document(hubId).collection("meal_plans")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(MealPlanDay::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun setMealPlanDay(
        hubId: String,
        memberUids: List<String>,
        plan: MealPlanDay
    ) {
        val uid = requireUserId()
        val docRef = db.collection("hubs").document(hubId).collection("meal_plans").document(plan.dateString)
        val payload = mapOf(
            "hubId" to hubId,
            "memberUids" to (if (memberUids.contains(uid)) memberUids else memberUids + uid),
            "dateString" to plan.dateString,
            "dayOfWeek" to plan.dayOfWeek,
            "mealId" to plan.mealId,
            "mealName" to plan.mealName,
            "tier" to plan.tier,
            "notes" to plan.notes,
            "assignedCook" to plan.assignedCook,
            "isFavorite" to plan.isFavorite,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            if (plan.mealName.isNotBlank()) {
                recordActivity(
                    hubId = hubId,
                    memberUids = memberUids,
                    actionType = "meal_planned",
                    itemTitle = plan.mealName,
                    description = "Planned for ${plan.dayOfWeek} (${plan.tier})"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun clearMealPlanDay(hubId: String, dateString: String) {
        val docRef = db.collection("hubs").document(hubId).collection("meal_plans").document(dateString)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // Custom Meals Subcollection
    fun observeCustomMeals(hubId: String): Flow<List<MasterMealItem>> = flow {
        val uid = requireUserId()
        val path = "hubs/$hubId/custom_meals"
        emitAll(
            db.collection("hubs").document(hubId).collection("custom_meals")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(MasterMealItem::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addCustomMeal(
        hubId: String,
        memberUids: List<String>,
        name: String,
        costAndEffortTier: String,
        notes: String
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("hubs").document(hubId).collection("custom_meals").document()
        val payload = mapOf(
            "hubId" to hubId,
            "memberUids" to (if (memberUids.contains(uid)) memberUids else memberUids + uid),
            "name" to name.trim(),
            "costAndEffortTier" to costAndEffortTier,
            "notes" to notes,
            "isCustom" to true,
            "isFavorite" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            recordActivity(
                hubId = hubId,
                memberUids = memberUids,
                actionType = "custom_meal_added",
                itemTitle = name.trim(),
                description = "Added to household meal library ($costAndEffortTier)"
            )
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCustomMeal(hubId: String, mealId: String) {
        val docRef = db.collection("hubs").document(hubId).collection("custom_meals").document(mealId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // Household Notes / Message Board
    fun observeNotes(hubId: String): Flow<List<HouseholdNote>> = flow {
        val uid = requireUserId()
        val path = "hubs/$hubId/notes"
        emitAll(
            db.collection("hubs").document(hubId).collection("notes")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(HouseholdNote::class.java).sortedWith(compareByDescending<HouseholdNote> { it.isPinned }.thenByDescending { it.createdAt?.seconds ?: 0 }) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addNote(
        hubId: String,
        memberUids: List<String>,
        title: String,
        content: String,
        category: String = "General",
        colorTag: String = "#00F5FF",
        isPinned: Boolean = false
    ): String {
        val uid = requireUserId()
        val docRef = db.collection("hubs").document(hubId).collection("notes").document()
        val payload = mapOf(
            "hubId" to hubId,
            "memberUids" to (if (memberUids.contains(uid)) memberUids else memberUids + uid),
            "title" to title.trim(),
            "content" to content.trim(),
            "authorUid" to uid,
            "authorName" to (auth.currentUser?.displayName ?: "Family Member"),
            "authorEmail" to currentUserEmail(),
            "category" to category,
            "colorTag" to colorTag,
            "isPinned" to isPinned,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            recordActivity(
                hubId = hubId,
                memberUids = memberUids,
                actionType = "note_posted",
                itemTitle = title.trim(),
                description = "Posted to Family Board by ${auth.currentUser?.displayName ?: "Family"}"
            )
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateNote(
        hubId: String,
        noteId: String,
        title: String,
        content: String,
        category: String,
        colorTag: String,
        isPinned: Boolean
    ) {
        val docRef = db.collection("hubs").document(hubId).collection("notes").document(noteId)
        val updates = mapOf(
            "title" to title.trim(),
            "content" to content.trim(),
            "category" to category,
            "colorTag" to colorTag,
            "isPinned" to isPinned,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteNote(hubId: String, noteId: String) {
        val docRef = db.collection("hubs").document(hubId).collection("notes").document(noteId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // Household JSON Backup & Safe Restore
    suspend fun exportHubDataAsJson(hubId: String): String {
        val hubRef = db.collection("hubs").document(hubId)
        val hubDoc = hubRef.get().await().toObject(Hub::class.java)

        val groceries = hubRef.collection("groceries").get().await().toObjects(GroceryItem::class.java)
        val appointments = hubRef.collection("appointments").get().await().toObjects(Appointment::class.java)
        val chores = hubRef.collection("chores").get().await().toObjects(Chore::class.java)
        val mealPlans = hubRef.collection("meal_plans").get().await().toObjects(MealPlanDay::class.java)
        val customMeals = hubRef.collection("custom_meals").get().await().toObjects(MasterMealItem::class.java)
        val notes = hubRef.collection("notes").get().await().toObjects(HouseholdNote::class.java)

        val root = JSONObject().apply {
            put("app", "Cosmic Family Hub")
            put("version", "2.0")
            put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
            put("hubName", hubDoc?.name ?: "Family Hub")

            val groceriesArray = JSONArray()
            groceries.forEach { item ->
                groceriesArray.put(JSONObject().apply {
                    put("title", item.title)
                    put("category", item.category)
                    put("quantity", item.quantity)
                    put("unit", item.unit)
                    put("store", item.store)
                    put("notes", item.notes)
                    put("isCompleted", item.isCompleted)
                })
            }
            put("groceries", groceriesArray)

            val appointmentsArray = JSONArray()
            appointments.forEach { appt ->
                appointmentsArray.put(JSONObject().apply {
                    put("title", appt.title)
                    put("description", appt.description)
                    put("location", appt.location)
                    put("category", appt.category)
                    put("colorTag", appt.colorTag)
                    put("startTime", appt.startTime?.toDate()?.time ?: 0L)
                    put("endTime", appt.endTime?.toDate()?.time ?: 0L)
                    put("reminderMinutes", appt.reminderMinutes)
                    put("googleCalendarEventId", appt.googleCalendarEventId)
                })
            }
            put("appointments", appointmentsArray)

            val choresArray = JSONArray()
            chores.forEach { chore ->
                choresArray.put(JSONObject().apply {
                    put("title", chore.title)
                    put("description", chore.description)
                    put("assignedToName", chore.assignedToName)
                    put("frequency", chore.frequency)
                    put("points", chore.points)
                    put("isCompleted", chore.isCompleted)
                })
            }
            put("chores", choresArray)

            val mealsArray = JSONArray()
            mealPlans.forEach { meal ->
                mealsArray.put(JSONObject().apply {
                    put("dateString", meal.dateString)
                    put("dayOfWeek", meal.dayOfWeek)
                    put("mealName", meal.mealName)
                    put("tier", meal.tier)
                    put("notes", meal.notes)
                    put("assignedCook", meal.assignedCook)
                })
            }
            put("mealPlans", mealsArray)

            val customMealsArray = JSONArray()
            customMeals.forEach { cm ->
                customMealsArray.put(JSONObject().apply {
                    put("name", cm.name)
                    put("costAndEffortTier", cm.costAndEffortTier)
                    put("notes", cm.notes)
                })
            }
            put("customMeals", customMealsArray)

            val notesArray = JSONArray()
            notes.forEach { note ->
                notesArray.put(JSONObject().apply {
                    put("title", note.title)
                    put("content", note.content)
                    put("category", note.category)
                    put("colorTag", note.colorTag)
                    put("isPinned", note.isPinned)
                    put("authorName", note.authorName)
                })
            }
            put("notes", notesArray)
        }

        return root.toString(2)
    }

    suspend fun importHubDataFromJson(
        hubId: String,
        memberUids: List<String>,
        jsonString: String
    ): Map<String, Int> {
        val root = JSONObject(jsonString)
        val counts = mutableMapOf("groceries" to 0, "appointments" to 0, "chores" to 0, "mealPlans" to 0, "notes" to 0)

        // Groceries Import (supports both Family Hub backup and older HTML exports)
        val groceriesArray = root.optJSONArray("groceries") ?: root.optJSONArray("groceryList")
        if (groceriesArray != null) {
            val existingGroceries = db.collection("hubs").document(hubId).collection("groceries")
                .get().await().toObjects(GroceryItem::class.java).map { it.title.lowercase() }.toSet()

            for (i in 0 until groceriesArray.length()) {
                val itemObj = groceriesArray.getJSONObject(i)
                val title = itemObj.optString("title", itemObj.optString("name", "")).trim()
                if (title.isNotBlank() && !existingGroceries.contains(title.lowercase())) {
                    addGroceryItem(
                        hubId = hubId,
                        memberUids = memberUids,
                        title = title,
                        category = itemObj.optString("category", "Produce"),
                        quantity = itemObj.optLong("quantity", 1L),
                        unit = itemObj.optString("unit", "pcs"),
                        assignedTo = itemObj.optString("assignedTo", ""),
                        notes = itemObj.optString("notes", ""),
                        store = itemObj.optString("store", "Kroger"),
                        source = "backup_import"
                    )
                    counts["groceries"] = (counts["groceries"] ?: 0) + 1
                }
            }
        }

        // Appointments Import
        val appointmentsArray = root.optJSONArray("appointments") ?: root.optJSONArray("calendarEvents")
        if (appointmentsArray != null) {
            val existingTitles = db.collection("hubs").document(hubId).collection("appointments")
                .get().await().toObjects(Appointment::class.java).map { it.title.lowercase() }.toSet()

            for (i in 0 until appointmentsArray.length()) {
                val apptObj = appointmentsArray.getJSONObject(i)
                val title = apptObj.optString("title", "").trim()
                if (title.isNotBlank() && !existingTitles.contains(title.lowercase())) {
                    val startMs = apptObj.optLong("startTime", System.currentTimeMillis() + 86400000L)
                    val endMs = apptObj.optLong("endTime", startMs + 3600000L)
                    addAppointment(
                        hubId = hubId,
                        memberUids = memberUids,
                        title = title,
                        description = apptObj.optString("description", ""),
                        location = apptObj.optString("location", ""),
                        startTime = Timestamp(Date(startMs)),
                        endTime = Timestamp(Date(endMs)),
                        category = apptObj.optString("category", "Family"),
                        colorTag = apptObj.optString("colorTag", "#FF007F"),
                        reminderMinutes = apptObj.optLong("reminderMinutes", 60L)
                    )
                    counts["appointments"] = (counts["appointments"] ?: 0) + 1
                }
            }
        }

        // Chores Import
        val choresArray = root.optJSONArray("chores")
        if (choresArray != null) {
            val existingChores = db.collection("hubs").document(hubId).collection("chores")
                .get().await().toObjects(Chore::class.java).map { it.title.lowercase() }.toSet()

            for (i in 0 until choresArray.length()) {
                val choreObj = choresArray.getJSONObject(i)
                val title = choreObj.optString("title", "").trim()
                if (title.isNotBlank() && !existingChores.contains(title.lowercase())) {
                    addChore(
                        hubId = hubId,
                        memberUids = memberUids,
                        title = title,
                        description = choreObj.optString("description", ""),
                        assignedToUid = "",
                        assignedToName = choreObj.optString("assignedToName", "Anyone"),
                        frequency = choreObj.optString("frequency", "daily"),
                        dueDate = null,
                        points = choreObj.optLong("points", 10L)
                    )
                    counts["chores"] = (counts["chores"] ?: 0) + 1
                }
            }
        }

        // Notes Import
        val notesArray = root.optJSONArray("notes") ?: root.optJSONArray("messages")
        if (notesArray != null) {
            for (i in 0 until notesArray.length()) {
                val noteObj = notesArray.getJSONObject(i)
                val title = noteObj.optString("title", "").trim()
                val content = noteObj.optString("content", noteObj.optString("text", "")).trim()
                if (title.isNotBlank() || content.isNotBlank()) {
                    addNote(
                        hubId = hubId,
                        memberUids = memberUids,
                        title = title.ifBlank { "Imported Note" },
                        content = content,
                        category = noteObj.optString("category", "General"),
                        colorTag = noteObj.optString("colorTag", "#00F5FF"),
                        isPinned = noteObj.optBoolean("isPinned", false)
                    )
                    counts["notes"] = (counts["notes"] ?: 0) + 1
                }
            }
        }

        return counts
    }
}
