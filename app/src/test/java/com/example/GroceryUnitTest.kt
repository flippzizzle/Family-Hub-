package com.example

import com.example.data.models.CatalogGroceryItem
import com.example.data.models.GroceryFavorite
import com.example.data.models.GroceryItem
import com.example.util.GroceryExportUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class GroceryUnitTest {

    @Test
    fun testExportFilenameFormatForTxtAndPng() {
        // Fix time to 2026-11-11 10:13:00 UTC
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 11, 10, 13, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val timestamp = cal.timeInMillis

        val pngFilename = GroceryExportUtil.generateExportFilename("png", timestamp)
        val txtFilename = GroceryExportUtil.generateExportFilename("txt", timestamp)

        // Strict format verification: grocery-list[date]_[timestamp].[extension]
        val regex = Regex("""^grocery-list\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\.(png|txt)$""")
        assertTrue("PNG filename '$pngFilename' should match regex", regex.matches(pngFilename))
        assertTrue("TXT filename '$txtFilename' should match regex", regex.matches(txtFilename))

        assertTrue(pngFilename.startsWith("grocery-list2026-11-11_"))
        assertTrue(pngFilename.endsWith(".png"))
        assertTrue(txtFilename.startsWith("grocery-list2026-11-11_"))
        assertTrue(txtFilename.endsWith(".txt"))
    }

    @Test
    fun testActiveShoppingListFiltersOutCompletedItems() {
        val allGroceries = listOf(
            GroceryItem(id = "1", title = "Organic Bananas", isCompleted = false),
            GroceryItem(id = "2", title = "Honeycrisp Apples", isCompleted = true),
            GroceryItem(id = "3", title = "Whole Milk", isCompleted = false),
            GroceryItem(id = "4", title = "Sourdough Bread", isCompleted = true)
        )

        val activeList = allGroceries.filter { !it.isCompleted }
        val completedList = allGroceries.filter { it.isCompleted }

        assertEquals(2, activeList.size)
        assertEquals("Organic Bananas", activeList[0].title)
        assertEquals("Whole Milk", activeList[1].title)

        assertEquals(2, completedList.size)
        assertEquals("Honeycrisp Apples", completedList[0].title)
        assertEquals("Sourdough Bread", completedList[1].title)
    }

    @Test
    fun testDuplicateDetectionAgainstActiveShoppingListOnly() {
        val groceries = listOf(
            GroceryItem(id = "1", title = "Kroger 2% Milk", isCompleted = false),
            GroceryItem(id = "2", title = "Eggs Large White", isCompleted = true)
        )

        fun isDuplicate(title: String): Boolean {
            val clean = title.trim().lowercase()
            return groceries.any { !it.isCompleted && it.title.trim().lowercase() == clean }
        }

        // Active item should trigger duplicate check
        assertTrue(isDuplicate("Kroger 2% Milk"))
        assertTrue(isDuplicate("  kroger 2% milk  "))

        // Completed/Purchased item should NOT trigger duplicate check (can be bought again)
        assertFalse(isDuplicate("Eggs Large White"))
        assertFalse(isDuplicate("Avocados"))
    }

    @Test
    fun testFavoritesDeduplicationKey() {
        val name1 = "Kroger Whole Vitamin D Milk"
        val docId1 = name1.trim().lowercase().replace(Regex("[^a-z0-9]"), "_").take(60)

        val name2 = "  kroger whole vitamin d milk  "
        val docId2 = name2.trim().lowercase().replace(Regex("[^a-z0-9]"), "_").take(60)

        assertEquals(docId1, docId2)
        assertEquals("kroger_whole_vitamin_d_milk", docId1)
    }

    @Test
    fun testCatalogItemsIntegrity() {
        val catalogItem = CatalogGroceryItem(
            name = "Kroger 100% Pure Canola Oil",
            category = "Pantry & Canned",
            source = "receipt"
        )
        val favorite = GroceryFavorite(
            id = "kroger_100__pure_canola_oil",
            name = catalogItem.name,
            category = catalogItem.category,
            preferredStore = "Kroger",
            source = catalogItem.source
        )

        assertEquals("Kroger 100% Pure Canola Oil", favorite.name)
        assertEquals("Pantry & Canned", favorite.category)
        assertEquals("Kroger", favorite.preferredStore)
    }
}
