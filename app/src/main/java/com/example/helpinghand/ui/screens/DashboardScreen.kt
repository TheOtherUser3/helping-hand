package com.example.helpinghand.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.helpinghand.R
import com.example.helpinghand.ui.theme.DashboardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController) {
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
            // === grid rows ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                DashboardTilePng(
                    resId = R.drawable.ic_shopping,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    borderColor = DashboardColors.Label
                ) { navController.navigate("shopping") }

                DashboardTilePng(
                    resId = R.drawable.ic_cleaning,
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    borderColor = DashboardColors.Label
                ) { navController.navigate("bills") }

                DashboardTilePng(
                    resId = R.drawable.ic_appointments,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    borderColor = DashboardColors.Label
                ) { navController.navigate("appointments") }
            }

            // === full-width Contacts card ===
            FullWidthContactsTile(
                text = "Contacts",
                shape = RoundedCornerShape(18.dp),
                borderColor = DashboardColors.Label
            ) { navController.navigate("contacts") }
        }
    }
}

/** Single square PNG card with strong elevation + visible border */
@Composable
private fun DashboardTilePng(
    resId: Int,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    borderColor: androidx.compose.ui.graphics.Color,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(resId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize(0.75f),  // fills more of the box
                contentScale = ContentScale.Fit
            )
        }
    }
}

/** Contacts tile — text only, centered near top, strong elevation */
@Composable
private fun FullWidthContactsTile(
    text: String,
    shape: RoundedCornerShape,
    borderColor: androidx.compose.ui.graphics.Color,
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
