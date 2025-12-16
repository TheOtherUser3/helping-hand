package com.example.helpinghand.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.helpinghand.data.model.HouseholdMember
import com.example.helpinghand.ui.theme.ShoppingColors as C

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    hasLightSensor: Boolean,
    isDynamicTheme: Boolean,
    onDynamicThemeChange: (Boolean) -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    navController: NavHostController,
    currentUserName: String,
    currentUserEmail: String,

    // NEW
    householdId: String?,
    onJoinHousehold: (String) -> Unit,
    onLeaveHousehold: () -> Unit,

    householdMembers: List<HouseholdMember>,
    onAddHouseholdMember: (String) -> Unit,
    onLogout: () -> Unit
) {
    val clipboard: ClipboardManager = LocalClipboardManager.current

    var showProfileDialog by remember { mutableStateOf(false) }
    var showHouseholdDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // NEW dialogs
    var showHouseholdCodeDialog by remember { mutableStateOf(false) }
    var showJoinHouseholdDialog by remember { mutableStateOf(false) }
    var showLeaveHouseholdDialog by remember { mutableStateOf(false) }

    Scaffold(containerColor = C.Background) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .testTag("settings_screen")
        ) {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("settings_back")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = C.OnBackground
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Replace person icon with help button
                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(C.Primary)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Help,
                                contentDescription = "Help",
                                tint = C.Surface
                            )
                        }
                        Text(
                            text = "Settings",
                            fontSize = 20.sp,
                            color = C.OnBackground,
                            modifier = Modifier.testTag("settings_title")
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* already in settings */ }) {
                        Icon(Icons.Filled.Settings, null, tint = C.OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.Background)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = C.SurfaceVariant
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        Text(
                            text = "Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.Primary,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        )
                    }

                    item {
                        SettingsRow(
                            title = currentUserName,
                            subtitle = currentUserEmail,
                            icon = Icons.Filled.AccountCircle,
                            onClick = { showProfileDialog = true }
                        )
                        Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                    }

                    item {
                        Text(
                            text = "Household",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.Primary,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        )
                    }

                    item {
                        SettingsRow(
                            title = "Household Members",
                            subtitle = "${householdMembers.size} member(s)",
                            icon = Icons.Filled.Group,
                            onClick = { showHouseholdDialog = true }
                        )
                        Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                    }

                    // NEW: Household code
                    item {
                        SettingsRow(
                            title = "Household Code",
                            subtitle = householdId ?: "Loading…",
                            icon = Icons.Filled.Group,
                            onClick = { showHouseholdCodeDialog = true }
                        )
                        Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                    }

                    // NEW: Join household by code
                    item {
                        SettingsRow(
                            title = "Join Household",
                            subtitle = "Paste a code to join someone else’s household",
                            icon = Icons.Filled.Group,
                            onClick = { showJoinHouseholdDialog = true }
                        )
                        Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                    }

                    // NEW: Leave household
                    item {
                        SettingsRow(
                            title = "Leave Household",
                            subtitle = "Create a new solo household for yourself",
                            icon = Icons.Filled.ExitToApp,
                            onClick = { showLeaveHouseholdDialog = true }
                        )
                        Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                    }

                    item {
                        Text(
                            text = "Appearance",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.Primary,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        )
                    }

                    if (hasLightSensor) {
                        item {
                            SettingsToggleRow(
                                title = "Dynamic Theme",
                                subtitle = "Auto light / dark based on ambient light",
                                isChecked = isDynamicTheme,
                                onCheckedChange = onDynamicThemeChange,
                                switchModifier = Modifier.testTag("switch_dynamic_theme")
                            )
                            Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                        }
                    }

                    item {
                        SettingsToggleRow(
                            title = "Dark Mode",
                            subtitle = if (isDynamicTheme) "Disabled while Dynamic Theme is on" else null,
                            isChecked = isDarkMode,
                            onCheckedChange = { if (!isDynamicTheme) onDarkModeChange(it) },
                            enabled = !isDynamicTheme,
                            switchModifier = Modifier.testTag("switch_dark_mode")
                        )
                        Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                    }

                    item {
                        Text(
                            text = "Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.Primary,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        )
                    }

                    item {
                        SettingsRow(
                            title = "Logout",
                            subtitle = "Sign out of your account",
                            icon = Icons.Filled.ExitToApp,
                            onClick = onLogout
                        )
                    }
                }
            }
        }

        if (showProfileDialog) {
            ProfileDialog(
                name = currentUserName,
                email = currentUserEmail,
                onDismiss = { showProfileDialog = false }
            )
        }

        if (showHouseholdDialog) {
            HouseholdDialog(
                members = householdMembers,
                onDismiss = { showHouseholdDialog = false },
                onAddMember = { showAddMemberDialog = true }
            )
        }

        // NEW: Household Code dialog
        if (showHouseholdCodeDialog) {
            AlertDialog(
                onDismissRequest = { showHouseholdCodeDialog = false },
                title = { Text("Household Code", color = C.OnBackground) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Share this code with someone so they can join your household:",
                            fontSize = 14.sp,
                            color = C.OnSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = householdId ?: "Loading…",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = C.OnBackground,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val id = householdId
                                    if (!id.isNullOrBlank()) {
                                        clipboard.setText(AnnotatedString(id))
                                    }
                                },
                                enabled = !householdId.isNullOrBlank()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = if (!householdId.isNullOrBlank()) C.Primary else C.OnSurfaceVariant
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHouseholdCodeDialog = false }) {
                        Text("Close", color = C.Primary)
                    }
                },
                containerColor = C.Surface
            )
        }

        // NEW: Join Household dialog
        if (showJoinHouseholdDialog) {
            var code by remember { mutableStateOf(TextFieldValue("")) }

            AlertDialog(
                onDismissRequest = { showJoinHouseholdDialog = false },
                title = { Text("Join Household", color = C.OnBackground) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Paste the household code you received.",
                            fontSize = 14.sp,
                            color = C.OnSurfaceVariant
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Household Code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onJoinHousehold(code.text)
                            showJoinHouseholdDialog = false
                        },
                        enabled = code.text.trim().isNotEmpty()
                    ) {
                        Text("Join", color = C.Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinHouseholdDialog = false }) {
                        Text("Cancel", color = C.OnSurfaceVariant)
                    }
                },
                containerColor = C.Surface
            )
        }

        // NEW: Leave Household confirm dialog
        if (showLeaveHouseholdDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveHouseholdDialog = false },
                title = { Text("Leave Household", color = C.OnBackground) },
                text = {
                    Text(
                        text = "This will remove you from the current household and create a new solo household for your account.",
                        fontSize = 14.sp,
                        color = C.OnSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onLeaveHousehold()
                            showLeaveHouseholdDialog = false
                        }
                    ) {
                        Text("Leave", color = C.Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveHouseholdDialog = false }) {
                        Text("Cancel", color = C.OnSurfaceVariant)
                    }
                },
                containerColor = C.Surface
            )
        }
    }
    if (showHelpDialog) {
        OnboardingDialog(onDismiss = { showHelpDialog = false })}
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = C.Primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = C.OnBackground,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = C.OnSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Open",
            tint = C.OnSurfaceVariant
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String?,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    switchModifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = if (enabled) C.OnBackground else C.OnSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = C.OnSurfaceVariant
                )
            }
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = switchModifier,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = C.Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = C.OnSurfaceVariant.copy(alpha = 0.38f)
            )
        )
    }
}

