package com.example.helpinghand.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Text
import com.example.helpinghand.HelpingHandApp
import com.example.helpinghand.ui.screens.DashboardScreen
import com.example.helpinghand.ui.screens.ShoppingCartScreen
import com.example.helpinghand.ui.screens.MealsScreen
import com.example.helpinghand.ui.screens.SettingsScreen
import com.example.helpinghand.ui.screens.ContactsScreen
import com.example.helpinghand.ui.screens.CleaningReminderScreen
import com.example.helpinghand.viewmodel.DashboardViewModel
import com.example.helpinghand.viewmodel.DashboardViewModelFactory
import com.example.helpinghand.viewmodel.ShoppingCartViewModel
import com.example.helpinghand.viewmodel.ShoppingCartViewModelFactory
import com.example.helpinghand.viewmodel.MealsViewModel
import com.example.helpinghand.viewmodel.MealsViewModelFactory
import com.example.helpinghand.viewmodel.CleaningReminderViewModel
import com.example.helpinghand.viewmodel.CleaningReminderViewModelFactory

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Get database
    val app = LocalContext.current.applicationContext as HelpingHandApp
    val db = app.database

    // ViewModels wired to DAOs via factories
    val shoppingCartViewModel: ShoppingCartViewModel = viewModel(
        factory = ShoppingCartViewModelFactory(db.shoppingItemDao())
    )

    val mealsViewModel: MealsViewModel = viewModel(
        factory = MealsViewModelFactory(db.shoppingItemDao())
    )

    val cleaningReminderViewModel: CleaningReminderViewModel = viewModel(
        factory = CleaningReminderViewModelFactory(db.cleaningReminderDao())
    )

    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(db.shoppingItemDao(), db.cleaningReminderDao())
    )


    NavHost(navController = navController, startDestination = "dashboard") {

        composable("dashboard") {
            DashboardScreen(navController, dashboardViewModel)
        }

        composable("shopping") {
            ShoppingCartScreen(
                navController = navController,
                viewModel = shoppingCartViewModel,
                mealsViewModel = mealsViewModel
            )
        }

        composable("meals") {
            MealsScreen(
                navController = navController,
                viewModel = mealsViewModel
            )
        }

        composable("bills") { Text("Bills Screen Coming Soon") }

        composable("appointments") { Text("Appointments Screen Coming Soon") }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("contacts") {
            ContactsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("cleaning") {
            CleaningReminderScreen(
                navController = navController,
                viewModel = cleaningReminderViewModel
            )
        }
    }
}
