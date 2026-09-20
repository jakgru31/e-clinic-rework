package com.example.e_clinic.Firebase.Repositories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Chat
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.ChatMessage
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.Doctor
import com.example.e_clinic.Firebase.FirestoreDatabase.collections.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream
import java.util.UUID

import com.google.firebase.firestore.DocumentReference
import android.util.Log

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val chatsCollection = db.collection("chats")

    fun getChatId(userId: String, doctorId: String): String {
        return "${userId}_${doctorId}"
    }

    fun getOrCreateChat(
        userId: String,
        doctorId: String,
        onComplete: (Chat?) -> Unit
    ) {
        val chatId = getChatId(userId, doctorId)
        val chatRef = chatsCollection.document(chatId)

        chatRef.get().addOnSuccessListener { doc ->
            if (doc != null && doc.exists()) {
                val chat = doc.toObject(Chat::class.java)
                onComplete(chat)
            } else {
                createNewChat(chatRef, chatId, userId, doctorId, onComplete)
            }
        }.addOnFailureListener { e ->
            Log.w("ChatRepository", "Chat doc does not exist or read failed: ${e.message}. Attempting create.")
            createNewChat(chatRef, chatId, userId, doctorId, onComplete)
        }
    }

    private fun createNewChat(
        chatRef: DocumentReference,
        chatId: String,
        userId: String,
        doctorId: String,
        onComplete: (Chat?) -> Unit
    ) {
        db.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val user = userDoc?.toObject(User::class.java)
            val userName = if (user != null) "${user.name} ${user.surname}".trim() else "Patient"
            val userAvatar = user?.profilePicture ?: ""

            db.collection("doctors").document(doctorId).get().addOnSuccessListener { docDoc ->
                val doctor = docDoc?.toObject(Doctor::class.java)
                val doctorName = if (doctor != null) "Dr. ${doctor.name} ${doctor.surname}".trim() else "Doctor"
                val doctorAvatar = doctor?.profilePicture ?: ""

                createChatWithFallback(chatRef, chatId, userId, doctorId, userName, userAvatar, doctorName, doctorAvatar, onComplete)
            }.addOnFailureListener {
                createChatWithFallback(chatRef, chatId, userId, doctorId, userName, userAvatar, "Doctor", "", onComplete)
            }
        }.addOnFailureListener {
            createChatWithFallback(chatRef, chatId, userId, doctorId, "Patient", "", "Doctor", "", onComplete)
        }
    }

    private fun createChatWithFallback(
        chatRef: DocumentReference,
        chatId: String,
        userId: String,
        doctorId: String,
        userName: String,
        userAvatar: String,
        doctorName: String,
        doctorAvatar: String,
        onComplete: (Chat?) -> Unit
    ) {
        val newChat = Chat(
            id = chatId,
            participants = listOf(userId, doctorId),
            user_id = userId,
            doctor_id = doctorId,
            user_name = userName.ifBlank { "Patient" },
            doctor_name = doctorName.ifBlank { "Doctor" },
            user_avatar = userAvatar,
            doctor_avatar = doctorAvatar,
            last_message = "",
            last_message_time = Timestamp.now(),
            last_sender_id = "",
            created_at = Timestamp.now()
        )

        chatRef.set(newChat).addOnSuccessListener {
            onComplete(newChat)
        }.addOnFailureListener { e ->
            Log.e("ChatRepository", "Failed to set new chat document", e)
            onComplete(null)
        }
    }

    fun listenToChats(
        currentUserId: String,
        onUpdate: (List<Chat>) -> Unit
    ): ListenerRegistration {
        return chatsCollection
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val chats = snapshot?.toObjects(Chat::class.java)
                    ?.sortedByDescending { it.last_message_time?.seconds ?: 0 }
                    ?: emptyList()
                onUpdate(chats)
            }
    }

    fun listenToMessages(
        chatId: String,
        onUpdate: (List<ChatMessage>) -> Unit
    ): ListenerRegistration {
        return chatsCollection.document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                onUpdate(messages)
            }
    }

    fun sendMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        text: String,
        imageUrl: String? = null,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val message = ChatMessage(
            sender_id = senderId,
            sender_name = senderName,
            text = text,
            image_url = imageUrl,
            timestamp = Timestamp.now(),
            status = "sent"
        )

        val messagesRef = chatsCollection.document(chatId).collection("messages")
        messagesRef.add(message).addOnSuccessListener {
            val previewText = if (text.isNotBlank()) text else "📷 Photo"
            chatsCollection.document(chatId).update(
                mapOf(
                    "last_message" to previewText,
                    "last_message_time" to Timestamp.now(),
                    "last_sender_id" to senderId
                )
            ).addOnCompleteListener {
                onComplete(true)
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun uploadImage(
        chatId: String,
        imageUri: Uri,
        context: Context,
        onProgress: (Float) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            // Check file size limit (5MB = 5 * 1024 * 1024 bytes)
            val pfd = context.contentResolver.openFileDescriptor(imageUri, "r")
            val fileSize = pfd?.statSize ?: 0L
            pfd?.close()

            if (fileSize > 5 * 1024 * 1024) {
                onFailure(Exception("Image size exceeds the 5MB limit. Please choose a smaller image."))
                return
            }

            // Compress image to JPEG to optimize bandwidth and speed
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                onFailure(Exception("Failed to decode selected image."))
                return
            }

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            val data = baos.toByteArray()

            val fileName = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference.child("chat_attachments/$chatId/$fileName")
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            val uploadTask = storageRef.putBytes(data, metadata)
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = if (taskSnapshot.totalByteCount > 0) {
                    taskSnapshot.bytesTransferred.toFloat() / taskSnapshot.totalByteCount.toFloat()
                } else 0f
                onProgress(progress)
            }.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    onSuccess(downloadUri.toString())
                }.addOnFailureListener { e ->
                    onFailure(e)
                }
            }.addOnFailureListener { e ->
                onFailure(e)
            }
        } catch (e: Exception) {
            onFailure(e)
        }
    }
}
