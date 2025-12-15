package com.example.helpinghand.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.helpinghand.data.model.Meal
import com.example.helpinghand.ui.theme.ShoppingColors as C
import com.example.helpinghand.viewmodel.MealsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsScreen(
    navController: NavHostController,
    viewModel: MealsViewModel
) {
    val meals by viewModel.meals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }
    // Search
    var searchQuery by remember { mutableStateOf("") }

    val filteredMeals = remember(meals, searchQuery) {
        if (searchQuery.isBlank()) {
            meals
        } else {
            val query = searchQuery.trim()
            meals.filter { meal ->
                val titleMatch = meal.title.contains(query, ignoreCase = true)

                val usedMatch = meal.usedIngredients.any { ingredient ->
                    ingredient.name.contains(query, ignoreCase = true)
                }

                val missedMatch = meal.missedIngredients.any { ingredient ->
                    ingredient.name.contains(query, ignoreCase = true)
                }

                titleMatch || usedMatch || missedMatch
            }
        }
    }


    Scaffold(containerColor = C.Background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(C.Background)
                .testTag("meals_screen")
        ) {

            // Top bar ---------------------------------------------------------
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                            navController.popBackStack()
                        },
                        modifier = Modifier.testTag("meals_back")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to Dashboard",
                            tint = C.OnBackground
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Replace person icon with help button
                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(C.Primary)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Help,
                                contentDescription = "Help",
                                tint = C.Surface
                            )
                        }
                        Text(
                            text = "Shopping & Meals",
                            fontSize = 20.sp,
                            color = C.OnBackground,
                            modifier = Modifier.testTag("dashboard_title")
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") },
                        modifier = Modifier.testTag("meals_settings_icon")) {
                        Icon(Icons.Filled.Settings, null, tint = C.OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.Background)
            )

            // Meals header row ---------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(C.SurfaceVariant)
                    .height(56.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = "Back",
                        tint = C.OnBackground
                    )
                }
                Text(
                    text = "Meals",
                    color = C.Primary,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .testTag("meals_title")
                        .align(Alignment.Center)
                )
            }

            // Search bar --------------------------------------------------------
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search meals or ingredients...", color = C.OnSurfaceVariant)
                },
                trailingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = C.OnSurfaceVariant)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("meals_search"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = C.Surface,
                    focusedContainerColor = C.Surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = C.Primary
                ),
                singleLine = true
            )

            // Meal list ---------------------------------------------------------
            when {
                isLoading -> {
                    Box(Modifier
                        .fillMaxSize()
                        .testTag("meals_loading"),
                        contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = C.Primary)
                    }
                }

                filteredMeals.isEmpty() -> {
                    Box(Modifier
                        .fillMaxSize()
                        .testTag("meals_empty"), contentAlignment = Alignment.Center) {
                        Text("No meals found.", color = C.OnSurfaceVariant)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("meals_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredMeals) { meal ->
                            MealCard(
                                meal = meal,
                                onAddMissing = { viewModel.addMissingIngredients(meal) },
                                onOpenRecipe = { url ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    navController.context.startActivity(intent)
                                },
                                modifier = Modifier.testTag("meal_card_${meal.id}")
                            )
                        }
                    }
                }
            }
        }
    }
    if (showHelpDialog) {
        OnboardingDialog(onDismiss = { showHelpDialog = false })}
}


// ---------------------------------------------------------------------------
// MEAL CARD
// ---------------------------------------------------------------------------

@Composable
fun MealCard(
    meal: Meal,
    onAddMissing: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenRecipe(meal.recipeUrl) },
        color = C.Surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {

            Image(
                painter = rememberAsyncImagePainter(meal.imageUrl),
                contentDescription = meal.title,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(end = 12.dp),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    meal.title,
                    fontSize = 18.sp,
                    color = C.OnBackground
                )
                Spacer(Modifier.height(6.dp))

                if (meal.usedIngredients.isNotEmpty()) {
                    Text(
                        text = "Uses: " +
                                meal.usedIngredients.joinToString(", ") { it.name },
                        fontSize = 13.sp,
                        color = C.OnSurfaceVariant
                    )
                }

                if (meal.missedIngredients.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Missing: " +
                                meal.missedIngredients.joinToString(", ") { it.name },
                        fontSize = 13.sp,
                        color = C.OnSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Only show button if there are missing ingredients
                if (meal.missedIngredients.isNotEmpty()) {
                    Button(
                        onClick = onAddMissing,
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = C.Primary,
                            contentColor = C.Surface
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add Missing Ingredients")
                    }
                }
            }
        }
    }
}


