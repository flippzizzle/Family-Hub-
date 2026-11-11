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
    val store: String = "Kroger", // Kroger, Amazon, Other
    val source: String = "manual", // receipt, catalog, manual
    val isHistorical: Boolean = false,
    val isCompleted: Boolean = false,
    val addedBy: String = "",
    val addedByUid: String = "",
    val assignedTo: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class GroceryFavorite(
    @DocumentId val id: String = "",
    val hubId: String = "",
    val name: String = "",
    val category: String = "Produce",
    val preferredStore: String = "Kroger",
    val source: String = "catalog", // catalog, receipt, manual
    val favoritedByUid: String = "",
    val favoritedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
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
    val actionType: String = "grocery_added", // grocery_added, grocery_completed, chore_completed, appointment_added, etc.
    val itemTitle: String = "",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Chore(
    @DocumentId val id: String = "",
    val hubId: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val description: String = "",
    val assignedToUid: String = "",
    val assignedToName: String = "Anyone",
    val frequency: String = "daily", // once, daily, weekly
    val dueDate: Timestamp? = null,
    val isCompleted: Boolean = false,
    val completedAt: Timestamp? = null,
    val completedByUid: String = "",
    val completedByName: String = "",
    val points: Long = 10L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MealPlanDay(
    @DocumentId val id: String = "", // e.g. "2026-11-16"
    val hubId: String = "",
    val memberUids: List<String> = emptyList(),
    val dateString: String = "", // "yyyy-MM-dd"
    val dayOfWeek: String = "", // "Monday", "Tuesday", etc.
    val mealId: String = "",
    val mealName: String = "",
    val tier: String = "Cheapest & Easiest", // Cheapest & Easiest, Moderate Cost & Effort, Higher Cost or More Complex
    val notes: String = "",
    val assignedCook: String = "",
    val isFavorite: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MasterMealItem(
    @DocumentId val id: String = "",
    val hubId: String = "",
    val memberUids: List<String> = emptyList(),
    val name: String = "",
    val costAndEffortTier: String = "Cheapest & Easiest",
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class HouseholdNote(
    @DocumentId val id: String = "",
    val hubId: String = "",
    val memberUids: List<String> = emptyList(),
    val title: String = "",
    val content: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val authorEmail: String = "",
    val category: String = "General", // General, Important, Reminder, Fun, Urgent
    val isPinned: Boolean = false,
    val colorTag: String = "#00F5FF",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CatalogGroceryItem(
    val name: String,
    val category: String,
    val source: String // receipt or general
)

data class TabletDisplayConfig(
    val showClock: Boolean = true,
    val showDinner: Boolean = true,
    val showCalendar: Boolean = true,
    val showChores: Boolean = true,
    val showGroceries: Boolean = true,
    val showNotes: Boolean = true,
    val cardOrder: List<String> = listOf("dinner", "calendar", "chores", "groceries", "notes")
)

data class CustomThemeConfig(
    val isCustomEnabled: Boolean = false,
    val activePaletteName: String = "Neon Galaxy",
    val primaryColorHex: String = "#00F5FF",
    val secondaryColorHex: String = "#FF007F",
    val tertiaryColorHex: String = "#B026FF",
    val backgroundColorHex: String = "#0B001A",
    val surfaceColorHex: String = "#120826",
    val textScale: Float = 1.0f,
    val cornerRadiusDp: Int = 16,
    val glowLevel: String = "High",
    val showStars: Boolean = true,
    val isDeviceOnly: Boolean = false
)
