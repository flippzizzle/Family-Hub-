package com.example.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Hub(
    @DocumentId val id: String = "",
    val name: String = "Cosmic Family Hub",
    val ownerId: String = "",
    val adminUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val whitelistedEmails: List<String> = emptyList(),
    val themePalette: String = "Neon Galaxy", // Neon Galaxy, Cyber Magenta, Retro Cyan, Sunset Horizon
    val glowIntensity: String = "High",
    val ambientStars: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GroceryItem(
    @DocumentId val id: String = "",
    val hubId: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val category: String = "Produce", // Produce, Bakery, Dairy, Pantry, Frozen, Snacks, Beverages, Household, Other
    val quantity: Long = 1L,
    val unit: String = "pcs",
    val isCompleted: Boolean = false,
    val addedBy: String = "",
    val addedByUid: String = "",
    val assignedTo: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Appointment(
    @DocumentId val id: String = "",
    val hubId: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val startTime: Timestamp? = null,
    val endTime: Timestamp? = null,
    val category: String = "Doctor", // Doctor, School, Sports, Social, Family, Work, Other
    val colorTag: String = "#FF007F", // Neon Pink (#FF007F), Neon Cyan (#00F0FF), Neon Purple (#9D00FF), Electric Yellow (#FFE600), Sunset Orange (#FF6B35)
    val googleCalendarEventId: String = "",
    val syncedWithGoogle: Boolean = false,
    val reminderMinutes: Long = 60L, // 0 = at time, 15, 60, 1440 (1 day), -1 = none
    val createdBy: String = "",
    val createdByUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Member(
    @DocumentId val id: String = "",
    val hubId: String = "",
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "member", // admin, member
    val avatarIcon: String = "astronaut", // astronaut, star, planet, comet, alien, rocket
    val deviceInfo: String = "Android Device",
    @ServerTimestamp val lastActive: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ActivityLog(
    @DocumentId val id: String = "",
    val hubId: String = "",
    val memberUids: List<String> = emptyList(),
    val userName: String = "",
    val userEmail: String = "",
    val actionType: String = "grocery_added", // grocery_added, grocery_completed, appointment_added, member_joined, theme_changed
    val itemTitle: String = "",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
