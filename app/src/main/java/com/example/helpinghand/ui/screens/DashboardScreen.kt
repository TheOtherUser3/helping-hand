package com.example.helpinghand.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.helpinghand.R
import com.example.helpinghand.ui.theme.DashboardColors
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


    val cleaningStatus = daysUntilNextDue?.let { days ->
        when {
            days < 0 -> "Overdue!"
            days == 0 -> "Due Today!"
            else -> "$days days until next due"
        }
    } ?: "No reminders"

    Scaffold(
        containerColor = DashboardColors.Dashboard,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DashboardColors.Dashboard)
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = "Home",
                                tint = DashboardColors.Label,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp)
                            )
                            Text(
                                text = "Home",
                                fontSize = 22.sp,
                                color = DashboardColors.Headline
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = DashboardColors.Icon
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DashboardColors.AppBar
                    )
                )
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(50.dp, Alignment.CenterVertically)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                DashboardTilePng(
                    resId = R.drawable.ic_shopping,
                    count = itemCount,
                    extraText = null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    borderColor = DashboardColors.Label
                ) { navController.navigate("shopping") }

                DashboardTilePng(
                    resId = R.drawable.ic_cleaning,
                    count = null,
                    extraText = cleaningStatus,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    borderColor = DashboardColors.Label
                ) { navController.navigate("cleaning") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                DashboardTilePng(
                    resId = R.drawable.ic_bills,
                    count = null,
                    extraText = null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    borderColor = DashboardColors.Label
                ) { navController.navigate("bills") }

                DashboardTilePng(
                    resId = R.drawable.ic_appointments,
                    count = null,
                    extraText = null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    borderColor = DashboardColors.Label
                ) { navController.navigate("appointments") }
            }

            FullWidthContactsTile(
                text = "Contacts",
                shape = RoundedCornerShape(18.dp),
                borderColor = DashboardColors.Label
            ) { navController.navigate("contacts") }
        }
    }
}


@Composable
private fun DashboardTilePng(
    resId: Int,
    count: Int? = null,
    extraText: String? = null,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    borderColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .shadow(8.dp, shape = shape, clip = false),
        shape = shape,
        border = BorderStroke(2.dp, borderColor),
        color = DashboardColors.CardBackground,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(resId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.6f),
                contentScale = ContentScale.Fit
            )

            if (count != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "$count item${if (count != 1) "s" else ""} in cart",
                    fontSize = 14.sp,
                    color = DashboardColors.Icon
                )
            }

            if (!extraText.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = extraText,
                    fontSize = 14.sp,
                    color = DashboardColors.Headline
                )
            }
        }
    }
}


@Composable
private fun FullWidthContactsTile(
    text: String,
    shape: RoundedCornerShape,
    borderColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .shadow(8.dp, shape = shape, clip = false),
        shape = shape,
        border = BorderStroke(2.dp, borderColor),
        color = DashboardColors.CardBackground,
        tonalElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = DashboardColors.Headline
            )
        }
    }
}
