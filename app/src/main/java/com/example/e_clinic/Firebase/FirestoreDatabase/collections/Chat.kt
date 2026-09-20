package com.example.e_clinic.Firebase.FirestoreDatabase.collections

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Chat(
    @DocumentId
    val id: String = "",
    val participants: List<String> = emptyList(),
    val user_id: String = "",
    val doctor_id: String = "",
    val user_name: String = "",
    val doctor_name: String = "",
    val user_avatar: String = "",
    val doctor_avatar: String = "",
    val last_message: String = "",
    val last_message_time: Timestamp? = null,
    val last_sender_id: String = "",
    val created_at: Timestamp? = null
)
