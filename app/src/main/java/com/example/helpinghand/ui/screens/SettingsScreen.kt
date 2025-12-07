package com.example.helpinghand.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
    householdMembers: List<HouseholdMember>,
    onAddHouseholdMember: (String) -> Unit,
    onLogout: () -> Unit
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var showHouseholdDialog by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    Scaffold(containerColor = C.Background) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .testTag("settings_screen")
        ) {
            // Top App Bar
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(C.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, null, tint = C.Surface)
                        }
                        Text("Settings", fontSize = 20.sp, color = C.OnBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { /* already in settings */ }) {
                        Icon(Icons.Filled.Settings, null, tint = C.OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.Background)
            )

            // Main Content
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
                    // Profile section
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

                    // Household section
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

                    // Appearance section
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
                                onCheckedChange = onDynamicThemeChange
                            )
                            Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                        }
                    }

                    item {
                        SettingsToggleRow(
                            title = "Dark Mode",
                            subtitle = if (isDynamicTheme)
                                "Disabled while Dynamic Theme is on"
                            else null,
                            isChecked = isDarkMode,
                            onCheckedChange = { if (!isDynamicTheme) onDarkModeChange(it) },
                            enabled = !isDynamicTheme
                        )
                        Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                    }

                    // Account section
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

        if (showAddMemberDialog) {
            AddMemberDialog(
                onDismiss = { showAddMemberDialog = false },
                onAdd = { email ->
                    onAddHouseholdMember(email)
                    showAddMemberDialog = false
                }
            )
        }
    }
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
    enabled: Boolean = true
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
    onAddMember: () -> Unit
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

                Button(
                    onClick = onAddMember,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = C.Primary)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Member")
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
