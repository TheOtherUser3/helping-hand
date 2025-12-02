package com.example.helpinghand.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.helpinghand.data.model.CleaningReminder
import com.example.helpinghand.viewmodel.CleaningReminderViewModel
import com.example.helpinghand.ui.theme.ShoppingColors as C

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleaningReminderScreen(
    navController: NavHostController,
    viewModel: CleaningReminderViewModel
) {
    val reminderItems by viewModel.reminders.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newInterval by remember { mutableStateOf("") }

    Scaffold(containerColor = C.Background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("cleaning_screen")
        ) {

            // Top App Bar
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("cleaning_back")) {
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(C.Primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile",
                                tint = C.Surface
                            )
                        }
                        Text(
                            text = "Cleaning",
                            fontSize = 20.sp,
                            color = C.OnBackground,
                            modifier = Modifier.testTag("cleaning_title")
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") },
                        modifier = Modifier.testTag("cleaning_settings_icon")) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = C.OnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = C.Background
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = C.SurfaceVariant
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    // Centered "+" pill button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(140.dp)
                                .height(40.dp)
                                .shadow(6.dp, RoundedCornerShape(999.dp), clip = false)
                                .clickable { showDialog = true }
                                .testTag("cleaning_add_button"),
                            shape = RoundedCornerShape(999.dp),
                            color = C.SurfaceVariant,
                            tonalElevation = 4.dp,
                            border = BorderStroke(1.5.dp, C.Primary.copy(alpha = 0.6f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add cleaning task",
                                    tint = C.Primary
                                )
                            }
                        }
                    }

                    val todayEpochDay = remember { java.time.LocalDate.now().toEpochDay().toInt() }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("cleaning_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reminderItems, key = { it.id }) { item ->
                            val daysUntil =
                                (item.nextDueEpochDay - todayEpochDay).coerceAtLeast(0)
                            CleaningReminderCard(
                                item = item,
                                daysUntil = daysUntil,
                                onResetClick = { viewModel.resetCycle(item) },
                                onDeleteClick = { viewModel.deleteReminder(item) }
                            )
                        }
                    }
                }
            }
        }

        // Dialog for new reminder
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("New cleaning task", color = C.OnBackground) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { input ->
                                // 15-char limit on name
                                if (input.length <= 15) newName = input
                            },
                            label = { Text("Task name (max 15 chars)") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = newInterval,
                            onValueChange = { newInterval = it },
                            label = { Text("Days between cleanings") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val interval = newInterval.toIntOrNull()
                            if (interval != null && interval > 0 && newName.isNotBlank()) {
                                viewModel.addReminder(newName.trim(), interval)
                                newName = ""
                                newInterval = ""
                                showDialog = false
                            }
                        },
                        modifier = Modifier.testTag("cleaning_dialog_confirm")
                    ) {
                        Text("Add", color = C.Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false },
                        modifier = Modifier.testTag("cleaning_dialog_cancel")) {
                        Text("Cancel", color = C.OnSurfaceVariant)
                    }
                },
                containerColor = C.Surface
            )
        }
    }
}

@Composable
private fun CleaningReminderCard(
    item: CleaningReminder,
    daysUntil: Int,
    onResetClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            ),
        shape = RoundedCornerShape(24.dp),
        color = C.Surface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.5.dp, C.Primary.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: task name
            Text(
                text = item.name,
                fontSize = 16.sp,
                color = C.Primary,
                fontWeight = FontWeight.Medium
            )

            // Right: alarm + label + reset pill + delete icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Alarm + text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = "Next cleaning",
                        tint = C.OnBackground,
                        modifier = Modifier.size(18.dp)
                    )

                    val label = when {
                        daysUntil <= 0 -> "Due now"
                        daysUntil == 1 -> "Tomorrow!"
                        else -> "$daysUntil days"
                    }

                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = C.OnSurfaceVariant
                    )
                }

                // Reset pill
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = C.SurfaceVariant,
                    modifier = Modifier.clickable(onClick = onResetClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (daysUntil > 0) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "On schedule",
                                tint = C.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "",
                            fontSize = 14.sp,
                            color = C.Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Delete icon
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete reminder",
                        tint = C.OnSurfaceVariant
                    )
                }
            }
        }
    }
}
