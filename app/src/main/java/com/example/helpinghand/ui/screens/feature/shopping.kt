package com.example.helpinghand.ui.screens.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colors from your Figma design
object AppColors {
    val Background = Color(0xFFEADDFF)
    val Primary = Color(0xFF6750A4)
    val OnBackground = Color(0xFF1D1B20)
    val OnSurfaceVariant = Color(0xFF49454F)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFFEF7FF)
    val GestureBar = Color(0xFFF3EDF7)
}

data class ListItem(
    val id: Int,
    val text: String,
    var isChecked: Boolean = false
)

@Composable
fun ShoppingApp() {
    var currentScreen by remember { mutableStateOf("shopping") }

    when (currentScreen) {
        "shopping" -> ShoppingListScreen(
            onNavigateToMeals = { currentScreen = "meals" }
        )
        "meals" -> MealsScreen(
            onNavigateBack = { currentScreen = "shopping" }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onNavigateToMeals: () -> Unit
) {
    var listItems by remember {
        mutableStateOf(
            List(9) { index ->
                ListItem(id = index, text = "List item")
            }
        )
    }

    Scaffold(
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Bar
            StatusBar()

            // Top App Bar
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AppColors.Primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            text = "Shopping & Meals",
                            fontSize = 22.sp,
                            color = AppColors.OnBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = AppColors.OnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )

            // Shopping List Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onNavigateToMeals() },
                shape = RoundedCornerShape(12.dp),
                color = AppColors.SurfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shopping List",
                        fontSize = 16.sp,
                        color = AppColors.OnBackground
                    )
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Open",
                        tint = AppColors.OnBackground
                    )
                }
            }

            // Sorting Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SortByAlpha,
                        contentDescription = "Sort",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Alphabetical",
                        fontSize = 14.sp,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { /* Edit */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = AppColors.OnBackground
                        )
                    }
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = AppColors.OnBackground
                        )
                    }
                }
            }

            // List Items
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(listItems) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.text,
                            fontSize = 16.sp,
                            color = AppColors.OnBackground
                        )
                        Checkbox(
                            checked = item.isChecked,
                            onCheckedChange = { checked ->
                                listItems = listItems.map {
                                    if (it.id == item.id) it.copy(isChecked = checked) else it
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AppColors.Primary
                            )
                        )
                    }
                    if (item.id < listItems.lastIndex) {
                        HorizontalDivider(color = AppColors.OnSurfaceVariant.copy(alpha = 0.2f))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Suggested Recipes Section
                    Text(
                        text = "Suggested Recipes",
                        fontSize = 14.sp,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = AppColors.Surface,
                            shadowElevation = 2.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize())
                        }

                        Surface(
                            modifier = Modifier
                                .width(80.dp)
                                .height(120.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = AppColors.Surface,
                            shadowElevation = 2.dp
                        ) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            // Gesture Bar
            GestureBar()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealsScreen(
    onNavigateBack: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = AppColors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Bar
            StatusBar()

            // Top App Bar with Back Button
            TopAppBar(
                title = {
                    Text(
                        text = "Meals",
                        fontSize = 22.sp,
                        color = AppColors.OnBackground,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppColors.OnBackground
                        )
                    }
                },
                actions = {
                    // Empty box to center the title
                    Box(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background
                )
            )

            // Search Bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = {
                    Text(
                        text = "Hinted search text",
                        color = AppColors.OnSurfaceVariant
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = AppColors.OnSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = AppColors.Surface,
                    focusedContainerColor = AppColors.Surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = AppColors.Primary
                ),
                singleLine = true
            )

            // Meal Cards
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) { index ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (index == 0) 140.dp else 120.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = AppColors.Surface,
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = if (index == 0) Alignment.Center else Alignment.TopEnd
                        ) {
                            if (index == 0) {
                                IconButton(
                                    onClick = { /* Add meal */ },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = "Add",
                                        tint = AppColors.Primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Gesture Bar
            GestureBar()
        }
    }
}

@Composable
fun StatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "9:30",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AppColors.OnBackground
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.SignalCellularAlt,
                contentDescription = "Signal",
                tint = AppColors.OnBackground,
                modifier = Modifier.size(16.dp)
            )
            Icon(
                imageVector = Icons.Filled.Wifi,
                contentDescription = "WiFi",
                tint = AppColors.OnBackground,
                modifier = Modifier.size(16.dp)
            )
            Icon(
                imageVector = Icons.Filled.BatteryFull,
                contentDescription = "Battery",
                tint = AppColors.OnBackground,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun GestureBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(AppColors.GestureBar),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .background(
                    AppColors.OnBackground,
                    shape = RoundedCornerShape(2.dp)
                )
        )
    }
}

// Main entry point
@Composable
fun ShoppingAndMealsApp() {
    MaterialTheme {
        ShoppingApp()
    }
}
