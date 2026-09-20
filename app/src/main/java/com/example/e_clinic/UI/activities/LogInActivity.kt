package com.example.e_clinic.UI.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.e_clinic.R
import com.example.e_clinic.Services.PinManager
import com.example.e_clinic.UI.activities.admin_screens.AdminPinEntryActivity
import com.example.e_clinic.UI.activities.admin_screens.SetAdminPinAfterLoginActivity
import com.example.e_clinic.UI.activities.doctor_screens.DoctorPinEntryActivity
import com.example.e_clinic.UI.activities.doctor_screens.SetDoctorPinAfterLoginActivity
import com.example.e_clinic.UI.activities.user_screens.PinEntryActivity
import com.example.e_clinic.UI.activities.user_screens.SetPinAfterLoginActivity
import com.example.e_clinic.UI.theme.EClinicTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore


class LogInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EClinicTheme {
                LogInScreen(
                    onSignUpClick = {
                        startActivity(Intent(this, SignUpActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}


private fun navigateToPinEntry(
    context: android.content.Context,
    user: com.google.firebase.auth.FirebaseUser,
    pinManager: PinManager
) {
    val cachedRole = pinManager.getUserRole()
    if (cachedRole != null) {
        val intent = when (cachedRole) {
            "admin" -> Intent(context, AdminPinEntryActivity::class.java)
            "doctor" -> Intent(context, DoctorPinEntryActivity::class.java)
            else -> Intent(context, PinEntryActivity::class.java)
        }
        context.startActivity(intent)
        (context as? Activity)?.finish()
        return
    }

    val email = user.email ?: ""
    val db = FirebaseFirestore.getInstance()

    db.collection("administrators").whereEqualTo("email", email).get()
        .addOnSuccessListener { adminDocs ->
            if (!adminDocs.isEmpty) {
                pinManager.saveUserRole("admin")
                val intent = Intent(context, AdminPinEntryActivity::class.java)
                context.startActivity(intent)
                (context as? Activity)?.finish()
            } else {
                db.collection("doctors").whereEqualTo("e-mail", email).get()
                    .addOnSuccessListener { doctorDocs ->
                        if (!doctorDocs.isEmpty) {
                            pinManager.saveUserRole("doctor")
                            val intent = Intent(context, DoctorPinEntryActivity::class.java)
                            context.startActivity(intent)
                            (context as? Activity)?.finish()
                        } else {
                            db.collection("doctors").whereEqualTo("email", email).get()
                                .addOnSuccessListener { doctorDocs2 ->
                                    if (!doctorDocs2.isEmpty) {
                                        pinManager.saveUserRole("doctor")
                                        val intent = Intent(context, DoctorPinEntryActivity::class.java)
                                        context.startActivity(intent)
                                        (context as? Activity)?.finish()
                                    } else {
                                        pinManager.saveUserRole("user")
                                        val intent = Intent(context, PinEntryActivity::class.java)
                                        context.startActivity(intent)
                                        (context as? Activity)?.finish()
                                    }
                                }
                                .addOnFailureListener {
                                    pinManager.saveUserRole("user")
                                    val intent = Intent(context, PinEntryActivity::class.java)
                                    context.startActivity(intent)
                                    (context as? Activity)?.finish()
                                }
                        }
                    }
                    .addOnFailureListener {
                        pinManager.saveUserRole("user")
                        val intent = Intent(context, PinEntryActivity::class.java)
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    }
            }
        }
        .addOnFailureListener {
            pinManager.saveUserRole("user")
            val intent = Intent(context, PinEntryActivity::class.java)
            context.startActivity(intent)
            (context as? Activity)?.finish()
        }
}

@Composable
fun LogInScreen(
    onSignUpClick: () -> Unit,
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var isCheckingAuth by remember { mutableStateOf(auth.currentUser == null) }
    val pinManager = remember { PinManager(context) }
    var isLoading by remember { mutableStateOf(false) }
    var showResetPassword by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showPinEntry by remember { mutableStateOf(false) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            currentUser = fbAuth.currentUser
            isCheckingAuth = false
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    if (isCheckingAuth) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (currentUser != null && pinManager.getPin() != null) {
        if (showPinEntry) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 400.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "eClinic",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Welcome Back",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        Button(
                            onClick = {
                                showPinEntry = true
                                navigateToPinEntry(context, currentUser!!, pinManager)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Continue")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                auth.signOut()
                                pinManager.clearPin()
                                context.startActivity(Intent(context, LogInActivity::class.java))
                                (context as? ComponentActivity)?.finish()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Sign Out", color = Color.White)
                        }
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
        return
    }
    else {
        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val credential = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    val account = credential.getResult(ApiException::class.java)
                    val authCredential = GoogleAuthProvider.getCredential(account.idToken, null)

                    auth.signInWithCredential(authCredential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                if (user != null) {
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(user.uid).get()
                                        .addOnSuccessListener { doc ->
                                            pinManager.saveUserRole("user")
                                            val intent = if (doc.getBoolean("hasSetPin") == true) {
                                                Intent(context, PinEntryActivity::class.java)
                                            } else {
                                                Intent(
                                                    context,
                                                    SetPinAfterLoginActivity::class.java
                                                )
                                            }
                                            context.startActivity(intent)
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                context,
                                                "Fetching User Data Failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            } else {
                                Toast.makeText(context, "Google Sign-In Failed", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                }
            }


        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxSize(
                ),
                contentAlignment = Alignment.Center,

                ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .widthIn(min = 320.dp, max = 400.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {


                        Text(
                            text = "Log in to eClinic",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

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

                        Spacer(modifier = Modifier.height(24.dp))
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

                        Button(
                            onClick = {
                                if (email.isEmpty() || password.isEmpty()) {
                                    errorMessage = "Please fill all fields"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            val user = auth.currentUser
                                            if (user != null) {
                                                val userEmail = user.email ?: ""
                                                val db = FirebaseFirestore.getInstance()
                                                val pinManager = PinManager(context)

                                                // Check administrators by email
                                                db.collection("administrators")
                                                    .whereEqualTo("email", userEmail)
                                                    .get()
                                                    .addOnSuccessListener { adminDocs ->
                                                        if (!adminDocs.isEmpty) {
                                                            pinManager.saveUserRole("admin")
                                                            val intent = if (pinManager.getPin() != null) {
                                                                Intent(context, AdminPinEntryActivity::class.java)
                                                            } else {
                                                                Intent(context, SetAdminPinAfterLoginActivity::class.java)
                                                            }
                                                            context.startActivity(intent)
                                                            (context as? Activity)?.finish()
                                                        } else {
                                                            // Check doctors by email (try both "e-mail" and "email")
                                                            db.collection("doctors")
                                                                .whereEqualTo("e-mail", userEmail)
                                                                .get()
                                                                .addOnSuccessListener { doctorDocs ->
                                                                    if (!doctorDocs.isEmpty) {
                                                                        pinManager.saveUserRole("doctor")
                                                                        val intent = if (pinManager.getPin() != null) {
                                                                            Intent(context, DoctorPinEntryActivity::class.java)
                                                                        } else {
                                                                            Intent(context, SetDoctorPinAfterLoginActivity::class.java)
                                                                        }
                                                                        context.startActivity(intent)
                                                                        (context as? Activity)?.finish()
                                                                    } else {
                                                                        // Try "email" field if "e-mail" not found
                                                                        db.collection("doctors")
                                                                            .whereEqualTo("email", userEmail)
                                                                            .get()
                                                                            .addOnSuccessListener { doctorDocs2 ->
                                                                                if (!doctorDocs2.isEmpty) {
                                                                                    pinManager.saveUserRole("doctor")
                                                                                    val intent = if (pinManager.getPin() != null) {
                                                                                        Intent(context, DoctorPinEntryActivity::class.java)
                                                                                    } else {
                                                                                        Intent(context, SetDoctorPinAfterLoginActivity::class.java)
                                                                                    }
                                                                                    context.startActivity(intent)
                                                                                    (context as? Activity)?.finish()
                                                                                } else {
                                                                                    // Default: User/Patient
                                                                                    pinManager.saveUserRole("user")
                                                                                    val intent = if (pinManager.getPin() != null) {
                                                                                        Intent(context, PinEntryActivity::class.java)
                                                                                    } else {
                                                                                        Intent(context, SetPinAfterLoginActivity::class.java)
                                                                                    }
                                                                                    context.startActivity(intent)
                                                                                    (context as? Activity)?.finish()
                                                                                }
                                                                            }
                                                                            .addOnFailureListener {
                                                                                errorMessage = "Failed to check roles"
                                                                            }
                                                                    }
                                                                }
                                                                .addOnFailureListener {
                                                                    errorMessage = "Failed to check roles"
                                                                }
                                                        }
                                                    }
                                                    .addOnFailureListener {
                                                        errorMessage = "Failed to check roles"
                                                    }
                                            } else {
                                                auth.signOut()
                                                errorMessage = "Email is not verified. Please check your inbox."
                                            }
                                        } else {
                                            errorMessage = "Incorrect email or password"
                                        }
                                    }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = "Log In",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onSignUpClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Text(
                                text = "Sign Up",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        TextButton(
                            onClick = { showResetPassword = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Forgot password?")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        IconButton(
                            onClick = {
                                val googleSignInClient = GoogleSignIn.getClient(
                                    context,
                                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                        .requestIdToken(context.getString(R.string.default_web_client_id))
                                        .requestEmail()
                                        .build()
                                )

                                googleSignInClient.signOut()

                                val signInIntent = googleSignInClient.signInIntent
                                val pendingIntent = PendingIntent.getActivity(
                                    context, 0, signInIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                                )
                                launcher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                            },
                            modifier = Modifier.size(200.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.google_sign_in),
                                contentDescription = "Google Sign-In",
                                tint = Color.Unspecified,
                                modifier = Modifier.fillMaxSize()
                            )
                        }


                    }
                }
            }
        }
            }

            if (showResetPassword) {
                ResetPasswordScreen(
                    onDismiss = { showResetPassword = false },
                )
            }
}
