package com.example.helpinghand.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.helpinghand.ui.theme.ShoppingColors as C
import com.example.helpinghand.viewmodel.DashboardViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel
) {
    val itemCount by viewModel.itemCount.collectAsState()
    val nextDueReminder by viewModel.nextDueReminder.collectAsState()
    val daysUntilNextDue by viewModel.daysUntilNextDue.collectAsState()

    var showHelpDialog by remember { mutableStateOf(false) }


    val cleaningStatus = daysUntilNextDue?.let { days ->
        when {
            days < 0 -> "Overdue!"
            days == 0 -> "Due Today!"
            else -> "$days days"
        }
    } ?: "No reminders"

    Scaffold(
        modifier = Modifier.testTag("dashboard_screen"),
        containerColor = C.Background
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // --- Top App Bar ---
            TopAppBar(
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
                            text = "Home",
                            fontSize = 20.sp,
                            color = C.OnBackground,
                            modifier = Modifier.testTag("dashboard_title")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("settings") },
                        modifier = Modifier.testTag("dashboard_settings_icon")
                    ) {
                        Icon(Icons.Filled.Settings, null, tint = C.OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.Background)
            )

            // --- Main Content ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(C.SurfaceVariant)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Welcome Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = C.Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Welcome Home",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = C.OnBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "What would you like to manage today?",
                            fontSize = 13.sp,
                            color = C.OnSurfaceVariant
                        )
                    }
                }

                // Quick Actions Grid
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardCard(
                            title = "Shopping",
                            subtitle = "$itemCount items",
                            icon = Icons.Filled.ShoppingCart,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("tile_shopping"),
                            onClick = { navController.navigate("shopping") }
                        )

                        DashboardCard(
                            title = "Cleaning",
                            subtitle = cleaningStatus,
                            icon = Icons.Filled.CleaningServices,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("tile_cleaning"),
                            onClick = { navController.navigate("cleaning") }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DashboardCard(
                            title = "Appointments",
                            subtitle = "Manage visits",
                            icon = Icons.Filled.CalendarToday,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { navController.navigate("appointments") }
                        )

                        DashboardCard(
                            title = "Contacts",
                            subtitle = "Manage contacts",
                            icon = Icons.Filled.Contacts,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("tile_contacts"),
                            onClick = { navController.navigate("contacts") }
                        )
                    }
                }

                // Quick Stats Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = C.Primary.copy(alpha = 0.1f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        QuickStat(
                            value = "$itemCount",
                            label = "Shopping Items"
                        )
                        VerticalDivider(
                            modifier = Modifier.height(36.dp),
                            color = C.OnSurfaceVariant.copy(alpha = 0.3f)
                        )
                        QuickStat(
                            value = cleaningStatus,
                            label = "Next Cleaning"
                        )
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        OnboardingDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
private fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = C.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(C.Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = C.Primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.OnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 15.sp,
                    color = C.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickStat(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = C.Primary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = C.OnSurfaceVariant
        )
    }
}