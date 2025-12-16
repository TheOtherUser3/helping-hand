package com.example.helpinghand.ui.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.helpinghand.AppLogger
import com.example.helpinghand.HelpingHandApp
import com.example.helpinghand.data.database.SettingsRepository
import com.example.helpinghand.data.household.HouseholdRepository
import com.example.helpinghand.data.model.HouseholdMember
import com.example.helpinghand.ui.screens.*
import com.example.helpinghand.viewmodel.*
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(
    settingsRepository: SettingsRepository,
    hasLightSensor: Boolean
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // App + DB
    val app = context.applicationContext as HelpingHandApp
    val db = app.database

    // --------------------
    // Auth
    // --------------------
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
    val authUiState by authViewModel.uiState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // --------------------
    // Settings (DataStore)
    // --------------------
    val darkMode by settingsRepository.darkModeEnabled.collectAsState(initial = false)
    val dynamicThemeEnabled by settingsRepository.dynamicThemeEnabled.collectAsState(initial = false)

    // CRITICAL FIX: nullable onboarding state
    val onboardingShown by settingsRepository.onboardingShown.collectAsState(initial = null)

    // --------------------
    // Feature ViewModels
    // --------------------
    val shoppingCartViewModel: ShoppingCartViewModel =
        viewModel(factory = ShoppingCartViewModelFactory(db.shoppingItemDao()))

    val mealsViewModel: MealsViewModel =
        viewModel(factory = MealsViewModelFactory(db.shoppingItemDao()))

    val cleaningReminderViewModel: CleaningReminderViewModel =
        viewModel(factory = CleaningReminderViewModelFactory(db.cleaningReminderDao()))

    val dashboardViewModel: DashboardViewModel =
        viewModel(factory = DashboardViewModelFactory(
            db.shoppingItemDao(),
            db.cleaningReminderDao()
        ))

    val contactsViewModel: ContactsViewModel =
        viewModel(factory = ContactsViewModelFactory(db.contactDao()))

    val doctorAppointmentsViewModel: DoctorAppointmentsViewModel =
        viewModel(factory = DoctorAppointmentsViewModelFactory(db.doctorAppointmentDao()))

    // --------------------
    // Household (Firebase)
    // --------------------
    val householdRepository = remember { HouseholdRepository() }
    var householdId by remember { mutableStateOf<String?>(null) }
    var householdMembers by remember { mutableStateOf<List<HouseholdMember>>(emptyList()) }

    // Initialize household when auth changes
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            try {
                householdRepository.ensureUserDocument()
                householdId = householdRepository.getOrCreateHouseholdId()
            } catch (e: Exception) {
                AppLogger.e(AppLogger.TAG_VM, "Household init failed", e)
                householdId = null
            }
        } else {
            householdId = null
            householdMembers = emptyList()
        }
    }

    // Observe household members
    LaunchedEffect(householdId) {
        cleaningReminderViewModel.onHouseholdIdChanged(householdId)
        doctorAppointmentsViewModel.onHouseholdIdChanged(householdId)
        contactsViewModel.onHouseholdIdChanged(householdId)
        shoppingCartViewModel.onHouseholdIdChanged(householdId)

        val id = householdId ?: return@LaunchedEffect
        householdRepository.observeHouseholdMembers(id).collect { members ->
            householdMembers = members
        }
    }

    // --------------------
    // Notifications permission gate
    // --------------------
    val notifPrefs = remember(context) {
        context.getSharedPreferences("helping_hand_prefs", Context.MODE_PRIVATE)
    }
    val KEY_NOTIF_PROMPT_SHOWN = "notif_permission_prompt_shown"

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notifPrefs.edit().putBoolean(KEY_NOTIF_PROMPT_SHOWN, true).apply()
            AppLogger.d(AppLogger.TAG_VM, "POST_NOTIFICATIONS granted=$granted")
        }

    LaunchedEffect(currentUser?.uid, onboardingShown) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            onboardingShown == true &&
            currentUser != null
        ) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            val granted = ContextCompat.checkSelfPermission(
                context,
                perm
            ) == PackageManager.PERMISSION_GRANTED

            val alreadyPrompted =
                notifPrefs.getBoolean(KEY_NOTIF_PROMPT_SHOWN, false)

            if (!granted && !alreadyPrompted) {
                notifPrefs.edit().putBoolean(KEY_NOTIF_PROMPT_SHOWN, true).apply()
                notificationPermissionLauncher.launch(perm)
            }
        }
    }

    // --------------------
    // HARD GATE: do not create NavHost until onboarding state is known
    // --------------------
    if (onboardingShown == null) {
        Box(Modifier.fillMaxSize())
        return
    }

    // --------------------
    // Navigation
    // --------------------
    NavHost(
        navController = navController,
        startDestination = when {
            onboardingShown == false -> "onboarding"
            currentUser == null -> "login"
            else -> "dashboard"
        }
    ) {

        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    scope.launch { settingsRepository.setOnboardingShown() }
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                uiState = authUiState,
                onLogin = authViewModel::login,
                navController = navController
            )
        }

        composable("register") {
            RegistrationScreen(
                uiState = authUiState,
                onRegister = authViewModel::register,
                navController = navController
            )
        }

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
                viewModel = cleaningReminderViewModel,
                householdMembers = householdMembers,
                currentUserUid = currentUser?.uid
            )
        }

        composable("settings") {
            SettingsScreen(
                hasLightSensor = hasLightSensor,
                isDynamicTheme = dynamicThemeEnabled,
                onDynamicThemeChange = {
                    scope.launch { settingsRepository.setDynamicTheme(it) }
                },
                isDarkMode = darkMode,
                onDarkModeChange = {
                    scope.launch { settingsRepository.setDarkMode(it) }
                },
                navController = navController,
                currentUserName = currentUser?.displayName ?: "Unknown",
                currentUserEmail = currentUser?.email ?: "Unknown",
                householdId = householdId,
                householdMembers = householdMembers,
                onAddHouseholdMember = { email ->
                    householdId?.let { hid ->
                        scope.launch {
                            householdRepository.addMemberByEmail(hid, email)
                        }
                    }
                },
                onJoinHousehold = { code ->
                    scope.launch {
                        val success = householdRepository.joinHousehold(code.trim())
                        if (success) householdId = code.trim()
                    }
                },
                onLeaveHousehold = {
                    scope.launch {
                        householdId =
                            householdRepository.leaveAndCreateSoloHousehold()
                    }
                },
                onLogout = authViewModel::logout,
                onUpdateDisplayName = { newName ->
                    scope.launch {
                        val ok = authViewModel.updateDisplayName(newName)
                        if (ok) {
                            // Push Auth displayName -> Firestore users/{uid}.displayName
                            householdRepository.ensureUserDocument()
                        }
                    }
                })
        }
    }
}
