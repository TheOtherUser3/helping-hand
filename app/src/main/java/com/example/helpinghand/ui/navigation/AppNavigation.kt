package com.example.helpinghand.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.helpinghand.ui.screens.DashboardScreen
import com.example.helpinghand.ui.screens.feature.ShoppingAndMealsApp


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen(navController) }

        composable("shopping") { ShoppingAndMealsApp() }
        composable("cleaning") { Text("Cleaning Screen Coming Soon") }
        composable("bills") { Text("Bills Screen Coming Soon") }
        composable("appointments") { Text("Appointments Screen Coming Soon") }
        composable("contacts") { Text("Contacts Screen Coming Soon") }
    }
}
