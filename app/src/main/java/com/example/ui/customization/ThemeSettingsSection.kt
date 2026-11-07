package com.example.ui.customization

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.models.Hub
import com.example.ui.components.NeonGlowCard

data class PaletteOption(
    val name: String,
    val description: String,
    val previewColors: List<Color>
)

val AvailablePalettes = listOf(
    PaletteOption(
        name = "Neon Galaxy",
        description = "Laser Cyan, Electric Magenta, and Deep Void Nebula",
        previewColors = listOf(Color(0xFF00F5FF), Color(0xFFFF007F), Color(0xFFB026FF))
    ),
    PaletteOption(
        name = "Cyber Magenta",
        description = "Synthwave Hot Pink, Laser Blue, and Sunset Glow",
        previewColors = listOf(Color(0xFFFF007F), Color(0xFF00F5FF), Color(0xFFFF7A00))
    ),
    PaletteOption(
        name = "Retro Cyan",
        description = "Tron Turquoise, Electric Mint, and Cyber Coral",
        previewColors = listOf(Color(0xFF00F5FF), Color(0xFF00FFB2), Color(0xFFFF4D6D))
    ),
    PaletteOption(
        name = "Sunset Horizon",
        description = "1984 Sunset Orange, Neon Magenta, and Starlight Gold",
        previewColors = listOf(Color(0xFFFF7A00), Color(0xFFFF007F), Color(0xFFFFE600))
    )
)

@Composable
fun ThemeSettingsSection(
    hub: Hub,
    onUpdateTheme: (palette: String, glow: String, stars: Boolean) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPalette by remember(hub.themePalette) { mutableStateOf(hub.themePalette) }
    var selectedGlow by remember(hub.glowIntensity) { mutableStateOf(hub.glowIntensity) }
    var ambientStarsEnabled by remember(hub.ambientStars) { mutableStateOf(hub.ambientStars) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vaporwave Galaxy Palettes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        items(AvailablePalettes.size) { index ->
            val option = AvailablePalettes[index]
            val isSelected = selectedPalette == option.name
            NeonGlowCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedPalette = option.name
                        onUpdateTheme(option.name, selectedGlow, ambientStarsEnabled)
                    }
                    .testTag("palette_option_${option.name.lowercase().replace(' ', '_')}"),
                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                borderWidth = if (isSelected) 2.dp else 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = option.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Color Preview circles
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        option.previewColors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(c)
                            )
                        }
                    }
                }
            }
        }

        // Section: Visual Effects & Ambiance
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Galaxy Effects & Ambiance",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item {
            NeonGlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Starfield toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Animated Starfield & Grid",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Twinkling celestial stars and retro synthwave horizon grid",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = ambientStarsEnabled,
                            onCheckedChange = {
                                ambientStarsEnabled = it
                                onUpdateTheme(selectedPalette, selectedGlow, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.testTag("starfield_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Glow Intensity
                    Text(
                        text = "Glow Intensity",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("High", "Medium", "Subtle").forEach { level ->
                            val isSelected = selectedGlow == level
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedGlow = level
                                        onUpdateTheme(selectedPalette, level, ambientStarsEnabled)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("glow_level_$level")
                            ) {
                                Text(
                                    text = level,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Sign Out
        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of Family Hub", style = MaterialTheme.typography.labelLarge)
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
