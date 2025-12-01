package com.example.helpinghand.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helpinghand.ui.theme.AppColors

data class Contact(
    val id: Int,
    val name: String,
    val phone: String,
    val email: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onNavigateBack: () -> Unit
) {
    val contacts = remember {
        listOf(
            Contact(1, "Alice Anderson", "(555) 123-4567", "alice@email.com"),
            Contact(2, "Amy Adams", "(555) 234-5678", "amy@email.com"),
            Contact(3, "Bob Brown", "(555) 345-6789", "bob@email.com"),
            Contact(4, "Barbara Bell", "(555) 456-7890", "barbara@email.com"),
            Contact(5, "Charlie Chen", "(555) 567-8901", "charlie@email.com"),
            Contact(6, "Christina Clark", "(555) 678-9012", "christina@email.com"),
            Contact(7, "David Davis", "(555) 789-0123", "david@email.com"),
            Contact(8, "Diana Diaz", "(555) 890-1234", "diana@email.com"),
            Contact(9, "Emily Evans", "(555) 901-2345", "emily@email.com"),
            Contact(10, "Frank Foster", "(555) 012-3456", "frank@email.com"),
            Contact(11, "George Garcia", "(555) 123-4568", "george@email.com"),
            Contact(12, "Hannah Hill", "(555) 234-5679", "hannah@email.com")
        ).sortedBy { it.name }
    }

    val groupedContacts = contacts.groupBy { it.name.first().uppercaseChar() }

    Scaffold(
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "9:30",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.OnBackground
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.SignalCellularAlt,
                        contentDescription = "Signal",
                        tint = AppColors.OnBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = "WiFi",
                        tint = AppColors.OnBackground,
                        modifier = Modifier.size(16.dp)
                    )
                    Icon(
                        imageVector = Icons.Filled.BatteryFull,
                        contentDescription = "Battery",
                        tint = AppColors.OnBackground,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Top App Bar
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AppColors.Primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            text = "Contacts",
                            fontSize = 22.sp,
                            color = AppColors.OnBackground
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.OnBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Add contact */ }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = AppColors.OnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )

            // Contacts List with Sticky Headers
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                groupedContacts.forEach { (letter, contactsForLetter) ->
                    stickyHeader {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = AppColors.Background
                        ) {
                            Text(
                                text = letter.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Primary,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                            )
                        }
                    }

                    items(contactsForLetter) { contact ->
                        ContactCard(contact)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Gesture Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(AppColors.GestureBar),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .background(
                            AppColors.OnBackground,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun ContactCard(contact: Contact) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppColors.Surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                shape = CircleShape,
                color = AppColors.Primary,
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = contact.name.first().toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Contact Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contact.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.OnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.phone,
                    fontSize = 14.sp,
                    color = AppColors.OnSurfaceVariant
                )
                if (contact.email.isNotEmpty()) {
                    Text(
                        text = contact.email,
                        fontSize = 12.sp,
                        color = AppColors.OnSurfaceVariant
                    )
                }
            }

            // Call button
            IconButton(onClick = { /* Make call */ }) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = "Call",
                    tint = AppColors.Primary
                )
            }
        }
    }
}
