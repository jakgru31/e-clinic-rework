@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.e_clinic.UI.activities.admin_screens.admin_activity

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
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
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.User
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun UsersScreen() {
    val users = remember { mutableStateListOf<User>() }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshUsers() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").get().addOnSuccessListener { snapshot ->
            users.clear()
            for (document in snapshot.documents) {
                val user = User(
                    id = document.id,
                    name = document.getString("name") ?: "Unknown",
                    surname = document.getString("surname") ?: "Unknown",
                    phone = document.getString("phone") ?: "Unknown",
                    email = document.getString("email") ?: "Unknown",
                    gender = document.getString("gender") ?: "Unknown",
                    address = document.getString("address") ?: "Unknown",
                    profilePicture = document.getString("profilePicture") ?: "",
                )
                users.add(user)
            }
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshUsers()
    }

    if (showDialog && selectedUser != null) {
        DataManagerScreen(
            id = selectedUser!!.id,
            type = "user",
            onBack = {
                showDialog = false
                refreshUsers()
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
                        text = "Manage Patients (${users.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                if (users.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No registered patients found.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(users) { user ->
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (!user.profilePicture.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = user.profilePicture,
                                            contentDescription = "User Avatar",
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.size(60.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = "User",
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${user.name} ${user.surname} (${user.gender})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Email: ${user.email}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Phone: ${user.phone}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Address: ${user.address}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { rotationY = 180f },
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Actions: ${user.name} ${user.surname}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Button(
                                        onClick = {
                                            selectedUser = user
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
                                        Text("Modify Patient Data")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            selectedUser = user
                                            showDeleteDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text("Remove Patient", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete patient ${selectedUser!!.name} ${selectedUser!!.surname}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        FirebaseFirestore.getInstance().collection("users").document(selectedUser!!.id).delete()
                        users.remove(selectedUser)
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
