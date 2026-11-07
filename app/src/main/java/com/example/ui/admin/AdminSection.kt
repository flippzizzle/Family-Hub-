package com.example.ui.admin

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Hub
import com.example.data.models.Member
import com.example.ui.components.NeonGlowCard
import com.example.ui.components.VaporwaveHeaderBadge

@Composable
fun AdminSection(
    hub: Hub,
    members: List<Member>,
    currentUserId: String,
    onWhitelistEmail: (String) -> Unit,
    onRemoveWhitelistedEmail: (String) -> Unit,
    onSetMemberRole: (String, String) -> Unit,
    onRemoveMember: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showInviteDialog by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<Member?>(null) }
    var whitelistFilter by remember { mutableStateOf("") }

    val isPrimaryUser = hub.ownerId == currentUserId
    val isCoAdmin = hub.adminUids.contains(currentUserId)
    val hasAdminRights = isPrimaryUser || isCoAdmin

    // Check which whitelisted emails currently have active connected devices
    val connectedEmails = remember(members) { members.map { it.email.lowercase() }.toSet() }

    val filteredWhitelist = remember(hub.whitelistedEmails, whitelistFilter) {
        if (whitelistFilter.isBlank()) hub.whitelistedEmails
        else hub.whitelistedEmails.filter { it.contains(whitelistFilter.trim(), ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Primary Admin Status Card
            NeonGlowCard(
                modifier = Modifier.fillMaxWidth().testTag("primary_admin_hero_card"),
                borderColor = if (isPrimaryUser) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f))
                                    .border(1.dp, MaterialTheme.colorScheme.tertiary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPrimaryUser) Icons.Default.Star else Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isPrimaryUser) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isPrimaryUser) "PRIMARY HUB FOUNDER" else if (isCoAdmin) "CO-ADMINISTRATOR" else "FAMILY MEMBER ACCESS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (isPrimaryUser) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = hub.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (hasAdminRights) {
                            IconButton(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "You've been invited to join the '${hub.name}' Family Hub! Download the app and sign in with your Google Account to sync grocery lists and appointments in real time."
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Family Hub Invite"))
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("share_hub_invite_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Invite",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isPrimaryUser)
                            "As primary administrator, you control family member invitations, device whitelisting, and live sync permissions across all household devices."
                        else if (isCoAdmin)
                            "You have co-admin privileges to invite new members and manage the household whitelist."
                        else
                            "Your device is connected and synchronized with this family hub.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VaporwaveHeaderBadge(
                            text = "${hub.whitelistedEmails.size} WHITELISTED",
                            glowColor = MaterialTheme.colorScheme.primary
                        )
                        VaporwaveHeaderBadge(
                            text = "${members.size} SYNCED DEVICES",
                            glowColor = Color(0xFF00FFB2)
                        )
                    }
                }
            }
        }

        // Action Row: Invite Member Button
        if (hasAdminRights) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Access Management",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = { showInviteDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.testTag("open_invite_member_dialog")
                    ) {
                        Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Invite Member", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Section: Connected Members with Device Telemetry
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Devices,
                    contentDescription = null,
                    tint = Color(0xFF00FFB2),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Connected Family Members (${members.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        items(members, key = { it.uid }) { member ->
            val isMemberOwner = member.uid == hub.ownerId
            val isCurrentUser = member.uid == currentUserId
            val canRemove = hasAdminRights && !isMemberOwner && !isCurrentUser

            MemberCardWithRemove(
                member = member,
                isMemberOwner = isMemberOwner,
                isCurrentUser = isCurrentUser,
                canManageRoles = isPrimaryUser && !isMemberOwner && !isCurrentUser,
                canRemove = canRemove,
                onPromote = { onSetMemberRole(member.uid, "admin") },
                onDemote = { onSetMemberRole(member.uid, "member") },
                onRequestRemove = { memberToRemove = member }
            )
        }

        // Section: Device Access Whitelist
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device Access Whitelist (${hub.whitelistedEmails.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (hub.whitelistedEmails.size > 4) {
            item {
                OutlinedTextField(
                    value = whitelistFilter,
                    onValueChange = { whitelistFilter = it },
                    placeholder = { Text("Filter whitelisted emails...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        if (filteredWhitelist.isEmpty()) {
            item {
                NeonGlowCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (whitelistFilter.isBlank()) "No emails whitelisted yet." else "No matches found.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        } else {
            items(filteredWhitelist) { email ->
                val isConnected = connectedEmails.contains(email.lowercase())
                WhitelistEntryCard(
                    email = email,
                    isConnected = isConnected,
                    canRevoke = hasAdminRights && email != members.firstOrNull { it.uid == hub.ownerId }?.email,
                    onRevoke = { onRemoveWhitelistedEmail(email) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Invite Member Dialog
    if (showInviteDialog) {
        InviteMemberDialog(
            onDismiss = { showInviteDialog = false },
            onInvite = { email ->
                onWhitelistEmail(email)
                showInviteDialog = false
            }
        )
    }

    // Remove Member Confirmation Dialog
    if (memberToRemove != null) {
        val member = memberToRemove!!
        var alsoRemoveFromWhitelist by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = {
                Text(
                    text = "Remove Member From Hub?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to disconnect ${member.displayName} (${member.email})?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Their device (${member.deviceInfo}) will immediately lose sync access to grocery lists and family calendar appointments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = alsoRemoveFromWhitelist,
                            onCheckedChange = { alsoRemoveFromWhitelist = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Also remove email from Whitelist",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = member.email
                        onRemoveMember(member.uid)
                        if (alsoRemoveFromWhitelist && email.isNotBlank()) {
                            onRemoveWhitelistedEmail(email)
                        }
                        memberToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_remove_member_button")
                ) {
                    Text("Remove Member")
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun MemberCardWithRemove(
    member: Member,
    isMemberOwner: Boolean,
    isCurrentUser: Boolean,
    canManageRoles: Boolean,
    canRemove: Boolean,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onRequestRemove: () -> Unit
) {
    NeonGlowCard(
        modifier = Modifier.fillMaxWidth().testTag("member_card_${member.uid}"),
        borderColor = if (isMemberOwner) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (isMemberOwner) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMemberOwner) Icons.Default.Star else if (member.role == "admin") Icons.Default.Shield else Icons.Default.Person,
                            contentDescription = null,
                            tint = if (isMemberOwner) MaterialTheme.colorScheme.tertiary else if (member.role == "admin") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.displayName.ifBlank { "Family Member" },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isCurrentUser) {
                                Text(
                                    text = " (You)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Text(
                            text = member.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isMemberOwner) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            else if (member.role == "admin") MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isMemberOwner) "PRIMARY" else member.role.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isMemberOwner) MaterialTheme.colorScheme.tertiary else if (member.role == "admin") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Device Telemetry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = Color(0xFF00FFB2),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = member.deviceInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Live Connected",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color(0xFF00FFB2)
                )
            }

            // Actions row: promote/demote and remove
            if (canManageRoles || canRemove) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canManageRoles) {
                        if (member.role == "member") {
                            TextButton(onClick = onPromote) {
                                Text("Make Co-Admin", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            TextButton(onClick = onDemote) {
                                Text("Set Member", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    if (canRemove) {
                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedButton(
                            onClick = onRequestRemove,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("remove_member_button_${member.uid}")
                        ) {
                            Icon(Icons.Default.PersonRemove, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WhitelistEntryCard(
    email: String,
    isConnected: Boolean,
    canRevoke: Boolean,
    onRevoke: () -> Unit
) {
    NeonGlowCard(
        modifier = Modifier.fillMaxWidth().testTag("whitelist_entry_$email"),
        borderColor = if (isConnected) Color(0xFF00FFB2).copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) Color(0xFF00FFB2) else MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isConnected) "Device Connected & Active" else "Pending Sign-in on Device",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = if (isConnected) Color(0xFF00FFB2) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (canRevoke) {
                IconButton(
                    onClick = onRevoke,
                    modifier = Modifier.size(32.dp).testTag("revoke_whitelist_$email")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Revoke access",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InviteMemberDialog(
    onDismiss: () -> Unit,
    onInvite: (email: String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Mail,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Invite Family Member",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter your family member's Google Account email. Once invited, their account is whitelisted so their device can instantly sync with this hub.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Family Member Google Email") },
                    placeholder = { Text("spouse@gmail.com, teen@gmail.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_email_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isNotBlank() && email.contains("@")) {
                        onInvite(email.trim().lowercase())
                    }
                },
                enabled = email.isNotBlank() && email.contains("@"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.testTag("confirm_invite_button")
            ) {
                Text("Send Invite & Whitelist")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )
}
