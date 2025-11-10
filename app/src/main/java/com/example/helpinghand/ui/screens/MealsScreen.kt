package com.example.helpinghand.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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

    // Local state for the search query
    var searchQuery by remember { mutableStateOf("") }

    // Filter meals by title or ingredients
    val filteredMeals = remember(meals, searchQuery) {
        if (searchQuery.isBlank()) meals
        else meals.filter { meal ->
            meal.title.contains(searchQuery, ignoreCase = true) ||
                    meal.usedIngredients.any { it.contains(searchQuery, ignoreCase = true) } ||
                    meal.missedIngredients.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(containerColor = C.Background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(C.Background)
        ) {

            // --- Top bar  ---
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                        navController.popBackStack()
                    }) {
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(C.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, null, tint = C.Surface)
                        }
                        Text("Shopping & Meals", fontSize = 20.sp, color = C.OnBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { /* settings later */ }) {
                        Icon(Icons.Filled.Settings, null, tint = C.OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.Background)
            )

            // --- Title Row ---
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
                        contentDescription = "Go Back",
                        tint = C.OnBackground
                    )
                }
                Text(
                    text = "Meals",
                    color = C.Primary,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // --- Search Bar  ---
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = C.Surface,
                    focusedContainerColor = C.Surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = C.Primary
                ),
                singleLine = true
            )

            // --- Meal list region  ---
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = C.Primary)
                    }
                }

                filteredMeals.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No meals found.", color = C.OnSurfaceVariant)
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredMeals) { meal ->
                            MealCard(meal)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun MealCard(meal: Meal) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(), // 💥 dynamic height instead of fixed!
        shape = RoundedCornerShape(16.dp),
        color = C.Surface,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top // ensures tall text expands down
        ) {
            // 🖼 Recipe image
            Image(
                painter = rememberAsyncImagePainter(meal.imageUrl),
                contentDescription = meal.title,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .padding(end = 12.dp),
                contentScale = ContentScale.Crop
            )

            // 📝 Recipe text (auto-expanding column)
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
                        text = "Uses: ${meal.usedIngredients.joinToString(", ")}",
                        fontSize = 13.sp,
                        color = C.OnSurfaceVariant
                    )
                }

                if (meal.missedIngredients.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Missing: ${meal.missedIngredients.joinToString(", ")}",
                        fontSize = 13.sp,
                        color = C.OnSurfaceVariant
                    )
                }
            }
        }
    }
}
