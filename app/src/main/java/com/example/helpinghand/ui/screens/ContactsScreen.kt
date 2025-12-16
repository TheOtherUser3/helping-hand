package com.example.helpinghand.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavHostController
import com.example.helpinghand.data.model.Contact
import com.example.helpinghand.data.model.formatPhoneNumber
import com.example.helpinghand.ui.theme.ShoppingColors as C
import com.example.helpinghand.viewmodel.ContactsViewModel

private const val MAX_NAME_CHARS = 60
private const val MAX_PHONE_CHARS = 20          // user input length cap (allows dashes/spaces)
private const val MAX_EMAIL_CHARS = 120

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactsScreen(
    navController: NavHostController,
    viewModel: ContactsViewModel
) {
    val contacts by viewModel.contacts.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var nameField by remember { mutableStateOf(TextFieldValue("")) }
    var phoneField by remember { mutableStateOf(TextFieldValue("")) }
    var emailField by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(containerColor = C.Background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("contacts_screen")
        ) {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("contacts_back")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Dashboard",
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
                            text = "Contacts",
                            fontSize = 20.sp,
                            color = C.OnBackground,
                            modifier = Modifier.testTag("dashboard_title")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("settings") },
                        modifier = Modifier.testTag("contacts_settings_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = C.OnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.Background)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = C.SurfaceVariant
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Add button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(160.dp)
                                .height(40.dp)
                                .shadow(6.dp, RoundedCornerShape(999.dp), clip = false)
                                .clickable { showDialog = true }
                                .testTag("contacts_add_button"),
                            shape = RoundedCornerShape(999.dp),
                            color = C.SurfaceVariant,
                            tonalElevation = 4.dp,
                            border = BorderStroke(1.5.dp, C.Primary.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add contact",
                                    tint = C.Primary
                                )
                            }
                        }
                    }

                    val grouped = contacts.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("contacts_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        grouped.toSortedMap().forEach { (letter, listForLetter) ->
                            stickyHeader {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(C.SurfaceVariant),
                                    color = Color.Transparent
                                ) {
                                    Text(
                                        text = letter.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = C.Primary,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }

                            items(listForLetter, key = { it.id }) { contact ->
                                ContactCard(
                                    contact = contact,
                                    onDelete = { viewModel.deleteContact(contact) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val name = nameField.text.trim()
                            val phone = phoneField.text.trim()
                            val email = emailField.text.trim()

                            if (name.isNotEmpty() && (phone.isNotEmpty() || email.isNotEmpty())) {
                                viewModel.addContact(name, phone, email)
                                nameField = TextFieldValue("")
                                phoneField = TextFieldValue("")
                                emailField = TextFieldValue("")
                                showDialog = false
                            }
                        },
                        modifier = Modifier.testTag("contacts_dialog_confirm")
                    ) {
                        Text("Add", color = C.Primary)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDialog = false },
                        modifier = Modifier.testTag("contacts_dialog_cancel")
                    ) {
                        Text("Cancel", color = C.OnSurfaceVariant)
                    }
                },
                title = { Text("Add Contact", color = C.OnBackground) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = nameField,
                            onValueChange = { new ->
                                if (new.text.length <= MAX_NAME_CHARS) nameField = new
                            },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = phoneField,
                            onValueChange = { new ->
                                val t = new.text
                                // accept digits and hyphen/spaces only; cap length
                                if (
                                    t.length <= MAX_PHONE_CHARS &&
                                    t.all { it.isDigit() || it == '-' || it == ' ' }
                                ) {
                                    phoneField = new
                                }
                            },
                            label = { Text("Phone (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = emailField,
                            onValueChange = { new ->
                                if (new.text.length <= MAX_EMAIL_CHARS) emailField = new
                            },
                            label = { Text("Email (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
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
private fun ContactCard(
    contact: Contact,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val digitsOnly = remember(contact.phone) { contact.phone.filter { it.isDigit() } }
    val formattedPhone = remember(contact.phone) { formatPhoneNumber(contact.phone) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), clip = false),
        shape = RoundedCornerShape(24.dp),
        color = C.Surface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.5.dp, C.Primary.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = C.Primary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = C.OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (digitsOnly.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedPhone, // always 666-666-6666 for 10 digits
                        fontSize = 14.sp,
                        color = C.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (contact.email.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = contact.email,
                        fontSize = 12.sp,
                        color = C.OnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        if (digitsOnly.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = "tel:$digitsOnly".toUri()
                            }
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = "Call",
                        tint = C.Primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete contact",
                        tint = C.OnSurfaceVariant
                    )
                }
            }
        }
    }
}
