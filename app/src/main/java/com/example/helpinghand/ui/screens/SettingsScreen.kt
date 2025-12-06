package com.example.helpinghand.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.helpinghand.ui.theme.ShoppingColors as C

// Data class for household members
data class HouseholdMember(
    val id: String,
    val name: String,
    val email: String,
    val role: String = "Member" // Owner, Admin, Member
)

// ========== UPDATED SETTINGS SCREEN ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    hasLightSensor: Boolean,
    isDynamicTheme: Boolean,
    onDynamicThemeChange: (Boolean) -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    navController: NavHostController,
    currentUserName: String = "John Doe",
    currentUserEmail: String = "john.doe@example.com",
    householdMembers: List<HouseholdMember> = emptyList(),
    onLogout: () -> Unit = {}
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
            // --- Top App Bar ---
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("settings_back")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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

            // --- Main Content ---
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
                    // Profile Section
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

                    // Household Section
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
                            subtitle = "${householdMembers.size} members",
                            icon = Icons.Filled.Group,
                            onClick = { showHouseholdDialog = true }
                        )
                        Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                    }

                    // Appearance Section
                    item {
                        Text(
                            text = "Appearance",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.Primary,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                        )
                    }

                    // Dynamic Theme (if sensor exists)
                    if (hasLightSensor) {
                        item {
                            SettingsToggleRow(
                                title = "Dynamic Theme",
                                subtitle = "Auto light/dark based on ambient light",
                                isChecked = isDynamicTheme,
                                onCheckedChange = onDynamicThemeChange
                            )
                            Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                        }
                    }

                    // Manual Light/Dark Mode
                    item {
                        SettingsToggleRow(
                            title = "Dark Mode",
                            subtitle = if (isDynamicTheme) "Disabled while Dynamic Theme is on" else null,
                            isChecked = isDarkMode,
                            onCheckedChange = { if (!isDynamicTheme) onDarkModeChange(it) },
                            enabled = !isDynamicTheme
                        )
                        Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                    }

                    // Account Section
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

        // Profile Dialog
        if (showProfileDialog) {
            ProfileDialog(
                name = currentUserName,
                email = currentUserEmail,
                onDismiss = { showProfileDialog = false }
            )
        }

        // Household Dialog
        if (showHouseholdDialog) {
            HouseholdDialog(
                members = householdMembers,
                onDismiss = { showHouseholdDialog = false },
                onAddMember = { showAddMemberDialog = true }
            )
        }

        // Add Member Dialog
        if (showAddMemberDialog) {
            AddMemberDialog(
                onDismiss = { showAddMemberDialog = false },
                onAdd = { email ->
                    // Handle adding member via Firebase
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                    text = member.name.first().toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = member.name,
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
                        Text(
                            text = member.role,
                            fontSize = 12.sp,
                            color = C.Primary
                        )
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

// ========== LOGIN SCREEN ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLogin: (email: String, password: String) -> Unit
) {
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(containerColor = C.Background) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Logo/Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(C.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "App Logo",
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Text(
                    text = "Welcome Back",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.OnBackground
                )

                Text(
                    text = "Sign in to continue",
                    fontSize = 16.sp,
                    color = C.OnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility
                                else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password"
                                else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Login Button
                Button(
                    onClick = { onLogin(email.text, password.text) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = C.Primary)
                ) {
                    Text("Login", fontSize = 16.sp)
                }

                // Register Link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        color = C.OnSurfaceVariant
                    )
                    Text(
                        text = "Register",
                        color = C.Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToRegister() }
                    )
                }
            }
        }
    }
}

// ========== REGISTRATION SCREEN ==========
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegister: (name: String, email: String, password: String) -> Unit
) {
    var name by remember { mutableStateOf(TextFieldValue("")) }
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var password by remember { mutableStateOf(TextFieldValue("")) }
    var confirmPassword by remember { mutableStateOf(TextFieldValue("")) }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(containerColor = C.Background) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // App Logo/Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(C.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = "App Logo",
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }

                Text(
                    text = "Create Account",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = C.OnBackground
                )

                Text(
                    text = "Sign up to get started",
                    fontSize = 16.sp,
                    color = C.OnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility
                                else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password"
                                else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Register Button
                Button(
                    onClick = {
                        if (password.text == confirmPassword.text) {
                            onRegister(name.text, email.text, password.text)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = C.Primary),
                    enabled = name.text.isNotEmpty() &&
                            email.text.isNotEmpty() &&
                            password.text.isNotEmpty() &&
                            password.text == confirmPassword.text
                ) {
                    Text("Register", fontSize = 16.sp)
                }

                // Login Link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account? ",
                        color = C.OnSurfaceVariant
                    )
                    Text(
                        text = "Login",
                        color = C.Primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
            }
        }
    }
}