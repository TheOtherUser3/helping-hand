package com.example.helpinghand.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.helpinghand.AppLogger
import com.example.helpinghand.HelpingHandApp
import com.example.helpinghand.data.database.SettingsRepository
import com.example.helpinghand.ui.screens.DashboardScreen
import com.example.helpinghand.ui.screens.ShoppingCartScreen
import com.example.helpinghand.ui.screens.MealsScreen
import com.example.helpinghand.ui.screens.SettingsScreen
import com.example.helpinghand.ui.screens.ContactsScreen
import com.example.helpinghand.ui.screens.CleaningReminderScreen
import com.example.helpinghand.viewmodel.ShoppingCartViewModel
import com.example.helpinghand.viewmodel.MealsViewModel
import com.example.helpinghand.ui.screens.DoctorAppointmentsScreen
import com.example.helpinghand.viewmodel.CleaningReminderViewModel
import com.example.helpinghand.viewmodel.CleaningReminderViewModelFactory
import com.example.helpinghand.viewmodel.ContactsViewModel
import com.example.helpinghand.viewmodel.ContactsViewModelFactory
import com.example.helpinghand.viewmodel.DashboardViewModel
import com.example.helpinghand.viewmodel.DashboardViewModelFactory
import com.example.helpinghand.viewmodel.DoctorAppointmentsViewModel
import com.example.helpinghand.viewmodel.DoctorAppointmentsViewModelFactory
import com.example.helpinghand.viewmodel.MealsViewModelFactory
import com.example.helpinghand.viewmodel.ShoppingCartViewModelFactory
import kotlinx.coroutines.launch


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
    val doctorAppointmentsViewModel: DoctorAppointmentsViewModel = viewModel(
        factory = DoctorAppointmentsViewModelFactory(db.doctorAppointmentDao())
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
                navController = navController,
                viewModel = doctorAppointmentsViewModel
            )
        }
        composable("settings") {
            SettingsScreen(
                hasLightSensor = hasLightSensor,
                isDynamicTheme = dynamicThemeEnabled,
                onDynamicThemeChange = { enabled ->
                    scope.launch {
                        settingsRepository.setDynamicTheme(enabled)
                    }
                },
                isDarkMode = darkMode,
                onDarkModeChange = { enabled ->
                    scope.launch {
                        settingsRepository.setDarkMode(enabled)
                    }
                },
                navController = navController
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

