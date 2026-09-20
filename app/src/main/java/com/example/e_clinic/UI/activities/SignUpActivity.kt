package com.example.e_clinic.UI.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.User
import com.example.e_clinic.R
import com.example.e_clinic.UI.theme.EClinicTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EClinicTheme {
                RegistrationScreen(onSignUpSuccess = {
                    startActivity(Intent(this, LogInActivity::class.java))
                    finish()
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(onSignUpSuccess: () -> Unit = {}, preFilledEmail: String? = null) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(preFilledEmail ?: "") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("Select Date of Birth") }
    var selectedGender by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val calendar = remember { Calendar.getInstance() }
    var expanded by remember { mutableStateOf(false) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dob = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val googleSignUpLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account.email ?: return@rememberLauncherForActivityResult

                auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                        auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                                val userRef = db.collection("users").document(userId)

                                userRef.get().addOnSuccessListener { document ->
                                    if (!document.exists()) {
                                        val newUser = User(
                                            id = userId,
                                            email = email,
                                            name = account.displayName.orEmpty(),
                                            surname = "",
                                            phone = "",
                                            dob = null,
                                            gender = "",
                                            address = ""
                                        )
                                        userRef.set(newUser).addOnSuccessListener {
                                            onSignUpSuccess()
                                        }.addOnFailureListener {
                                            errorMessage = "Firestore error: ${it.message}"
                                        }
                                    } else {
                                        successMessage = "Welcome back!"
                                        onSignUpSuccess()
                                    }
                                }
                            } else {
                                errorMessage = "Google Sign-In Failed: ${authTask.exception?.message}"
                            }
                        }
                    } else {
                        errorMessage = task.exception?.message ?: "Error checking email."
                    }
                }
            } catch (e: ApiException) {
                errorMessage = "Google Sign-In Failed: ${e.message}"
            }
        }
    }

    fun signUpWithGoogle() {
        val googleSignInClient = GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )
        googleSignInClient.signOut()
        val signInIntent = googleSignInClient.signInIntent
        val pendingIntent = PendingIntent.getActivity(context, 0, signInIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        googleSignUpLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
    }

    fun registerUser() {
        if (name.isEmpty() || surname.isEmpty() || email.isEmpty() || phone.isEmpty() || dob == "Select Date of Birth" || selectedGender.isEmpty()) {
            errorMessage = "All fields must be filled!"
            return
        }
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match!"
            return
        }

        auth.createUserWithEmailAndPassword(email.trim(), password.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result.user
                    firebaseUser?.sendEmailVerification()

                    val userId = firebaseUser?.uid ?: return@addOnCompleteListener

                    val user = User(
                        id = userId,
                        email = email.trim(),
                        name = name,
                        surname = surname,
                        phone = phone,
                        dob = parseDobToTimestamp(dob),
                        gender = selectedGender,
                        address = address,
                    )

                    db.collection("users").document(userId).set(user)
                        .addOnSuccessListener {
                            onSignUpSuccess()
                        }
                        .addOnFailureListener {
                            errorMessage = "Error saving user: ${it.message}"
                        }
                } else {
                    errorMessage = task.exception?.message ?: "Registration failed."
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .widthIn(max = 480.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Create Patient Account",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("First Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
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

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = if (selectedGender.isEmpty()) "Gender" else selectedGender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            listOf("Male", "Female").forEach { gender ->
                                DropdownMenuItem(
                                    text = { Text(gender) },
                                    onClick = {
                                        selectedGender = gender
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = if (dob == "Select Date of Birth") "" else dob,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date of Birth") },
                        placeholder = { Text("DD/MM/YYYY") },
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { datePickerDialog.show() }
                    )
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { registerUser() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Register", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }

                IconButton(
                    onClick = { signUpWithGoogle() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.google_sign_up),
                        contentDescription = "Google Sign-Up",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(200.dp)
                    )
                }
            }
        }
    }
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(errorMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
    if (successMessage != null) {
        AlertDialog(
            onDismissRequest = { successMessage = null },
            title = { Text("Success") },
            text = { Text(successMessage ?: "") },
            confirmButton = {
                TextButton(onClick = {
                    successMessage = null
                    onSignUpSuccess()
                }) {
                    Text("OK")
                }
            }
        )
    }
}

fun parseDobToTimestamp(dob: String): Timestamp? {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = sdf.parse(dob)
        if (date != null) Timestamp(date) else null
    } catch (e: Exception) {
        null
    }
}
