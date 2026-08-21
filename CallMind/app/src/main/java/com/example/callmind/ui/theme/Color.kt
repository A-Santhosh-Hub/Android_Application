package com.example.callmind.ui.theme

import androidx.compose.ui.graphics.Color

// Dark Theme Palette - Deep Black
val DarkBackground = Color(0xFF000000) // Pure Black
val DarkSurface = Color(0xFF121212) // Dark Grey for Cards
val DarkSurfaceVariant = Color(0xFF1E1E1E) // Surface for buttons

// Light Theme Palette - Pure White
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF5F5F5)
val LightSurfaceVariant = Color(0xFFEEEEEE)

// Primary Gradients (Work well on both)
val PurpleGradientStart = Color(0xFF9D50BB)
val PurpleGradientEnd = Color(0xFF6E48AA)
val BlueGradientStart = Color(0xFF4361EE)
val BlueGradientEnd = Color(0xFF4895EF)

// Accent Colors
val AccentGreen = Color(0xFF4CAF50)
val AccentWhatsApp = Color(0xFF25D366)
val AccentTelegram = Color(0xFF0088CC)
val AccentSchedule = Color(0xFFF9A825)
val AccentNotes = Color(0xFF7B1FA2)
val AccentRed = Color(0xFFFF5252)

// Text Colors - Dark
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFFB0B0B0)

// Text Colors - Light
val LightTextPrimary = Color(0xFF000000)
val LightTextSecondary = Color(0xFF757575)

// Status
val OnlineStatus = Color(0xFF4CAF50)

// Standard Material Fallbacks
val IndigoPrimary = Color(0xFF4361EE)
val IndigoSecondary = Color(0xFF3F37C9)
val IndigoTertiary = Color(0xFF4895EF)

// Legacy compatibility (mapping to new names to avoid immediate breaks if missed)
val BackgroundMain = DarkBackground
val SurfaceCard = DarkSurface
val SurfaceAction = DarkSurfaceVariant
val TextPrimary = DarkTextPrimary
val TextSecondary = DarkTextSecondary
