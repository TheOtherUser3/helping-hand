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
import com.example.helpinghand.ui.viewmodel.ShoppingCartViewModel
import com.example.helpinghand.viewmodel.MealsViewModel


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val shoppingCartViewModel: ShoppingCartViewModel = viewModel()
    val mealsViewModel: MealsViewModel = viewModel()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen(navController) }
        composable("shopping") { ShoppingCartScreen(navController, shoppingCartViewModel, mealsViewModel) }
        composable("meals") { MealsScreen(navController, mealsViewModel) }
        composable("cleaning") { Text("Cleaning Screen Coming Soon") }
        composable("bills") { Text("Bills Screen Coming Soon") }
        composable("appointments") { Text("Appointments Screen Coming Soon") }
        composable("contacts") { Text("Contacts Screen Coming Soon") }
    }
}
