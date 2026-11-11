package com.example.ui.groceries

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CatalogGroceryItem
import com.example.data.models.GroceryItem
import com.example.data.models.Member
import com.example.ui.components.NeonGlowCard
import com.example.ui.components.VaporwaveHeaderBadge

val DefaultGroceryCategories = listOf(
    "All", "Produce", "Dairy & Eggs", "Meat & Protein", "Frozen & Quick Meals",
    "Pantry & Canned", "Beverages", "Breakfast & Snacks", "Baking & Seasonings",
    "Household & Personal", "Pet Supplies"
)

val StoreOptions = listOf("All Stores", "Kroger", "Amazon", "Other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrocerySection(
    groceries: List<GroceryItem>,
    catalog: List<CatalogGroceryItem>,
    members: List<Member>,
    onToggleGrocery: (GroceryItem) -> Unit,
    onDeleteGrocery: (String) -> Unit,
    onAddGrocery: (title: String, category: String, quantity: Long, unit: String, assignedTo: String, notes: String, store: String, source: String, isHistorical: Boolean) -> Unit,
    isDuplicateCheck: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var selectedStore by remember { mutableStateOf("All Stores") }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCatalogSheet by remember { mutableStateOf(false) }
    var pendingDuplicateItem by remember { mutableStateOf<CatalogGroceryItem?>(null) }
    var pendingDuplicateManualTitle by remember { mutableStateOf<String?>(null) }

    // Filtered shopping list
    val filteredGroceries = remember(groceries, selectedStore, selectedCategory, searchQuery) {
        groceries.filter { item ->
            val matchStore = selectedStore == "All Stores" || item.store.equals(selectedStore, ignoreCase = true)
            val matchCat = selectedCategory == "All" || item.category.equals(selectedCategory, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true)
            matchStore && matchCat && matchQuery
        }
    }

    val pendingCount = groceries.count { !it.isCompleted }
    val completedCount = groceries.count { it.isCompleted }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Stats Row & Catalog Trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VaporwaveHeaderBadge(
                        text = "$pendingCount NEEDED",
                        glowColor = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    VaporwaveHeaderBadge(
                        text = "$completedCount BOUGHT",
                        glowColor = MaterialTheme.colorScheme.secondary
                    )
                }

                Button(
                    onClick = { showCatalogSheet = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("open_catalog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Catalog (318)", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search your shopping list...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("grocery_search_input")
            )

            // Store Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(StoreOptions) { store ->
                    val selected = store == selectedStore
                    FilterChip(
                        selected = selected,
                        onClick = { selectedStore = store },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (store != "All Stores") {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(store)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (store) {
                                "Kroger" -> Color(0xFF00E5FF).copy(alpha = 0.25f)
                                "Amazon" -> Color(0xFFFF9900).copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            },
                            selectedLabelColor = when (store) {
                                "Kroger" -> Color(0xFF00E5FF)
                                "Amazon" -> Color(0xFFFF9900)
                                else -> MaterialTheme.colorScheme.primary
                            },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("filter_store_${store.lowercase().replace(" ", "_")}")
                    )
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(DefaultGroceryCategories) { cat ->
                    val selected = cat == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                            selectedLabelColor = MaterialTheme.colorScheme.secondary,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("filter_cat_${cat.lowercase().replace(" ", "_")}")
                    )
                }
            }

            // Grocery Items List
            if (filteredGroceries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (groceries.isEmpty()) "Your household shopping list is clear" else "No matching grocery items found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Catalog (318)' to quick-add past purchases or '+' for custom",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredGroceries, key = { it.id }) { item ->
                        GroceryItemCard(
                            item = item,
                            onToggle = { onToggleGrocery(item) },
                            onDelete = { onDeleteGrocery(item.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        // Floating Action Button to Add Manual Item
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_grocery_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add custom grocery item")
        }
    }

    // Modal BottomSheet for 318 Item Catalog (Tabs: Past Purchases & All Items)
    if (showCatalogSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCatalogSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CatalogBrowserSheet(
                catalog = catalog,
                onAddCatalogItem = { item, targetStore ->
                    if (isDuplicateCheck(item.name)) {
                        pendingDuplicateItem = item
                    } else {
                        onAddGrocery(
                            item.name,
                            item.category,
                            1L,
                            "pcs",
                            "",
                            "",
                            targetStore,
                            item.source,
                            item.source == "receipt"
                        )
                    }
                },
                onClose = { showCatalogSheet = false }
            )
        }
    }

    // Duplicate Item Confirmation Dialog
    if (pendingDuplicateItem != null) {
        val dupItem = pendingDuplicateItem!!
        AlertDialog(
            onDismissRequest = { pendingDuplicateItem = null },
            title = { Text("Item Already On List") },
            text = { Text("'${dupItem.name}' is already currently on your grocery list. Would you like to add another anyway?") },
            confirmButton = {
                Button(
                    onClick = {
                        onAddGrocery(
                            dupItem.name,
                            dupItem.category,
                            1L,
                            "pcs",
                            "",
                            "",
                            "Kroger",
                            dupItem.source,
                            dupItem.source == "receipt"
                        )
                        pendingDuplicateItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Add Duplicate")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDuplicateItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manual Add Grocery Item Dialog
    if (showAddDialog) {
        AddGroceryDialog(
            members = members,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, category, quantity, unit, assignedTo, notes, store ->
                if (isDuplicateCheck(title)) {
                    pendingDuplicateManualTitle = title
                } else {
                    onAddGrocery(title, category, quantity, unit, assignedTo, notes, store, "manual", false)
                    showAddDialog = false
                }
            }
        )
    }

    if (pendingDuplicateManualTitle != null) {
        val title = pendingDuplicateManualTitle!!
        AlertDialog(
            onDismissRequest = { pendingDuplicateManualTitle = null },
            title = { Text("Duplicate Warning") },
            text = { Text("'$title' is already in your active grocery list. Add another entry anyway?") },
            confirmButton = {
                Button(
                    onClick = {
                        onAddGrocery(title, "Produce", 1L, "pcs", "", "", "Kroger", "manual", false)
                        pendingDuplicateManualTitle = null
                        showAddDialog = false
                    }
                ) {
                    Text("Yes, Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDuplicateManualTitle = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GroceryItemCard(
    item: GroceryItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = item.isCompleted

    NeonGlowCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("grocery_item_${item.id}"),
        borderColor = if (isDone) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
        containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox Icon
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isDone) "Mark pending" else "Mark bought",
                    tint = if (isDone) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.quantity > 1L || item.unit != "pcs") {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${item.quantity} ${item.unit}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Store badge
                    val storeColor = when (item.store) {
                        "Amazon" -> Color(0xFFFF9900)
                        "Kroger" -> Color(0xFF00E5FF)
                        else -> MaterialTheme.colorScheme.secondary
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(storeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.store.ifBlank { "Kroger" },
                            style = MaterialTheme.typography.labelSmall,
                            color = storeColor
                        )
                    }

                    // Category badge
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Receipt history indicator
                    if (item.isHistorical || item.source == "receipt") {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historical Receipt Purchase",
                            tint = Color(0xFFFF007F).copy(alpha = 0.8f),
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    if (item.assignedTo.isNotBlank()) {
                        Text(
                            text = "• ${item.assignedTo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }

                if (item.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Delete Action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete item",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CatalogBrowserSheet(
    catalog: List<CatalogGroceryItem>,
    onAddCatalogItem: (CatalogGroceryItem, String) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Past Purchases (158), 1: All Items (318)
    var catalogSearch by remember { mutableStateOf("") }
    var selectedCatalogCategory by remember { mutableStateOf("All") }
    var defaultTargetStore by remember { mutableStateOf("Kroger") }
    var addedFeedbackItem by remember { mutableStateOf<String?>(null) }

    val pastPurchases = remember(catalog) { catalog.filter { it.source == "receipt" } }

    val baseList = if (selectedTab == 0) pastPurchases else catalog

    val filteredList = remember(baseList, catalogSearch, selectedCatalogCategory) {
        baseList.filter { item ->
            val matchCat = selectedCatalogCategory == "All" || item.category.equals(selectedCatalogCategory, ignoreCase = true)
            val matchQuery = catalogSearch.isBlank() || item.name.contains(catalogSearch, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(550.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Cosmic Grocery Catalog",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "One-tap quick add from 318 household items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        // Tabs: Past Purchases (158) vs All Items (318)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("My Past Purchases (${pastPurchases.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("All Items (${catalog.size})") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Target Store Selection for Quick-Add
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Add to store:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Kroger", "Amazon", "Other").forEach { store ->
                    val isSelected = defaultTargetStore == store
                    FilterChip(
                        selected = isSelected,
                        onClick = { defaultTargetStore = store },
                        label = { Text(store, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Catalog Search Bar
        OutlinedTextField(
            value = catalogSearch,
            onValueChange = { catalogSearch = it },
            placeholder = { Text("Search catalog items...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (catalogSearch.isNotBlank()) {
                    IconButton(onClick = { catalogSearch = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(DefaultGroceryCategories) { cat ->
                val selected = cat == selectedCatalogCategory
                FilterChip(
                    selected = selected,
                    onClick = { selectedCatalogCategory = cat },
                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                        selectedLabelColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Catalog Items List
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredList) { item ->
                val wasJustAdded = addedFeedbackItem == item.name

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.source == "receipt") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Past Receipt",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF007F)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onAddCatalogItem(item, defaultTargetStore)
                            addedFeedbackItem = item.name
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (wasJustAdded) Color(0xFF00FFB2) else MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFF04101A)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("catalog_add_${item.name.lowercase().take(10).replace(" ", "_")}")
                    ) {
                        if (wasJustAdded) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Added", style = MaterialTheme.typography.labelSmall)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroceryDialog(
    members: List<Member>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, category: String, quantity: Long, unit: String, assignedTo: String, notes: String, store: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Produce") }
    var selectedStore by remember { mutableStateOf("Kroger") }
    var quantity by remember { mutableLongStateOf(1L) }
    var unit by remember { mutableStateOf("pcs") }
    var assignedTo by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var storeExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }

    val unitOptions = listOf("pcs", "pack", "bag", "box", "lbs", "oz", "gal", "bunch", "can", "bottle")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Grocery") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Item Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_grocery_title_input")
                )

                // Store Selection
                ExposedDropdownMenuBox(
                    expanded = storeExpanded,
                    onExpandedChange = { storeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedStore,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Store") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = storeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = storeExpanded,
                        onDismissRequest = { storeExpanded = false }
                    ) {
                        listOf("Kroger", "Amazon", "Other").forEach { store ->
                            DropdownMenuItem(
                                text = { Text(store) },
                                onClick = {
                                    selectedStore = store
                                    storeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DefaultGroceryCategories.filter { it != "All" }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quantity and Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedButton(
                            onClick = { if (quantity > 1L) quantity-- },
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "$quantity",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        OutlinedButton(
                            onClick = { quantity++ },
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold)
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            unitOptions.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = assignedTo,
                    onValueChange = { assignedTo = it },
                    label = { Text("Assigned Family Member (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Brand (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, selectedCategory, quantity, unit, assignedTo, notes, selectedStore)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_grocery_button")
            ) {
                Text("Add to List")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
