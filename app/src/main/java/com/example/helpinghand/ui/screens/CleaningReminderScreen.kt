package com.example.helpinghand.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.helpinghand.data.model.CleaningReminder
import com.example.helpinghand.data.model.HouseholdMember
import com.example.helpinghand.viewmodel.CleaningReminderViewModel
import com.example.helpinghand.ui.theme.ShoppingColors as C

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleaningReminderScreen(
    navController: NavHostController,
    viewModel: CleaningReminderViewModel,
    householdMembers: List<HouseholdMember>,
    currentUserUid: String?
) {
    val reminderItems by viewModel.reminders.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newInterval by remember { mutableStateOf("") }

    // Assignee selection state (only used when householdMembers.size > 1)
    var assigneeExpanded by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<HouseholdMember?>(null) }

    val membersByUid = remember(householdMembers) {
        householdMembers.associateBy { it.uid }
    }

    fun memberLabel(uid: String): String {
        val m = membersByUid[uid]
        return m?.displayName?.takeIf { it.isNotBlank() }
            ?: m?.email
            ?: "Household member"
    }

    val canAssign = householdMembers.size > 1

    // Menu options for reassignment and creation
    val assigneeMenu = remember(householdMembers) {
        buildList {
            add(null to "Unassigned")
            householdMembers.forEach { member ->
                val label = member.displayName.takeIf { it.isNotBlank() } ?: member.email
                add(member.uid to label)
            }
        }
    }

    Scaffold(containerColor = C.Background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("cleaning_screen")
        ) {
            // Top App Bar
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.testTag("cleaning_back")
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(C.Primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile",
                                tint = C.Surface
                            )
                        }
                        Text(
                            text = "Cleaning",
                            fontSize = 20.sp,
                            color = C.OnBackground,
                            modifier = Modifier.testTag("cleaning_title")
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("settings") },
                        modifier = Modifier.testTag("cleaning_settings_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = C.OnBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = C.Background
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = C.SurfaceVariant
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Centered "+"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(140.dp)
                                .height(40.dp)
                                .shadow(6.dp, RoundedCornerShape(999.dp), clip = false)
                                .clickable {
                                    newName = ""
                                    newInterval = ""
                                    selectedMember = null
                                    assigneeExpanded = false
                                    showDialog = true
                                }
                                .testTag("cleaning_add_button"),
                            shape = RoundedCornerShape(999.dp),
                            color = C.SurfaceVariant,
                            tonalElevation = 4.dp,
                            border = BorderStroke(1.5.dp, C.Primary.copy(alpha = 0.6f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add cleaning task",
                                    tint = C.Primary
                                )
                            }
                        }
                    }

                    val todayEpochDay = remember {
                        java.time.LocalDate.now().toEpochDay().toInt()
                    }

                    // Grouping logic
                    val mine = remember(reminderItems, currentUserUid) {
                        reminderItems.filter {
                            it.assignedToUid != null &&
                                    currentUserUid != null &&
                                    it.assignedToUid == currentUserUid
                        }
                    }
                    val unassigned = remember(reminderItems) {
                        reminderItems.filter { it.assignedToUid == null }
                    }
                    val othersGrouped = remember(reminderItems, currentUserUid) {
                        reminderItems
                            .filter { it.assignedToUid != null && it.assignedToUid != currentUserUid }
                            .groupBy { it.assignedToUid!! }
                    }

                    val otherUidsSorted = remember(othersGrouped, householdMembers) {
                        othersGrouped.keys.sortedBy { uid -> memberLabel(uid) }
                    }

                    // Flattened list order when headers are hidden
                    val flatOrdered = remember(mine, unassigned, othersGrouped, otherUidsSorted) {
                        buildList {
                            addAll(mine)
                            addAll(unassigned)
                            otherUidsSorted.forEach { uid ->
                                addAll(othersGrouped[uid].orEmpty())
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("cleaning_list"),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isEmpty = mine.isEmpty() && unassigned.isEmpty() && othersGrouped.isEmpty()

                        if (isEmpty) {
                            item {
                                Text(
                                    text = "No cleaning tasks yet",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    color = C.OnSurfaceVariant
                                )
                            }
                        } else {
                            if (canAssign) {
                                // HEADERS MODE (household size > 1)

                                if (mine.isNotEmpty()) {
                                    stickyHeader {
                                        CleaningSectionHeader(
                                            title = "Assigned to you",
                                            icon = Icons.Filled.Star
                                        )
                                    }
                                    items(mine, key = { it.id }) { item ->
                                        val daysUntil =
                                            (item.nextDueEpochDay - todayEpochDay).coerceAtLeast(0)
                                        CleaningReminderCard(
                                            item = item,
                                            daysUntil = daysUntil,
                                            canAssign = canAssign,
                                            assigneeMenu = assigneeMenu,
                                            onAssign = { uid, label ->
                                                val nameOrNull = if (uid == null) null else label
                                                viewModel.reassignReminder(item, uid, nameOrNull)
                                            },
                                            onResetClick = { viewModel.resetCycle(item) },
                                            onDeleteClick = { viewModel.deleteReminder(item) }
                                        )
                                    }
                                }

                                if (unassigned.isNotEmpty()) {
                                    stickyHeader {
                                        CleaningSectionHeader(
                                            title = "Unassigned",
                                            icon = Icons.Filled.PersonOff
                                        )
                                    }
                                    items(unassigned, key = { it.id }) { item ->
                                        val daysUntil =
                                            (item.nextDueEpochDay - todayEpochDay).coerceAtLeast(0)
                                        CleaningReminderCard(
                                            item = item,
                                            daysUntil = daysUntil,
                                            canAssign = canAssign,
                                            assigneeMenu = assigneeMenu,
                                            onAssign = { uid, label ->
                                                val nameOrNull = if (uid == null) null else label
                                                viewModel.reassignReminder(item, uid, nameOrNull)
                                            },
                                            onResetClick = { viewModel.resetCycle(item) },
                                            onDeleteClick = { viewModel.deleteReminder(item) }
                                        )
                                    }
                                }

                                otherUidsSorted.forEach { uid ->
                                    val group = othersGrouped[uid].orEmpty()
                                    if (group.isNotEmpty()) {
                                        stickyHeader {
                                            CleaningSectionHeader(
                                                title = memberLabel(uid),
                                                icon = Icons.Filled.Person
                                            )
                                        }
                                        items(group, key = { it.id }) { item ->
                                            val daysUntil =
                                                (item.nextDueEpochDay - todayEpochDay).coerceAtLeast(0)
                                            CleaningReminderCard(
                                                item = item,
                                                daysUntil = daysUntil,
                                                canAssign = canAssign,
                                                assigneeMenu = assigneeMenu,
                                                onAssign = { newUid, label ->
                                                    val nameOrNull = if (newUid == null) null else label
                                                    viewModel.reassignReminder(item, newUid, nameOrNull)
                                                },
                                                onResetClick = { viewModel.resetCycle(item) },
                                                onDeleteClick = { viewModel.deleteReminder(item) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                // NO HEADERS MODE (household size == 1)
                                items(flatOrdered, key = { it.id }) { item ->
                                    val daysUntil =
                                        (item.nextDueEpochDay - todayEpochDay).coerceAtLeast(0)
                                    CleaningReminderCard(
                                        item = item,
                                        daysUntil = daysUntil,
                                        canAssign = false,
                                        assigneeMenu = assigneeMenu,
                                        onAssign = { _, _ -> },
                                        onResetClick = { viewModel.resetCycle(item) },
                                        onDeleteClick = { viewModel.deleteReminder(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog for new reminder
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("New cleaning task", color = C.OnBackground) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { input ->
                                if (input.length <= 15) newName = input
                            },
                            label = { Text("Task name (max 15 chars)") },
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = newInterval,
                            onValueChange = { newInterval = it },
                            label = { Text("Days between cleanings") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        if (canAssign) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Assign to",
                                fontSize = 14.sp,
                                color = C.OnSurfaceVariant
                            )

                            ExposedDropdownMenuBox(
                                expanded = assigneeExpanded,
                                onExpandedChange = { assigneeExpanded = !assigneeExpanded }
                            ) {
                                val selectedLabel =
                                    selectedMember?.displayName?.takeIf { it.isNotBlank() }
                                        ?: selectedMember?.email
                                        ?: "Unassigned"

                                TextField(
                                    value = selectedLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Assignee") },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = assigneeExpanded)
                                    },
                                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                                )

                                ExposedDropdownMenu(
                                    expanded = assigneeExpanded,
                                    onDismissRequest = { assigneeExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Unassigned") },
                                        onClick = {
                                            selectedMember = null
                                            assigneeExpanded = false
                                        }
                                    )

                                    householdMembers.forEach { member ->
                                        val label =
                                            member.displayName.takeIf { it.isNotBlank() }
                                                ?: member.email
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                selectedMember = member
                                                assigneeExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val interval = newInterval.toIntOrNull()
                            if (interval != null && interval > 0 && newName.isNotBlank()) {
                                val assignedUid = if (canAssign) selectedMember?.uid else null
                                val assignedName = if (canAssign) {
                                    selectedMember?.displayName?.takeIf { it.isNotBlank() }
                                        ?: selectedMember?.email
                                } else null

                                viewModel.addReminder(
                                    name = newName.trim(),
                                    intervalDays = interval,
                                    assignedToUid = assignedUid,
                                    assignedToName = assignedName
                                )

                                newName = ""
                                newInterval = ""
                                selectedMember = null
                                showDialog = false
                            }
                        },
                        modifier = Modifier.testTag("cleaning_dialog_confirm")
                    ) {
                        Text("Add", color = C.Primary)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDialog = false },
                        modifier = Modifier.testTag("cleaning_dialog_cancel")
                    ) {
                        Text("Cancel", color = C.OnSurfaceVariant)
                    }
                },
                containerColor = C.Surface
            )
        }
    }
}

@Composable
private fun CleaningSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    // Nicer header: pill surface with icon + label
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(C.SurfaceVariant) // ensures sticky header masks items beneath
            .padding(top = 6.dp, bottom = 6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(999.dp), clip = false),
            shape = RoundedCornerShape(999.dp),
            color = C.Surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, C.Primary.copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = C.Primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = C.Primary,
                        modifier = Modifier.padding(6.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = C.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CleaningReminderCard(
    item: CleaningReminder,
    daysUntil: Int,
    canAssign: Boolean,
    assigneeMenu: List<Pair<String?, String>>,
    onAssign: (String?, String) -> Unit,
    onResetClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var assignExpanded by remember(item.id) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            ),
        shape = RoundedCornerShape(24.dp),
        color = C.Surface,
        tonalElevation = 6.dp,
        border = BorderStroke(1.5.dp, C.Primary.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.name,
                fontSize = 16.sp,
                color = C.Primary,
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = "Next cleaning",
                        tint = C.OnBackground,
                        modifier = Modifier.size(18.dp)
                    )

                    val label = when {
                        daysUntil <= 0 -> "Due now"
                        daysUntil == 1 -> "Tomorrow!"
                        else -> "$daysUntil days"
                    }

                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = C.OnSurfaceVariant
                    )
                }

                // Reset pill
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = C.SurfaceVariant,
                    modifier = Modifier.clickable(onClick = onResetClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isDue = daysUntil <= 0
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = if (isDue) "Due" else "On schedule",
                            tint = if (isDue) MaterialTheme.colorScheme.error.copy(alpha = 0.55f) else C.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "",
                            fontSize = 14.sp,
                            color = C.Primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Reassign menu
                if (canAssign) {
                    Box {
                        IconButton(
                            onClick = { assignExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Reassign task",
                                tint = C.OnSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = assignExpanded,
                            onDismissRequest = { assignExpanded = false }
                        ) {
                            assigneeMenu.forEach { (uid, label) ->
                                val isSelected =
                                    (uid == null && item.assignedToUid == null) ||
                                            (uid != null && uid == item.assignedToUid)

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = "Selected",
                                                    tint = C.Primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.size(16.dp))
                                            }
                                            Text(label)
                                        }
                                    },
                                    onClick = {
                                        assignExpanded = false
                                        onAssign(uid, label)
                                    }
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete reminder",
                        tint = C.OnSurfaceVariant
                    )
                }
            }
        }
    }
}
