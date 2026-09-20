package com.example.e_clinic.UI.activities.admin_screens.admin_activity

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DoctorsScreen() {
    val doctors = remember { mutableStateListOf<Doctor>() }
    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showCreateDoctorScreen by remember { mutableStateOf(false) }
    var showTimeslotManager by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshDoctors() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("doctors").get().addOnSuccessListener { snapshot ->
            doctors.clear()
            for (document in snapshot.documents) {
                val doctor = Doctor(
                    id = document.id,
                    name = document.getString("name") ?: "Unknown",
                    surname = document.getString("surname") ?: "Unknown",
                    gender = document.getString("gender") ?: "Unknown",
                    phone = document.getString("phone") ?: "Unknown",
                    email = document.getString("e-mail") ?: document.getString("email") ?: "Unknown",
                    specialization = document.getString("specialization") ?: "Unknown",
                    address = document.getString("address") ?: "Unknown",
                    experience = document.getString("experience") ?: "0",
                    profilePicture = document.getString("profilePicture") ?: "",
                )
                doctors.add(doctor)
            }
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshDoctors()
    }

    if (showCreateDoctorScreen) {
        NewDoctorScreen(
            onDoctorAdded = {
                showCreateDoctorScreen = false
                refreshDoctors()
            },
            onBack = { showCreateDoctorScreen = false }
        )
        return
    }

    if (showTimeslotManager && selectedDoctor != null) {
        TimeslotManagerScreen(
            selectedDoctor = selectedDoctor!!,
            onBack = { showTimeslotManager = false }
        )
        return
    }

    if (showDialog && selectedDoctor != null) {
        DataManagerScreen(
            id = selectedDoctor!!.id,
            type = "doctor",
            onBack = {
                showDialog = false
                refreshDoctors()
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item {
                    Text(
                        text = "Manage Doctors (${doctors.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                if (doctors.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No doctors registered yet. Tap '+' to add one.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(doctors) { doctor ->
                    var isFlipped by remember { mutableStateOf(false) }
                    val rotation by animateFloatAsState(targetValue = if (isFlipped) 180f else 0f)

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable { isFlipped = !isFlipped }
                            .graphicsLayer {
                                rotationY = rotation
                                cameraDistance = 12f * density
                            },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rotation <= 90f) {
                                // Front side
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (!doctor.profilePicture.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = doctor.profilePicture,
                                            contentDescription = "Doctor Avatar",
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(60.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = "Doctor",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Dr. ${doctor.name} ${doctor.surname}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = doctor.specialization,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Email: ${doctor.email}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Phone: ${doctor.phone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Experience: ${doctor.experience} yrs",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                // Back side (Options)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { rotationY = 180f },
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Actions: Dr. ${doctor.surname}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Button(
                                        onClick = {
                                            selectedDoctor = doctor
                                            showDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Modify Doctor Data")
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            selectedDoctor = doctor
                                            showTimeslotManager = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Manage Timeslots")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            selectedDoctor = doctor
                                            showDeleteDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Remove Doctor", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }

        FloatingActionButton(
            onClick = { showCreateDoctorScreen = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Doctor", modifier = Modifier.size(28.dp))
        }
    }

    if (showDeleteDialog && selectedDoctor != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to remove Dr. ${selectedDoctor!!.name} ${selectedDoctor!!.surname}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        FirebaseFirestore.getInstance().collection("doctors").document(selectedDoctor!!.id).delete()
                        doctors.remove(selectedDoctor)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}