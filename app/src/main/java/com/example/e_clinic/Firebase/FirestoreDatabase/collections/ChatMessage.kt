package com.example.e_clinic.Firebase.FirestoreDatabase.collections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ChatMessage(
    @DocumentId
    val id: String = "",
    val sender_id: String = "",
    val sender_name: String = "",
    val text: String = "",
    val image_url: String? = null,
    val timestamp: Timestamp? = null,
    val status: String = "sent"
)