@Composable
private fun ProfileDialog(
    name: String,
    email: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile", color = C.OnBackground) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(C.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = C.Surface,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.OnBackground
                    )
                    Text(
                        text = email,
                        fontSize = 14.sp,
                        color = C.OnSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = C.Primary)
            }
        },
        containerColor = C.Surface
    )
}

@Composable
private fun HouseholdDialog(
    members: List<HouseholdMember>,
    onDismiss: () -> Unit,
    onAddMember: () -> Unit  // Keep parameter for compatibility but won't be used
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Household Members", color = C.OnBackground) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                members.forEach { member ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(C.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.displayName.firstOrNull()?.toString()
                                    ?: member.email.firstOrNull()?.toString()
                                    ?: "?",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = member.displayName.ifBlank { member.email },
                                fontSize = 16.sp,
                                color = C.OnBackground
                            )
                            Text(
                                text = member.email,
                                fontSize = 12.sp,
                                color = C.OnSurfaceVariant
                            )
                        }
                    }
                    Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                }

                // Button removed - members can only be added via household code now
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = C.Primary)
            }
        },
        containerColor = C.Surface
    )
}

@Composable
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var email by remember { mutableStateOf(TextFieldValue("")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Household Member", color = C.OnBackground) },
        text = {
            Column {
                Text(
                    text = "Enter the email address of the person you want to add to your household.",
                    fontSize = 14.sp,
                    color = C.OnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(email.text) },
                enabled = email.text.isNotEmpty()
            ) {
                Text("Add", color = C.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = C.OnSurfaceVariant)
            }
        },
        containerColor = C.Surface
    )
}
