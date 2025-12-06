@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.helpinghand.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.helpinghand.ui.theme.ShoppingColors as C
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Data classes
data class DoctorAppointment(
    val id: Int,
    val doctorName: String,
    val type: AppointmentType,
    val lastVisitDate: LocalDate?,
    val nextVisitDate: LocalDate?,
    val phoneNumber: String = "",
    val officeName: String = "",
    val intervalMonths: Int = 6,
    val documents: List<String> = emptyList()
) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun getNextVisitText(): String {
        if (nextVisitDate == null && lastVisitDate == null) {
            return "No visits scheduled"
        }

        val today = LocalDate.now()

        return when {
            nextVisitDate != null -> {
                val daysUntil = ChronoUnit.DAYS.between(today, nextVisitDate)
                when {
                    daysUntil < 0 -> "Overdue by ${-daysUntil} days"
                    daysUntil == 0L -> "Today"
                    daysUntil < 7 -> "In $daysUntil days"
                    daysUntil < 30 -> "In ${daysUntil / 7} weeks"
                    else -> "In ${daysUntil / 30} months"
                }
            }
            lastVisitDate != null -> {
                val monthsAgo = ChronoUnit.MONTHS.between(lastVisitDate, today)
                "Last visit ${monthsAgo}mo ago"
            }
            else -> "No visits scheduled"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getFormattedNextDate(): String {
        return nextVisitDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: "Not set"
    }
}

enum class AppointmentType {
    DOCTOR,
    DENTIST,
    SPECIALIST
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorAppointmentsScreen(
    navController: NavHostController
) {
    var appointments by remember {
        mutableStateOf(
            listOf(
                DoctorAppointment(
                    id = 1,
                    doctorName = "Dr XXX",
                    type = AppointmentType.DOCTOR,
                    lastVisitDate = LocalDate.now().minusMonths(5),
                    nextVisitDate = LocalDate.now().plusMonths(1),
                    phoneNumber = "(555) 123-4567",
                    officeName = "Main Medical Center"
                ),
                DoctorAppointment(
                    id = 2,
                    doctorName = "Dr XXX",
                    type = AppointmentType.DOCTOR,
                    lastVisitDate = null,
                    nextVisitDate = LocalDate.now().plusWeeks(2),
                    phoneNumber = "(555) 234-5678",
                    officeName = "Family Health Clinic"
                ),
                DoctorAppointment(
                    id = 3,
                    doctorName = "Eye Clinic",
                    type = AppointmentType.SPECIALIST,
                    lastVisitDate = null,
                    nextVisitDate = LocalDate.now().plusMonths(3),
                    phoneNumber = "555-5555",
                    officeName = "Vision Specialists"
                )
            )
        )
    }

    var showAddContactDialog by remember { mutableStateOf(false) }
    var showAddDocumentDialog by remember { mutableStateOf(false) }

    val groupedAppointments = appointments.groupBy { it.type }

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
                            contentDescription = "Back",
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
                        Text("Doctor Appointments", fontSize = 20.sp, color = C.OnBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { /* settings */ }) {
                        Icon(Icons.Filled.Settings, null, tint = C.OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.Background)
            )

            // --- Controls Row (Add buttons) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(C.SurfaceVariant)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { showAddContactDialog = true },
                    label = { Text("Add New Contact", color = C.Primary, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = C.Primary)
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = C.Surface),
                    modifier = Modifier.weight(1f)
                )

                AssistChip(
                    onClick = { showAddDocumentDialog = true },
                    label = { Text("Add New Document", color = C.Primary, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = C.Primary)
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = C.Surface),
                    modifier = Modifier.weight(1f)
                )
            }

            // --- Appointments List with Sticky Headers ---
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
                    // Group by appointment type with sticky headers
                    listOf(
                        AppointmentType.DOCTOR to "Doctors",
                        AppointmentType.DENTIST to "Dentist",
                        AppointmentType.SPECIALIST to "Specialists"
                    ).forEach { (type, label) ->
                        val appointmentsForType = groupedAppointments[type] ?: emptyList()

                        if (appointmentsForType.isNotEmpty()) {
                            stickyHeader {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = C.SurfaceVariant
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = C.Primary,
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                    )
                                }
                            }

                            items(appointmentsForType) { appointment ->
                                AppointmentRow(
                                    appointment = appointment,
                                    onUpdateAppointment = { updated ->
                                        appointments = appointments.map {
                                            if (it.id == updated.id) updated else it
                                        }
                                    }
                                )
                                Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }
        }

        // --- Add Contact Dialog ---
        if (showAddContactDialog) {
            AddContactDialog(
                onDismiss = { showAddContactDialog = false },
                onAdd = { newAppointment ->
                    appointments = appointments + newAppointment
                    showAddContactDialog = false
                }
            )
        }

        // --- Add Document Dialog ---
        if (showAddDocumentDialog) {
            AddDocumentDialog(
                onDismiss = { showAddDocumentDialog = false },
                onAdd = { /* Handle document upload */ }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppointmentRow(
    appointment: DoctorAppointment,
    onUpdateAppointment: (DoctorAppointment) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.doctorName,
                    color = C.OnBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = appointment.getNextVisitText(),
                    color = C.OnSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = C.Primary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Expanded details
        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))

            // Next Appointment Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = C.Surface.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Next Appointment",
                            fontSize = 12.sp,
                            color = C.OnSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = appointment.getFormattedNextDate(),
                            fontSize = 16.sp,
                            color = C.OnBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { showDatePicker = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = "Set appointment date",
                            tint = C.Primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (appointment.phoneNumber.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Phone",
                            fontSize = 12.sp,
                            color = C.OnSurfaceVariant
                        )
                        Text(
                            text = appointment.phoneNumber,
                            fontSize = 14.sp,
                            color = C.OnBackground
                        )
                    }
                    IconButton(onClick = { /* Call */ }) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = "Call",
                            tint = C.Primary
                        )
                    }
                }
            }

            if (appointment.officeName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Office",
                    fontSize = 12.sp,
                    color = C.OnSurfaceVariant
                )
                Text(
                    text = appointment.officeName,
                    fontSize = 14.sp,
                    color = C.OnBackground
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Visit Interval",
                fontSize = 12.sp,
                color = C.OnSurfaceVariant
            )
            Text(
                text = "Every ${appointment.intervalMonths} months",
                fontSize = 14.sp,
                color = C.OnBackground
            )

            if (appointment.documents.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Documents",
                    fontSize = 12.sp,
                    color = C.OnSurfaceVariant
                )
                appointment.documents.forEach { doc ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = "Document",
                            tint = C.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = doc,
                            fontSize = 14.sp,
                            color = C.OnBackground
                        )
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = appointment.nextVisitDate?.toEpochDay()?.times(86400000)
                ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = LocalDate.ofEpochDay(millis / 86400000)
                            onUpdateAppointment(
                                appointment.copy(nextVisitDate = selectedDate)
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = C.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = C.OnSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AddContactDialog(
    onDismiss: () -> Unit,
    onAdd: (DoctorAppointment) -> Unit
) {
    var doctorName by remember { mutableStateOf(TextFieldValue("")) }
    var phoneNumber by remember { mutableStateOf(TextFieldValue("")) }
    var officeName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedType by remember { mutableStateOf(AppointmentType.DOCTOR) }
    var intervalMonths by remember { mutableStateOf("6") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val name = doctorName.text.trim()
                    if (name.isNotEmpty()) {
                        onAdd(
                            DoctorAppointment(
                                id = (0..10000).random(),
                                doctorName = name,
                                type = selectedType,
                                lastVisitDate = null,
                                nextVisitDate = LocalDate.now().plusMonths(intervalMonths.toLongOrNull() ?: 6),
                                phoneNumber = phoneNumber.text.trim(),
                                officeName = officeName.text.trim(),
                                intervalMonths = intervalMonths.toIntOrNull() ?: 6
                            )
                        )
                    }
                }
            ) {
                Text("Add", color = C.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = C.OnSurfaceVariant)
            }
        },
        title = { Text("Add New Contact", color = C.OnBackground) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doctor/Clinic Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = when(selectedType) {
                            AppointmentType.DOCTOR -> "Doctor"
                            AppointmentType.DENTIST -> "Dentist"
                            AppointmentType.SPECIALIST -> "Specialist"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Doctor") },
                            onClick = {
                                selectedType = AppointmentType.DOCTOR
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Dentist") },
                            onClick = {
                                selectedType = AppointmentType.DENTIST
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Specialist") },
                            onClick = {
                                selectedType = AppointmentType.SPECIALIST
                                expanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = officeName,
                    onValueChange = { officeName = it },
                    label = { Text("Office Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = intervalMonths,
                    onValueChange = { if (it.all { char -> char.isDigit() }) intervalMonths = it },
                    label = { Text("Visit Interval (months)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        containerColor = C.Surface
    )
}

@Composable
private fun AddDocumentDialog(
    onDismiss: () -> Unit,
    onAdd: () -> Unit
) {
    var documentName by remember { mutableStateOf(TextFieldValue("")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    if (documentName.text.trim().isNotEmpty()) {
                        onAdd()
                        onDismiss()
                    }
                }
            ) {
                Text("Add", color = C.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = C.OnSurfaceVariant)
            }
        },
        title = { Text("Add New Document", color = C.OnBackground) },
        text = {
            Column {
                OutlinedTextField(
                    value = documentName,
                    onValueChange = { documentName = it },
                    label = { Text("Document Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Upload functionality coming soon",
                    fontSize = 12.sp,
                    color = C.OnSurfaceVariant
                )
            }
        },
        containerColor = C.Surface
    )
}