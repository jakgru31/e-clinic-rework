package com.example.e_clinic.AIAssistant

import android.util.Log
import com.example.e_clinic.BuildConfig

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlin.random.Random
import java.util.UUID

private const val TAG = "HealthTipGenerator"
private var currentCachedTip: String = "Stay hydrated: Drink water throughout the day for better energy."

fun getInitialTip(): String = currentCachedTip

suspend fun getDailyHealthTip(): String {
    if (BuildConfig.GEMINI_API_KEY.isBlank()) {
        return getFallbackTip()
    }

    val category = getRotatingCategory()
    val prompt = "Provide exactly ONE concise, actionable daily health tip (under 15 words) about $category. Plain text only, no quotes, no bullets."

    return try {
        val model = GenerativeModel(
            modelName = "gemini-3.5-flash-lite",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        val response = model.generateContent(prompt)
        val rawTip = response.text?.trim() ?: throw Exception("Empty API response")

        val cleanTip = rawTip
            .replace(Regex("^[\"']|[\"']$"), "")
            .replace(Regex("^[-•*]\\s*"), "")
            .trim()

        if (cleanTip.isNotBlank()) {
            currentCachedTip = cleanTip
            Log.d(TAG, "New tip generated: $cleanTip")
            cleanTip
        } else {
            getFallbackTip()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Tip generation failed: ${e.message}")
        getFallbackTip()
    }
}

private fun getRotatingCategory(): String {
    val categories = listOf(
        "physical activity",
        "nutrition",
        "mental health",
        "hygiene",
        "preventive care",
        "chronic condition management"
    )
    return categories.random()
}

private fun getFallbackTip(): String {
    val fallbackTips = listOf(
        "Take a 5-minute walk every hour",
        "Choose whole grains over refined carbs",
        "Practice deep breathing for stress relief",
        "Clean your phone screen daily",
        "Get regular health check-ups",
        "Manage diabetes with balanced meals",
        "Stretch your neck and shoulders",
        "Limit processed food intake",
        "Wear sunscreen when outdoors",
        "Stay socially connected for mental health"
    )
    return fallbackTips.random()
}