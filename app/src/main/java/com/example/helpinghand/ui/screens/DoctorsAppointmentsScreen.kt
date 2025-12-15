@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.helpinghand.ui.screens

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.helpinghand.data.model.DoctorAppointment
import com.example.helpinghand.data.model.formatPhoneNumber
import com.example.helpinghand.ui.theme.ShoppingColors as C
import com.example.helpinghand.viewmodel.DoctorAppointmentsViewModel
import java.time.LocalDate
import androidx.core.net.toUri

private const val MAX_NAME_CHARS = 60
private const val MAX_PHONE_CHARS = 20
private const val MAX_OFFICE_CHARS = 60

enum class AppointmentTypeUi {
    DOCTOR, DENTIST, SPECIALIST
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DoctorAppointmentsScreen(
    navController: NavHostController,
    viewModel: DoctorAppointmentsViewModel
) {
    val appointments by viewModel.appointments.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var appointmentToEdit by remember { mutableStateOf<DoctorAppointment?>(null) }

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
                            text = "Doctor Appointments",
                            fontSize = 20.sp,
                            color = C.OnBackground,
                            modifier = Modifier.testTag("dashboard_title")
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, null, tint = C.OnBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = C.Background)
            )

            // --- Controls Row (Add button) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(C.SurfaceVariant)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { showAddDialog = true },
                    label = { Text("Add Appointment", color = C.Primary, fontSize = 14.sp) },
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
                    val sections = listOf(
                        "Doctor" to "Doctors",
                        "Dentist" to "Dentists",
                        "Specialist" to "Specialists"
                    )

                    sections.forEach { (typeKey, label) ->
                        val appointmentsForType = groupedAppointments[typeKey] ?: emptyList()
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
                                        modifier = Modifier.padding(
                                            vertical = 12.dp,
                                            horizontal = 4.dp
                                        )
                                    )
                                }
                            }

                            items(appointmentsForType, key = { it.id }) { appointment ->
                                AppointmentRow(
                                    appointment = appointment,
                                    onUpdateNextVisit = { newDate ->
                                        viewModel.updateNextVisit(appointment, newDate)
                                    },
                                    onEdit = {
                                        appointmentToEdit = appointment
                                        showEditDialog = true
                                    },
                                    onDelete = {
                                        viewModel.deleteAppointment(appointment)
                                    }
                                )
                                Divider(color = C.OnSurfaceVariant.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }
        }

        // --- Add Appointment Dialog ---
        if (showAddDialog) {
            AddAppointmentDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, typeString, phone, office, intervalMonths ->
                    viewModel.addAppointment(name, typeString, phone, office, intervalMonths)
                    showAddDialog = false
                }
            )
        }

        // --- Edit Appointment Dialog ---
        if (showEditDialog && appointmentToEdit != null) {
            EditAppointmentDialog(
                appointment = appointmentToEdit!!,
                onDismiss = { showEditDialog = false },
                onSave = { name, typeString, phone, office, intervalMonths ->
                    viewModel.updateAppointment(
                        appointmentToEdit!!,
                        name,
                        typeString,
                        phone,
                        office,
                        intervalMonths
                    )
                    showEditDialog = false
                }
            )
        }
    }
    if (showHelpDialog) {
        OnboardingDialog(onDismiss = { showHelpDialog = false })}
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AppointmentRow(
    appointment: DoctorAppointment,
    onUpdateNextVisit: (LocalDate) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

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

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit appointment",
                        tint = C.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete appointment",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Expand/Collapse
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = C.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))

            // Next Appointment section
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

                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = "Set appointment date",
                            tint = C.Primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (appointment.phoneRaw.isNotBlank()) {
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
                            text = appointment.displayPhone(),
                            fontSize = 14.sp,
                            color = C.OnBackground
                        )
                    }
                    IconButton(onClick = {
                        val phone = formatPhoneNumber(appointment.phoneRaw).replace(" ", "")
                        if (phone.isNotEmpty()) {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = "tel:$phone".toUri()
                            }
                            context.startActivity(intent)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = "Call",
                            tint = C.Primary
                        )
                    }
                }
            }

            if (appointment.officeName.isNotBlank()) {
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
        }
    }

    if (showDatePicker) {
        val initialMillis = appointment.nextVisitEpochDay
            ?.let { LocalDate.ofEpochDay(it.toLong()) }
            ?.toEpochDay()
            ?.times(86_400_000)
            ?: System.currentTimeMillis()

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = LocalDate.ofEpochDay(millis / 86_400_000)
                            onUpdateNextVisit(selectedDate)
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
private fun AddAppointmentDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, type: String, phone: String, office: String, intervalMonths: Int) -> Unit
) {
    var doctorName by remember { mutableStateOf(TextFieldValue("")) }
    var phoneNumber by remember { mutableStateOf(TextFieldValue("")) }
    var officeName by remember { mutableStateOf(TextFieldValue("")) }
    var selectedType by remember { mutableStateOf(AppointmentTypeUi.DOCTOR) }
    var intervalMonths by remember { mutableStateOf("6") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val name = doctorName.text.trim()
                    val phone = phoneNumber.text.trim()
                    val office = officeName.text.trim()
                    val interval = intervalMonths.toIntOrNull() ?: 6

                    if (name.isNotEmpty()) {
                        val typeString = when (selectedType) {
                            AppointmentTypeUi.DOCTOR -> "Doctor"
                            AppointmentTypeUi.DENTIST -> "Dentist"
                            AppointmentTypeUi.SPECIALIST -> "Specialist"
                        }
                        onAdd(name, typeString, phone, office, interval)
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
        title = { Text("Add New Appointment", color = C.OnBackground) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { new ->
                        if (new.text.length <= MAX_NAME_CHARS) {
                            doctorName = new
                        }
                    },
                    label = { Text("Doctor/Clinic Name (max $MAX_NAME_CHARS)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${doctorName.text.length}/$MAX_NAME_CHARS",
                            fontSize = 12.sp,
                            color = if (doctorName.text.length >= MAX_NAME_CHARS)
                                MaterialTheme.colorScheme.error
                            else
                                C.OnSurfaceVariant
                        )
                    }
                )

                // Type dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = when (selectedType) {
                            AppointmentTypeUi.DOCTOR -> "Doctor"
                            AppointmentTypeUi.DENTIST -> "Dentist"
                            AppointmentTypeUi.SPECIALIST -> "Specialist"
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
                                selectedType = AppointmentTypeUi.DOCTOR
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Dentist") },
                            onClick = {
                                selectedType = AppointmentTypeUi.DENTIST
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Specialist") },
                            onClick = {
                                selectedType = AppointmentTypeUi.SPECIALIST
                                expanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { new ->
                        val text = new.text
                        if (
                            text.length <= MAX_PHONE_CHARS &&
                            text.all { it.isDigit() || it == '-' || it == ' ' || it == '(' || it == ')' }
                        ) {
                            phoneNumber = new
                        }
                    },
                    label = { Text("Phone Number (max $MAX_PHONE_CHARS)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${phoneNumber.text.length}/$MAX_PHONE_CHARS",
                            fontSize = 12.sp,
                            color = if (phoneNumber.text.length >= MAX_PHONE_CHARS)
                                MaterialTheme.colorScheme.error
                            else
                                C.OnSurfaceVariant
                        )
                    }
                )

                OutlinedTextField(
                    value = officeName,
                    onValueChange = { new ->
                        if (new.text.length <= MAX_OFFICE_CHARS) {
                            officeName = new
                        }
                    },
                    label = { Text("Office Name (max $MAX_OFFICE_CHARS)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${officeName.text.length}/$MAX_OFFICE_CHARS",
                            fontSize = 12.sp,
                            color = if (officeName.text.length >= MAX_OFFICE_CHARS)
                                MaterialTheme.colorScheme.error
                            else
                                C.OnSurfaceVariant
                        )
                    }
                )

                OutlinedTextField(
                    value = intervalMonths,
                    onValueChange = { new ->
                        if (new.all { it.isDigit() } && new.length <= 2) {
                            intervalMonths = new
                        }
                    },
                    label = { Text("Visit Interval (months)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        containerColor = C.Surface
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun EditAppointmentDialog(
    appointment: DoctorAppointment,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, phone: String, office: String, intervalMonths: Int) -> Unit
) {
    var doctorName by remember { mutableStateOf(TextFieldValue(appointment.doctorName)) }
    var phoneNumber by remember { mutableStateOf(TextFieldValue(appointment.phoneRaw)) }
    var officeName by remember { mutableStateOf(TextFieldValue(appointment.officeName)) }
    var selectedType by remember {
        mutableStateOf(
            when (appointment.type) {
                "Dentist" -> AppointmentTypeUi.DENTIST
                "Specialist" -> AppointmentTypeUi.SPECIALIST
                else -> AppointmentTypeUi.DOCTOR
            }
        )
    }
    var intervalMonths by remember { mutableStateOf(appointment.intervalMonths.toString()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val name = doctorName.text.trim()
                    val phone = phoneNumber.text.trim()
                    val office = officeName.text.trim()
                    val interval = intervalMonths.toIntOrNull() ?: 6

                    if (name.isNotEmpty()) {
                        val typeString = when (selectedType) {
                            AppointmentTypeUi.DOCTOR -> "Doctor"
                            AppointmentTypeUi.DENTIST -> "Dentist"
                            AppointmentTypeUi.SPECIALIST -> "Specialist"
                        }
                        onSave(name, typeString, phone, office, interval)
                    }
                }
            ) {
                Text("Save", color = C.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = C.OnSurfaceVariant)
            }
        },
        title = { Text("Edit Appointment", color = C.OnBackground) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { new ->
                        if (new.text.length <= MAX_NAME_CHARS) {
                            doctorName = new
                        }
                    },
                    label = { Text("Doctor/Clinic Name (max $MAX_NAME_CHARS)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${doctorName.text.length}/$MAX_NAME_CHARS",
                            fontSize = 12.sp,
                            color = if (doctorName.text.length >= MAX_NAME_CHARS)
                                MaterialTheme.colorScheme.error
                            else
                                C.OnSurfaceVariant
                        )
                    }
                )

                // Type dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = when (selectedType) {
                            AppointmentTypeUi.DOCTOR -> "Doctor"
                            AppointmentTypeUi.DENTIST -> "Dentist"
                            AppointmentTypeUi.SPECIALIST -> "Specialist"
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
                                selectedType = AppointmentTypeUi.DOCTOR
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Dentist") },
                            onClick = {
                                selectedType = AppointmentTypeUi.DENTIST
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Specialist") },
                            onClick = {
                                selectedType = AppointmentTypeUi.SPECIALIST
                                expanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { new ->
                        val text = new.text
                        if (
                            text.length <= MAX_PHONE_CHARS &&
                            text.all { it.isDigit() || it == '-' || it == ' ' || it == '(' || it == ')' }
                        ) {
                            phoneNumber = new
                        }
                    },
                    label = { Text("Phone Number (max $MAX_PHONE_CHARS)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${phoneNumber.text.length}/$MAX_PHONE_CHARS",
                            fontSize = 12.sp,
                            color = if (phoneNumber.text.length >= MAX_PHONE_CHARS)
                                MaterialTheme.colorScheme.error
                            else
                                C.OnSurfaceVariant
                        )
                    }
                )

                OutlinedTextField(
                    value = officeName,
                    onValueChange = { new ->
                        if (new.text.length <= MAX_OFFICE_CHARS) {
                            officeName = new
                        }
                    },
                    label = { Text("Office Name (max $MAX_OFFICE_CHARS)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${officeName.text.length}/$MAX_OFFICE_CHARS",
                            fontSize = 12.sp,
                            color = if (officeName.text.length >= MAX_OFFICE_CHARS)
                                MaterialTheme.colorScheme.error
                            else
                                C.OnSurfaceVariant
                        )
                    }
                )

                OutlinedTextField(
                    value = intervalMonths,
                    onValueChange = { new ->
                        if (new.all { it.isDigit() } && new.length <= 2) {
                            intervalMonths = new
                        }
                    },
                    label = { Text("Visit Interval (months)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        containerColor = C.Surface
    )
}