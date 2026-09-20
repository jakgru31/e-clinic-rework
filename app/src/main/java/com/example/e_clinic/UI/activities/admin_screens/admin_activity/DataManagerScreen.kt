@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.e_clinic.UI.activities.admin_screens.admin_activity

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.specializations.DoctorSpecialization
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DataManagerScreen(
    id: String,
    type: String,
    onBack: () -> Unit = {}
) {
    val firestore = FirebaseFirestore.getInstance()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }

    var education by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    var specialization by remember { mutableStateOf("") }

    val genderOptions = listOf("Male", "Female")
    var genderExpanded by remember { mutableStateOf(false) }
    var specializationExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                dob = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    LaunchedEffect(id, type) {
        loading = true
        error = null
        success = false
        try {
            if (type == "user") {
                val doc = firestore.collection("users").document(id).get().await()
                val timestamp = doc.getTimestamp("dob")
                name = doc.getString("name") ?: ""
                surname = doc.getString("surname") ?: ""
                gender = doc.getString("gender") ?: ""
                phone = doc.getString("phone") ?: ""
                email = doc.getString("email") ?: ""
                address = doc.getString("address") ?: ""
                dob = timestamp?.toDate()?.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                } ?: ""
            } else if (type == "doctor") {
                val doc = firestore.collection("doctors").document(id).get().await()
                val timestamp = doc.getTimestamp("dob")
                name = doc.getString("name") ?: ""
                surname = doc.getString("surname") ?: ""
                gender = doc.getString("gender") ?: ""
                phone = doc.getString("phone") ?: ""
                email = doc.getString("e-mail") ?: doc.getString("email") ?: ""
                address = doc.getString("address") ?: ""
                specialization = doc.getString("specialization") ?: ""
                experience = doc.getString("experience") ?: ""
                education = doc.getString("education") ?: ""
                dob = timestamp?.toDate()?.let {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                } ?: ""
            }
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (type == "user") "Edit Patient Details" else "Edit Doctor Details",
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
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        if (error != null && !isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
            return@Scaffold
        }

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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Identity & Demographics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("First Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = surname,
                        onValueChange = { surname = it },
                        label = { Text("Last Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = !genderExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = if (gender.isEmpty()) "Select Gender" else gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = genderExpanded,
                            onDismissRequest = { genderExpanded = false }
                        ) {
                            genderOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        gender = option
                                        genderExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dob,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date of Birth") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() }
                    )

                    if (type == "doctor") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Professional Qualifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val specializationOptions = DoctorSpecialization.entries.toTypedArray()
                        val selectedSpec = specializationOptions.find { it.displayName == specialization || it.name == specialization }
                            ?: DoctorSpecialization.CARDIOLOGY

                        ExposedDropdownMenuBox(
                            expanded = specializationExpanded,
                            onExpandedChange = { specializationExpanded = !specializationExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedSpec.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Specialization") },
                                leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specializationExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = specializationExpanded,
                                onDismissRequest = { specializationExpanded = false }
                            ) {
                                specializationOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayName) },
                                        onClick = {
                                            specialization = option.displayName
                                            specializationExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = experience,
                            onValueChange = { experience = it },
                            label = { Text("Experience (years)") },
                            leadingIcon = { Icon(Icons.Default.Work, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = education,
                            onValueChange = { education = it },
                            label = { Text("Education") },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Contact Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (success) {
                        Text(
                            text = "Changes saved successfully!",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (error != null) {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            isSaving = true
                            success = false
                            error = null
                            val timestamp = try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val date = sdf.parse(dob)
                                if (date != null) Timestamp(date) else null
                            } catch (_: Exception) { null }

                            if (type == "user") {
                                val updateMap = mutableMapOf<String, Any>(
                                    "name" to name,
                                    "surname" to surname,
                                    "gender" to gender,
                                    "phone" to phone,
                                    "email" to email,
                                    "address" to address
                                )
                                if (timestamp != null) updateMap["dob"] = timestamp
                                firestore.collection("users").document(id).update(updateMap)
                                    .addOnSuccessListener {
                                        isSaving = false
                                        success = true
                                    }
                                    .addOnFailureListener {
                                        isSaving = false
                                        error = it.message
                                    }
                            } else if (type == "doctor") {
                                val updateMap = mutableMapOf<String, Any>(
                                    "name" to name,
                                    "surname" to surname,
                                    "gender" to gender,
                                    "phone" to phone,
                                    "e-mail" to email,
                                    "email" to email,
                                    "address" to address,
                                    "specialization" to (DoctorSpecialization.fromDisplayName(specialization)?.name ?: specialization),
                                    "experience" to experience,
                                    "education" to education
                                )
                                if (timestamp != null) updateMap["dob"] = timestamp
                                firestore.collection("doctors").document(id).update(updateMap)
                                    .addOnSuccessListener {
                                        isSaving = false
                                        success = true
                                    }
                                    .addOnFailureListener {
                                        isSaving = false
                                        error = it.message
                                    }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving,
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
                                text = "Save Changes",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}