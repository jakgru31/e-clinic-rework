package com.example.e_clinic.UI.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.e_clinic.UI.theme.EClinicDarkTheme
import com.example.e_clinic.UI.theme.EClinicLightTheme


// Base color definitions for clinic brand
val DarkBlue = Color(0xFF0D1B2A)
val Blue = Color(0xFF006A60)
val LightBlue = Color(0xFF74F8E5)
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)

@Composable
fun EClinicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Default to false for unified brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) EClinicDarkTheme else EClinicLightTheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
