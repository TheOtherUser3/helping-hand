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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helpinghand.ui.theme.AppColors

data class ReminderItem(
    val id: Int,
    val name: String,
    val daysAgo: String,
    var isCompleted: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleaningReminderScreen(
    onNavigateBack: () -> Unit
) {
    var reminderItems by remember {
        mutableStateOf(
            listOf(
                ReminderItem(1, "Item 1", "XXX Days Ago"),
                ReminderItem(2, "Item 2", "XXX Days Ago"),
                ReminderItem(3, "Item 3", "XXX Days Ago"),
                ReminderItem(4, "Item 4", "XXX Days Ago"),
                ReminderItem(5, "Item 5", "XXX Days Ago"),
                ReminderItem(6, "Item 6", "XXX Days Ago")
            )
        )
    }

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
                            text = "Cleaning",
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
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = AppColors.OnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )

            // Add Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = AppColors.SurfaceVariant,
                    modifier = Modifier.clickable { /* Add new item */ }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = AppColors.OnBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Reminder Items List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reminderItems) { item ->
                    ReminderCard(
                        item = item,
                        onResetClick = {
                            reminderItems = reminderItems.map {
                                if (it.id == item.id) it.copy(isCompleted = !it.isCompleted) else it
                            }
                        }
                    )
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
private fun ReminderCard(
    item: ReminderItem,
    onResetClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = AppColors.Surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.name,
                fontSize = 16.sp,
                color = AppColors.OnBackground,
                fontWeight = FontWeight.Medium
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = "Alarm",
                    tint = AppColors.OnBackground,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = item.daysAgo,
                    fontSize = 14.sp,
                    color = AppColors.OnSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AppColors.SurfaceVariant,
                    modifier = Modifier.clickable(onClick = onResetClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Completed",
                                tint = AppColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Reset",
                            fontSize = 14.sp,
                            color = AppColors.Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}