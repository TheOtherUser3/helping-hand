package com.example.helpinghand.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
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


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(
    settingsRepository: SettingsRepository,
    hasLightSensor: Boolean
) {
    val navController = rememberNavController()
    // log each nav change
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            AppLogger.d(
                AppLogger.TAG_NAV,
                "Nav destination changed: route=${destination.route}, args=$arguments"
            )
        }
    }


    val app = LocalContext.current.applicationContext as HelpingHandApp
    val db = app.database

    // ViewModels like you already had...
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
    val contactsViewModel: ContactsViewModel = viewModel(
        factory = ContactsViewModelFactory(db.contactDao())
    )

    val darkMode by settingsRepository.darkModeEnabled.collectAsState(initial = false)
    val dynamicThemeEnabled by settingsRepository.dynamicThemeEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

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
                navController = navController,
                viewModel = contactsViewModel
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

