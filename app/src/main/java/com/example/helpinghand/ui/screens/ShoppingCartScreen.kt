package com.example.helpinghand.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.helpinghand.data.model.ShoppingItem
import com.example.helpinghand.ui.viewmodel.ShoppingCartViewModel
import com.example.helpinghand.viewmodel.MealsViewModel
import com.example.helpinghand.ui.theme.ShoppingColors as C
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingCartScreen(
    navController: NavHostController,
    viewModel: ShoppingCartViewModel,
    mealsViewModel: MealsViewModel
) {
    val items by viewModel.items.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newItemText by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(containerColor = C.Background) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // --- Top App Bar ---
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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

                // Centered text
                Text(
                    text = "Shopping Cart",
                    color = C.Primary,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Right-aligned arrow
                IconButton(
                    onClick = { navController.navigate("meals") },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "Go Back",
                        tint = C.OnBackground
                    )
                }
            }


            // --- Controls Row ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(C.SurfaceVariant)
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { showDialog = true },
                    label = { Text("Add item", color = C.Primary) },
                    leadingIcon = {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = C.Primary)
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = C.Surface)
                )

                Spacer(Modifier.weight(1f))

                // Generate Meals button
                IconButton(onClick = {
                    mealsViewModel.fetchMealsFromCheckedItems()
                    navController.navigate("meals")
                }) {
                    Icon(
                        imageVector = Icons.Filled.RestaurantMenu,
                        contentDescription = "Generate Meals",
                        tint = C.OnBackground
                    )
                }

                // delete button
                IconButton(onClick = { viewModel.deleteChecked() }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Checked", tint = C.OnBackground)
                }
            }

            // --- List ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = C.SurfaceVariant
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ShoppingRow(item, onCheck = { checked ->
                            viewModel.toggleChecked(item, checked)
                        })
                        Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                    }
                }
            }

            // --- Bottom Placeholder Section ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(C.Background)
            ) {
                Surface(color = C.Primary.copy(alpha = 0.12f)) {
                    Text(
                        "Suggested Recipes",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = C.OnBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlaceholderCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                    )
                    PlaceholderCard(
                        modifier = Modifier
                            .width(56.dp)
                            .height(120.dp)
                    )
                }
            }
        }

        // --- Add Item Dialog ---
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val name = newItemText.text.trim()
                            if (name.isNotEmpty()) {
                                viewModel.addItem(name)
                                newItemText = TextFieldValue("")
                                showDialog = false
                            }
                        }
                    ) {
                        Text("Add", color = C.Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel", color = C.OnSurfaceVariant)
                    }
                },
                title = { Text("Add New Item", color = C.OnBackground) },
                text = {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        label = { Text("Item name") },
                        singleLine = true
                    )
                },
                containerColor = C.Surface
            )
        }
    }
}

@Composable
private fun ShoppingRow(item: ShoppingItem, onCheck: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.text, color = C.OnBackground, fontSize = 16.sp)
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = onCheck,
            colors = CheckboxDefaults.colors(checkedColor = C.Primary)
        )
    }
}

@Composable
private fun PlaceholderCard(modifier: Modifier) {
    Surface(
        modifier = modifier.shadow(6.dp, RoundedCornerShape(18.dp), clip = false),
        shape = RoundedCornerShape(18.dp),
        color = C.Surface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.5.dp, C.Primary.copy(alpha = 0.6f))
    ) {}
}
