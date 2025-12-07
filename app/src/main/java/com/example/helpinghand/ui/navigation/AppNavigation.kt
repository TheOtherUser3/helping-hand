package com.example.helpinghand.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.helpinghand.AppLogger
import com.example.helpinghand.HelpingHandApp
import com.example.helpinghand.data.database.SettingsRepository
import com.example.helpinghand.data.household.HouseholdRepository
import com.example.helpinghand.data.model.HouseholdMember
import com.example.helpinghand.ui.screens.CleaningReminderScreen
import com.example.helpinghand.ui.screens.ContactsScreen
import com.example.helpinghand.ui.screens.DashboardScreen
import com.example.helpinghand.ui.screens.DoctorAppointmentsScreen
import com.example.helpinghand.ui.screens.LoginScreen
import com.example.helpinghand.ui.screens.MealsScreen
import com.example.helpinghand.ui.screens.OnboardingScreen
import com.example.helpinghand.ui.screens.RegistrationScreen
import com.example.helpinghand.ui.screens.SettingsScreen
import com.example.helpinghand.ui.screens.ShoppingCartScreen
import com.example.helpinghand.viewmodel.AuthViewModel
import com.example.helpinghand.viewmodel.AuthViewModelFactory
import com.example.helpinghand.viewmodel.CleaningReminderViewModel
import com.example.helpinghand.viewmodel.CleaningReminderViewModelFactory
import com.example.helpinghand.viewmodel.ContactsViewModel
import com.example.helpinghand.viewmodel.ContactsViewModelFactory
import com.example.helpinghand.viewmodel.DashboardViewModel
import com.example.helpinghand.viewmodel.DashboardViewModelFactory
import com.example.helpinghand.viewmodel.DoctorAppointmentsViewModel
import com.example.helpinghand.viewmodel.DoctorAppointmentsViewModelFactory
import com.example.helpinghand.viewmodel.MealsViewModel
import com.example.helpinghand.viewmodel.MealsViewModelFactory
import com.example.helpinghand.viewmodel.ShoppingCartViewModel
import com.example.helpinghand.viewmodel.ShoppingCartViewModelFactory
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(
    settingsRepository: SettingsRepository,
    hasLightSensor: Boolean
) {
    val navController = rememberNavController()

    // Log each destination change for debugging
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

    // Firebase auth viewmodel
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
    val authUiState by authViewModel.uiState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // App feature viewmodels
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

    // Household integration with Firebase
    val householdRepository = remember { HouseholdRepository() }
    var householdId by remember { mutableStateOf<String?>(null) }
    var householdMembers by remember { mutableStateOf<List<HouseholdMember>>(emptyList()) }

    // Settings (DataStore)
    val darkMode by settingsRepository.darkModeEnabled.collectAsState(initial = false)
    val dynamicThemeEnabled by settingsRepository.dynamicThemeEnabled.collectAsState(initial = false)
    val onboardingShown by settingsRepository.onboardingShown.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    // Navigation gate: redirect based on auth, but allow onboarding and ignore null route
    LaunchedEffect(currentUser, onboardingShown) {
        val route = navController.currentDestination?.route
        AppLogger.d(
            AppLogger.TAG_NAV,
            "Auth gate: currentUser=${currentUser?.uid}, route=$route, onboardingShown=$onboardingShown"
        )

        // If navController hasn't set a destination yet, don't redirect
        if (route == null) {
            AppLogger.d(AppLogger.TAG_NAV, "Auth gate: route is null, skipping redirect")
            return@LaunchedEffect
        }

        // While onboarding hasn't been completed and we're on onboarding, don't redirect
        if (!onboardingShown && route == "onboarding") {
            AppLogger.d(AppLogger.TAG_NAV, "Auth gate: onboarding active, skipping redirect")
            return@LaunchedEffect
        }

        if (currentUser == null &&
            route != "login" &&
            route != "register" &&
            route != "onboarding"
        ) {
            AppLogger.d(AppLogger.TAG_NAV, "User null, forcing navigation to login")
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        } else if (currentUser != null &&
            (route == "login" || route == "register")
        ) {
            AppLogger.d(AppLogger.TAG_NAV, "User logged in, navigating to dashboard")
            navController.navigate("dashboard") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // When the auth user changes, set up their user document and household
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            AppLogger.d(AppLogger.TAG_VM, "Auth user changed -> ensure user and household")
            try {
                householdRepository.ensureUserDocument()
                val id = householdRepository.getOrCreateHouseholdId()
                AppLogger.d(AppLogger.TAG_VM, "Resolved householdId=$id")
                householdId = id
            } catch (e: Exception) {
                AppLogger.e(
                    AppLogger.TAG_VM,
                    "Error initializing household: ${e.message}",
                    e
                )
                householdId = null
            }
        } else {
            AppLogger.d(AppLogger.TAG_VM, "No auth user, clearing household state")
            householdId = null
            householdMembers = emptyList()
        }
    }

    // Observe members whenever the householdId changes
    LaunchedEffect(householdId) {
        val id = householdId
        if (id != null) {
            AppLogger.d(AppLogger.TAG_VM, "Starting member observation for householdId=$id")
            householdRepository
                .observeHouseholdMembers(id)
                .collect { members ->
                    AppLogger.d(
                        AppLogger.TAG_VM,
                        "Household members updated: count=${members.size}"
                    )
                    householdMembers = members
                }
        } else {
            householdMembers = emptyList()
        }
    }

    NavHost(
        navController = navController,
        startDestination = when {
            !onboardingShown -> "onboarding"
            currentUser == null -> "login"
            else -> "dashboard"
        }
    ) {
        // ONBOARDING

        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    scope.launch {
                        settingsRepository.setOnboardingShown()
                    }
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // AUTH SCREENS

        composable("login") {
            LoginScreen(
                uiState = authUiState,
                onLogin = { email, password ->
                    authViewModel.login(email, password)
                },
                navController = navController
            )
        }

        composable("register") {
            RegistrationScreen(
                uiState = authUiState,
                onRegister = { name, email, password ->
                    authViewModel.register(name, email, password)
                },
                navController = navController
            )
        }

        // MAIN APP SCREENS

        composable("dashboard") {
            DashboardScreen(
                navController = navController,
                viewModel = dashboardViewModel
            )
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

        composable("bills") {
            Text("Bills Screen Coming Soon")
        }

        composable("appointments") {
            DoctorAppointmentsScreen(
                navController = navController,
                viewModel = doctorAppointmentsViewModel
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

        composable("settings") {
            val user = currentUser
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
                navController = navController,
                currentUserName = user?.displayName ?: "Unknown user",
                currentUserEmail = user?.email ?: "Unknown email",
                householdMembers = householdMembers,
                onAddHouseholdMember = { email ->
                    val id = householdId
                    if (id != null) {
                        scope.launch {
                            val success = householdRepository.addMemberByEmail(id, email)
                            if (!success) {
                                AppLogger.e(
                                    AppLogger.TAG_VM,
                                    "Failed to add member with email=$email"
                                )
                            }
                        }
                    } else {
                        AppLogger.e(
                            AppLogger.TAG_VM,
                            "Cannot add member, householdId is null"
                        )
                    }
                },
                onLogout = {
                    authViewModel.logout()
                }
            )
        }
    }
}
