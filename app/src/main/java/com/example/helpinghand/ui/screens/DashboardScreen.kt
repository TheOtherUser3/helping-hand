package com.example.helpinghand.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.helpinghand.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Home", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { /* maybe open drawer later */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Profile"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* edit order */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { /* sort priority */ }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First row (Shopping + Cleaning)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardCard(
                    title = "Shopping",
                    iconRes = R.drawable.ic_shopping,
                    onClick = { navController.navigate("shopping") }
                )
                DashboardCard(
                    title = "Cleaning",
                    iconRes = R.drawable.ic_cleaning,
                    onClick = { navController.navigate("cleaning") }
                )
            }

            // Second row (Bills + Appointments)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardCard(
                    title = "Bills",
                    iconRes = R.drawable.ic_bills,
                    onClick = { navController.navigate("bills") }
                )
                DashboardCard(
                    title = "Appointments",
                    iconRes = R.drawable.ic_appointments,
                    onClick = { navController.navigate("appointments") }
                )
            }

            // Contacts (full width)
            DashboardCard(
                title = "Contacts",
                iconRes = R.drawable.ic_contacts,
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate("contacts") }
            )
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(64.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
            )
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold)
        }
    }
}
