package com.example.ui.theme

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

// 🎨 Palette Gelap (Menyesuaikan dengan ThemeColors Neo Minesweeper)
private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF00E5FF),      // Primary Accent Cyan
  secondary = Color(0xFF3B82F6),    // Secondary Steel/Blue
  tertiary = Color(0xFF10B981),     // Success Emerald
  background = Color(0xFF0F172A),   // Dark Navy Background
  surface = Color(0xFF1E293B),      // Card / Dialog Background
  onPrimary = Color.Black,
  onSecondary = Color.White,
  onTertiary = Color.White,
  onBackground = Color(0xFFF8FAFC), // Text Primary Light
  onSurface = Color(0xFFF8FAFC)
)

// 🎨 Palette Terang (Fallback jika dipakai di mode terang)
private val LightColorScheme = lightColorScheme(
  primary = Color(0xFF0284C7),
  secondary = Color(0xFF2563EB),
  tertiary = Color(0xFF059669),
  background = Color(0xFFF8FAFC),
  surface = Color(0xFFFFFFFF),
  onPrimary = Color.White,
  onSecondary = Color.White,
  onTertiary = Color.White,
  onBackground = Color(0xFF0F172A),
  onSurface = Color(0xFF0F172A)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // 💡 Di-set FALSE agar warna game selalu konsisten & tidak terpengaruh wallpaper HP (Android 12+)
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> DarkColorScheme // 🕹️ Game default pakai tema gelap agar gaya Cyberpunk/Neon makin mantap!
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}