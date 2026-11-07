package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.models.Appointment
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
        notes: String
    ) {
        val uid = requireUserId()
        val docRef = db.collection("hubs").document(hubId).collection("groceries").document()
        val payload = mapOf(
            "hubId" to hubId,
            "memberUids" to (if (memberUids.contains(uid)) memberUids else memberUids + uid),
            "title" to title.trim(),
            "category" to category,
            "quantity" to quantity,
            "unit" to unit,
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
                description = "Added $quantity $unit to $category"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleGroceryCompleted(item: GroceryItem, memberUids: List<String> = emptyList()) {
        val docRef = db.collection("hubs").document(item.hubId).collection("groceries").document(item.id)
        val newStatus = !item.isCompleted
        try {
            docRef.update(
                "isCompleted", newStatus,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()

            recordActivity(
                hubId = item.hubId,
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Silently log; activity auditing is best-effort and shouldn't block primary user operations
            try {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
            } catch (_: Exception) {}
        }
    }
}
