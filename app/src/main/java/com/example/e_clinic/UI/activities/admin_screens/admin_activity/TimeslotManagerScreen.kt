@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.e_clinic.UI.activities.admin_screens.admin_activity

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.TimeSlot
import com.example.e_clinic.Firebase.Repositories.TimeSlotRepository
import com.google.firebase.Timestamp
import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Date

@Composable
fun TimeslotManagerScreen(
    selectedDoctor: Doctor,
    onBack: () -> Unit = {}
) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayMap = listOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    )
    val selectedDays = remember { mutableStateListOf<Int>() }
    var startHour by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endHour by remember { mutableStateOf(LocalTime.of(17, 0)) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(true) }
    var generatedSlots by remember { mutableStateOf(listOf<LocalDateTime>()) }
    var isSaving by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Manage Timeslots",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Doctor Info Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MedicalServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dr. ${selectedDoctor.name} ${selectedDoctor.surname}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = selectedDoctor.specialization,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Days of week selection
                    Text(
                        text = "Working Days",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysOfWeek.forEachIndexed { idx, label ->
                            val selected = selectedDays.contains(idx)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected) selectedDays.remove(idx) else selectedDays.add(idx)
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Working hours
                    Text(
                        text = "Working Hours",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedCard(
                            onClick = {
                                pickingStart = true
                                showTimePicker = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Start Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "%02d:%02d".format(startHour.hour, startHour.minute),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        OutlinedCard(
                            onClick = {
                                pickingStart = false
                                showTimePicker = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "End Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "%02d:%02d".format(endHour.hour, endHour.minute),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (selectedDays.isEmpty()) return@Button
                            isSaving = true
                            val repository = TimeSlotRepository()
                            val slots = mutableListOf<LocalDateTime>()
                            val today = LocalDate.now()

                            selectedDays.forEach { dayIdx ->
                                val dayOfWeek = dayMap[dayIdx]
                                val nextOrSameDate = today.with(TemporalAdjusters.nextOrSame(dayOfWeek))
                                val startDateTime = nextOrSameDate.atTime(startHour)
                                val endDateTime = nextOrSameDate.atTime(endHour)

                                var currentSlot = startDateTime
                                while (currentSlot.isBefore(endDateTime)) {
                                    slots.add(currentSlot)
                                    currentSlot = currentSlot.plus(1, ChronoUnit.HOURS)
                                }
                            }

                            generatedSlots = slots

                            slots.forEach { slot ->
                                val instant = slot.atZone(ZoneId.systemDefault()).toInstant()
                                val timeslot = TimeSlot(
                                    id = "",
                                    doctor_id = selectedDoctor.id,
                                    specialization = selectedDoctor.specialization,
                                    available_slots = listOf(Timestamp(Date.from(instant)))
                                )
                                repository.getCollection().add(timeslot)
                            }
                            isSaving = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedDays.isNotEmpty() && !isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Generate & Save Timeslots",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (generatedSlots.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { generatedSlots = emptyList() },
            title = { Text("Timeslots Generated") },
            text = { Text("Successfully generated ${generatedSlots.size} one-hour appointment slots for Dr. ${selectedDoctor.name} ${selectedDoctor.surname}.") },
            confirmButton = {
                Button(
                    onClick = {
                        generatedSlots = emptyList()
                        onBack()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Done")
                }
            }
        )
    }

    if (showTimePicker) {
        val initialHour = if (pickingStart) startHour.hour else endHour.hour
        val initialMinute = if (pickingStart) startHour.minute else endHour.minute
        TimePickerDialog(
            context,
            { _, pickedHour, pickedMinute ->
                if (pickingStart) {
                    startHour = LocalTime.of(pickedHour, pickedMinute)
                } else {
                    endHour = LocalTime.of(pickedHour, pickedMinute)
                }
                showTimePicker = false
            },
            initialHour,
            initialMinute,
            true
        ).show()
    }
}