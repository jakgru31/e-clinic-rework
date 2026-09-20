package com.example.e_clinic.UI.activities.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.ChatMessage
import com.example.e_clinic.Firebase.Repositories.ChatRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    currentUserId: String,
    otherUserId: String,
    otherUserName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val chatRepository = remember { ChatRepository() }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var enlargedImageUrl by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    // Listen to messages in real time
    DisposableEffect(chatId) {
        if (chatId.isBlank()) return@DisposableEffect onDispose {}
        val registration = chatRepository.listenToMessages(chatId) { updatedMessages ->
            messages = updatedMessages
        }
        onDispose {
            registration.remove()
        }
    }

    // Auto-scroll to latest message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Image Picker Launcher with 5MB validation
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val pfd = try {
                context.contentResolver.openFileDescriptor(uri, "r")
            } catch (e: Exception) {
                null
            }
            val fileSize = pfd?.statSize ?: 0L
            pfd?.close()

            if (fileSize > 5 * 1024 * 1024) {
                Toast.makeText(
                    context,
                    "Image size exceeds 5MB limit. Please choose a smaller image.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                selectedImageUri = uri
            }
        }
    }

    var currentUserName by remember {
        val user = FirebaseAuth.getInstance().currentUser
        mutableStateOf(
            user?.displayName?.takeIf { it.isNotBlank() }
                ?: user?.email?.substringBefore("@")
                ?: "User"
        )
    }

    // Resolve actual First Name and Last Name from Firestore
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("doctors").document(currentUserId).get().addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val name = doc.getString("name") ?: ""
                    val surname = doc.getString("surname") ?: ""
                    val fullName = "Dr. $name $surname".trim()
                    if (fullName.isNotBlank() && fullName != "Dr.") {
                        currentUserName = fullName
                    }
                } else {
                    db.collection("users").document(currentUserId).get().addOnSuccessListener { userDoc ->
                        if (userDoc != null && userDoc.exists()) {
                            val name = userDoc.getString("name") ?: ""
                            val surname = userDoc.getString("surname") ?: ""
                            val fullName = "$name $surname".trim()
                            if (fullName.isNotBlank()) {
                                currentUserName = fullName
                            }
                        }
                    }
                }
            }
        }
    }

    fun handleSend() {
        val textToSend = inputText.trim()
        val imageToSend = selectedImageUri

        if (textToSend.isEmpty() && imageToSend == null) return

        if (imageToSend != null) {
            isUploading = true
            chatRepository.uploadImage(
                chatId = chatId,
                imageUri = imageToSend,
                context = context,
                onProgress = { progress -> uploadProgress = progress },
                onSuccess = { downloadUrl ->
                    chatRepository.sendMessage(
                        chatId = chatId,
                        senderId = currentUserId,
                        senderName = currentUserName,
                        text = textToSend,
                        imageUrl = downloadUrl
                    ) {
                        isUploading = false
                        selectedImageUri = null
                        inputText = ""
                    }
                },
                onFailure = { error ->
                    isUploading = false
                    Toast.makeText(context, error.message ?: "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            chatRepository.sendMessage(
                chatId = chatId,
                senderId = currentUserId,
                senderName = currentUserName,
                text = textToSend,
                imageUrl = null
            ) {
                inputText = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = otherUserName.ifBlank { "Chat" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Secure Consultation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    val isCurrentUser = message.sender_id == currentUserId
                    ChatMessageBubble(
                        message = message,
                        isCurrentUser = isCurrentUser,
                        onImageClick = { url -> enlargedImageUrl = url }
                    )
                }
            }

            // Image Preview (if image selected before sending)
            if (selectedImageUri != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected image preview",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        if (isUploading) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Uploading image...", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { uploadProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            Text(
                                text = "Image attached (< 5MB)",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { selectedImageUri = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove image")
                            }
                        }
                    }
                }
            }

            // Bottom Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !isUploading
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        placeholder = { Text("Type a message...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        enabled = !isUploading
                    )

                    IconButton(
                        onClick = { handleSend() },
                        enabled = !isUploading && (inputText.isNotBlank() || selectedImageUri != null)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank() || selectedImageUri != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Image Dialog
    if (enlargedImageUrl != null) {
        Dialog(
            onDismissRequest = { enlargedImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = enlargedImageUrl,
                    contentDescription = "Full image view",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { enlargedImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean,
    onImageClick: (String) -> Unit
) {
    val bubbleShape = if (isCurrentUser) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    val bubbleColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = bubbleShape,
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Image attachment if present
                if (!message.image_url.isNullOrBlank()) {
                    AsyncImage(
                        model = message.image_url,
                        contentDescription = "Sent image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onImageClick(message.image_url) },
                        contentScale = ContentScale.Crop
                    )
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatMessageTime(message.timestamp),
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

private fun formatMessageTime(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestamp.toDate())
}
