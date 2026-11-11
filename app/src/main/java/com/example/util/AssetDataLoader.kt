package com.example.util

import android.content.Context
import com.example.data.models.CatalogGroceryItem
import com.example.data.models.MasterMealItem
import org.json.JSONObject

object AssetDataLoader {

    private var cachedGroceryCatalog: List<CatalogGroceryItem>? = null
    private var cachedMasterMeals: List<MasterMealItem>? = null

    fun loadGroceryCatalog(context: Context): List<CatalogGroceryItem> {
        cachedGroceryCatalog?.let { return it }

        return try {
            val jsonString = context.assets.open("Cosmic_Hub_Grocery_Catalog_318_07b7d5b8.json")
                .bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val itemsArray = root.getJSONArray("items")
            val result = ArrayList<CatalogGroceryItem>(itemsArray.length())
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.getJSONObject(i)
                result.add(
                    CatalogGroceryItem(
                        name = obj.getString("name"),
                        category = obj.getString("category"),
                        source = obj.optString("source", "general")
                    )
                )
            }
            cachedGroceryCatalog = result
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadMasterMeals(context: Context): List<MasterMealItem> {
        cachedMasterMeals?.let { return it }

        return try {
            val jsonString = context.assets.open("Family_Hub_Master_Meals_List_b868b837.json")
                .bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)
            val mealsArray = root.getJSONArray("meals")
            val result = ArrayList<MasterMealItem>(mealsArray.length())
            for (i in 0 until mealsArray.length()) {
                val obj = mealsArray.getJSONObject(i)
                result.add(
                    MasterMealItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        costAndEffortTier = obj.getString("costAndEffortTier"),
                        isCustom = false,
                        isFavorite = false
                    )
                )
            }
            cachedMasterMeals = result
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}
