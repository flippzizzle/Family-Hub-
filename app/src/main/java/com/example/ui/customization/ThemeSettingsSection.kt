package com.example.ui.customization

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.CustomThemeConfig
import com.example.data.models.Hub
import com.example.ui.components.NeonGlowCard
import com.example.ui.components.VaporwaveHeaderBadge
import com.example.ui.theme.PRESET_THEMES
import com.example.ui.theme.PresetThemeDefinition
import com.example.ui.theme.getThemeDefinitionByName
import com.example.ui.theme.parseColorHex
import org.json.JSONObject

val ThemeCategories = listOf("All", "Cosmic", "Vaporwave", "Cyberpunk", "Retro", "Pastel")

@Composable
fun ThemeSettingsSection(
    hub: Hub,
    customThemeConfig: CustomThemeConfig,
    onUpdateCustomTheme: (CustomThemeConfig) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var activeConfig by remember(customThemeConfig) { mutableStateOf(customThemeConfig) }
    var showImportDialog by remember { mutableStateOf(false) }

    val filteredPresets = remember(selectedCategory) {
        if (selectedCategory == "All") PRESET_THEMES
        else PRESET_THEMES.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Title: Theme Studio
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Cosmic Theme Studio (35 Presets)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Active: ${activeConfig.activePaletteName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Export JSON
                    IconButton(
                        onClick = {
                            val json = exportThemeConfigToJson(activeConfig)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Cosmic Theme", json))
                            Toast.makeText(context, "Theme JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Export Theme JSON",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Import JSON
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Import Theme JSON",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Live Preview Card
        item {
            val preset = remember(activeConfig.activePaletteName) { getThemeDefinitionByName(activeConfig.activePaletteName) }
            val primary = parseColorHex(activeConfig.primaryColorHex, preset.primary)
            val secondary = parseColorHex(activeConfig.secondaryColorHex, preset.secondary)
            val surface = parseColorHex(activeConfig.surfaceColorHex, preset.surface)

            NeonGlowCard(
                borderColor = primary,
                containerColor = surface,
                shape = RoundedCornerShape(activeConfig.cornerRadiusDp.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE THEME PREVIEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = primary
                        )
                        VaporwaveHeaderBadge(text = activeConfig.glowLevel + " Glow", glowColor = secondary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeConfig.activePaletteName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Vaporwave galaxy aesthetic running with ${activeConfig.cornerRadiusDp}dp corners",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Personal Device vs Household Default Scope
        item {
            NeonGlowCard(
                borderColor = if (activeConfig.isDeviceOnly) Color(0xFF00FFB2) else MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val updated = activeConfig.copy(isDeviceOnly = !activeConfig.isDeviceOnly)
                            activeConfig = updated
                            onUpdateCustomTheme(updated)
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = if (activeConfig.isDeviceOnly) Color(0xFF00FFB2) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeConfig.isDeviceOnly) "Device-Only Theme Preference" else "Set as Household Default Theme",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (activeConfig.isDeviceOnly)
                                "Theme applies only to this device without changing other family members"
                            else
                                "Theme syncs to all family members connected to this hub",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = activeConfig.isDeviceOnly,
                        onCheckedChange = {
                            val updated = activeConfig.copy(isDeviceOnly = it)
                            activeConfig = updated
                            onUpdateCustomTheme(updated)
                        }
                    )
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ThemeCategories) { cat ->
                    val selected = cat == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // 35 Theme Presets List
        items(filteredPresets) { preset ->
            val isSelected = activeConfig.activePaletteName.equals(preset.name, ignoreCase = true)
            NeonGlowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val updated = activeConfig.copy(
                            activePaletteName = preset.name,
                            primaryColorHex = String.format("#%06X", 0xFFFFFF and preset.primary.value.toInt()),
                            secondaryColorHex = String.format("#%06X", 0xFFFFFF and preset.secondary.value.toInt()),
                            tertiaryColorHex = String.format("#%06X", 0xFFFFFF and preset.tertiary.value.toInt()),
                            backgroundColorHex = String.format("#%06X", 0xFFFFFF and preset.background.value.toInt()),
                            surfaceColorHex = String.format("#%06X", 0xFFFFFF and preset.surface.value.toInt())
                        )
                        activeConfig = updated
                        onUpdateCustomTheme(updated)
                    },
                borderColor = if (isSelected) preset.primary else Color.Transparent,
                borderWidth = if (isSelected) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = preset.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = preset.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = preset.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Color Preview Circles
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(preset.primary, preset.secondary, preset.tertiary, preset.background).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                        }
                    }
                }
            }
        }

        // Sliders & Custom Adjustments
        item {
            NeonGlowCard(
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Geometry & Typography Fine-Tuning",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Corner Radius
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Card Corner Radius", style = MaterialTheme.typography.bodyMedium)
                        Text("${activeConfig.cornerRadiusDp} dp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = activeConfig.cornerRadiusDp.toFloat(),
                        onValueChange = {
                            val updated = activeConfig.copy(cornerRadiusDp = it.toInt())
                            activeConfig = updated
                            onUpdateCustomTheme(updated)
                        },
                        valueRange = 4f..28f,
                        steps = 5,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Text Scale
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Text Scaling", style = MaterialTheme.typography.bodyMedium)
                        Text(String.format("%.2fx", activeConfig.textScale), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = activeConfig.textScale,
                        onValueChange = {
                            val updated = activeConfig.copy(textScale = it)
                            activeConfig = updated
                            onUpdateCustomTheme(updated)
                        },
                        valueRange = 0.85f..1.25f,
                        steps = 4,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.secondary)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Starfield switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Twinkling Ambient Stars & Grid", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = activeConfig.showStars,
                            onCheckedChange = {
                                val updated = activeConfig.copy(showStars = it)
                                activeConfig = updated
                                onUpdateCustomTheme(updated)
                            }
                        )
                    }
                }
            }
        }

        // Sign Out Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of Family Hub", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showImportDialog) {
        ThemeImportDialog(
            onDismiss = { showImportDialog = false },
            onImport = { importedConfig ->
                activeConfig = importedConfig
                onUpdateCustomTheme(importedConfig)
                showImportDialog = false
                Toast.makeText(context, "Theme '${importedConfig.activePaletteName}' applied!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

private fun exportThemeConfigToJson(config: CustomThemeConfig): String {
    val json = JSONObject()
    json.put("paletteName", config.activePaletteName)
    json.put("primaryColorHex", config.primaryColorHex)
    json.put("secondaryColorHex", config.secondaryColorHex)
    json.put("tertiaryColorHex", config.tertiaryColorHex)
    json.put("backgroundColorHex", config.backgroundColorHex)
    json.put("surfaceColorHex", config.surfaceColorHex)
    json.put("textScale", config.textScale.toDouble())
    json.put("cornerRadiusDp", config.cornerRadiusDp)
    json.put("glowLevel", config.glowLevel)
    json.put("showStars", config.showStars)
    json.put("isDeviceOnly", config.isDeviceOnly)
    return json.toString(2)
}

@Composable
fun ThemeImportDialog(
    onDismiss: () -> Unit,
    onImport: (CustomThemeConfig) -> Unit
) {
    var jsonText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Theme JSON") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste exported Cosmic Hub theme JSON here:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = {
                        jsonText = it
                        errorMsg = null
                    },
                    placeholder = { Text("{\n  \"paletteName\": \"Neon Galaxy\"...\n}") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val obj = JSONObject(jsonText)
                        val config = CustomThemeConfig(
                            isCustomEnabled = true,
                            activePaletteName = obj.optString("paletteName", "Custom Theme"),
                            primaryColorHex = obj.optString("primaryColorHex", "#00F5FF"),
                            secondaryColorHex = obj.optString("secondaryColorHex", "#FF007F"),
                            tertiaryColorHex = obj.optString("tertiaryColorHex", "#B026FF"),
                            backgroundColorHex = obj.optString("backgroundColorHex", "#080114"),
                            surfaceColorHex = obj.optString("surfaceColorHex", "#120826"),
                            textScale = obj.optDouble("textScale", 1.0).toFloat(),
                            cornerRadiusDp = obj.optInt("cornerRadiusDp", 16),
                            glowLevel = obj.optString("glowLevel", "High"),
                            showStars = obj.optBoolean("showStars", true),
                            isDeviceOnly = obj.optBoolean("isDeviceOnly", true)
                        )
                        onImport(config)
                    } catch (e: Exception) {
                        errorMsg = "Invalid Theme JSON: ${e.localizedMessage}"
                    }
                },
                enabled = jsonText.isNotBlank()
            ) {
                Text("Apply Theme")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
