package com.example.e_clinic.Firebase.CloudMessaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.e_clinic.R
import com.example.e_clinic.UI.activities.doctor_screens.doctor_activity.DoctorActivity
import com.example.e_clinic.UI.activities.user_screens.user_activity.UserActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        when (remoteMessage.data["type"]) {
            "chat_message" -> handleChatMessage(remoteMessage.data)
            "finish_reminder" -> handleFinishReminder(remoteMessage.data)
            "cancel_notification" -> handleCancelNotification(remoteMessage.data)
            "user_appointment_reminder" -> handleUserAppointmentReminder(remoteMessage.data)
            else -> createNotification(remoteMessage)
        }
    }

    private fun handleChatMessage(data: Map<String, String>) {
        val chatId = data["chatId"] ?: return
        val senderName = data["senderName"] ?: "New Message"
        val text = data["text"] ?: "You received a new message"
        val isDoctor = data["isDoctor"]?.toBoolean() ?: false
        val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()

        val channelId = "chat_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming chat messages from doctors and patients"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val targetActivity = if (isDoctor) DoctorActivity::class.java else UserActivity::class.java
        val intent = Intent(this, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_chat_id", chatId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(senderName)
            .setContentText(text)
            .setWhen(timestamp)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(chatId.hashCode(), notification)
    }

    private fun handleUserAppointmentReminder(data: Map<String, String>) {
        val appointmentId = data["appointmentId"] ?: return
        val dayType = data["dayType"] ?: "Today"
        val timestamp = data["appointmentTimestamp"]?.toLongOrNull()
        val appointmentDate = if (timestamp != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            sdf.format(Date(timestamp))
        } else {
            data["appointmentDate"] ?: "Unknown time"
        }

        val channelId = "appointment_reminders"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🩺 Reminder: $dayType Appointment")
            .setContentText("Your appointment is scheduled on $appointmentDate")
            .setWhen(timestamp ?: System.currentTimeMillis())
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(appointmentId.hashCode(), notification)
    }


    private fun handleFinishReminder(data: Map<String, String>) {
        val appointmentId = data["appointmentId"] ?: return

        val intent = Intent(this, DoctorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("showFinishDialog", true)
            putExtra("appointmentId", appointmentId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "appointment_reminders"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\uD83D\uDCCB✍\uFE0F✅\uD83D\uDE0E\uD83D\uDCCB\uD83D\uDD25 Appointment Not Finished")
            .setContentText("Tap to finish the appointment.")
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(appointmentId.hashCode(), notification)
    }

    private fun handleCancelNotification(data: Map<String, String>) {
        val appointmentId = data["appointmentId"] ?: return
        val patientName = data["patientName"] ?: "Unknown"
        val timestamp = data["appointmentTimestamp"]?.toLongOrNull()
        val appointmentDate = if (timestamp != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getDefault()
            }
            sdf.format(Date(timestamp))
        } else {
            data["appointmentDate"] ?: "Unknown time"
        }

        val intent = Intent(this, DoctorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("appointmentId", appointmentId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "appointment_reminders"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\uD83C\uDF7E \uD83D\uDED1\uD83E\uDE7A\uD83D\uDE0CAppointment Canceled")
            .setContentText("Patient: $patientName\nDate: $appointmentDate")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Patient: $patientName\nDate: $appointmentDate"))
            .setWhen(timestamp ?: System.currentTimeMillis())
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(appointmentId.hashCode(), notification)
    }

    private fun createNotification(remoteMessage: RemoteMessage) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "appointment_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Appointment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(remoteMessage.notification?.title ?: "Appointment Reminder")
            .setContentText(remoteMessage.notification?.body ?: "You have an appointment reminder")
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
