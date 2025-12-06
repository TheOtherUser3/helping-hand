package com.example.helpinghand.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.helpinghand.ui.screens.DashboardScreen
import com.example.helpinghand.ui.screens.ShoppingCartScreen
import com.example.helpinghand.ui.screens.MealsScreen
import com.example.helpinghand.ui.screens.SettingsScreen
import com.example.helpinghand.ui.screens.ContactsScreen
import com.example.helpinghand.ui.screens.CleaningReminderScreen
import com.example.helpinghand.ui.viewmodel.ShoppingCartViewModel
import com.example.helpinghand.viewmodel.MealsViewModel
import com.example.helpinghand.ui.screens.DoctorAppointmentsScreen


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val shoppingCartViewModel: ShoppingCartViewModel = viewModel()
    val mealsViewModel: MealsViewModel = viewModel()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen(navController) }
        composable("shopping") { ShoppingCartScreen(navController, shoppingCartViewModel, mealsViewModel) }
        composable("meals") { MealsScreen(navController, mealsViewModel) }
        composable("bills") { Text("Bills Screen Coming Soon") }
        composable("appointments") {
            DoctorAppointmentsScreen(
                navController = navController
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack()}
            )
        }
        composable("contacts") {
            ContactsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("cleaning") {
            CleaningReminderScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
