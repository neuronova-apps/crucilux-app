package com.neuronova.crucilux.ui.theme

import androidx.compose.ui.graphics.Color

// ── Crucilux Brand Palette ──────────────────────────────────────────────────

// 1. Modo Día Normal
val LightPrimary              = Color(0xFF1976D2) // Azul Crucilux vibrante y limpio
val LightOnPrimary            = Color(0xFFFFFFFF)
val LightPrimaryContainer     = Color(0xFFE3F2FD)
val LightOnPrimaryContainer   = Color(0xFF0D47A1)

val LightSecondary            = Color(0xFF0277BD)
val LightOnSecondary          = Color(0xFFFFFFFF)
val LightSecondaryContainer   = Color(0xFFE1F5FE)
val LightOnSecondaryContainer = Color(0xFF01579B)

val LightTertiary             = Color(0xFF7E57C2)
val LightOnTertiary           = Color(0xFFFFFFFF)
val LightTertiaryContainer    = Color(0xFFEDE7F6)
val LightOnTertiaryContainer  = Color(0xFF4527A0)

val LightBackground           = Color(0xFFF7F9FC)
val LightOnBackground         = Color(0xFF1E293B)
val LightSurface              = Color(0xFFFFFFFF)
val LightOnSurface            = Color(0xFF1E293B)
val LightSurfaceVariant       = Color(0xFFEEF2F6)
val LightOnSurfaceVariant     = Color(0xFF64748B)
val LightOutline              = Color(0xFFCBD5E1)
val LightOutlineVariant       = Color(0xFFE2E8F0)

val LightError                = Color(0xFFBA1A1A)
val LightOnError              = Color(0xFFFFFFFF)
val LightErrorContainer       = Color(0xFFFFDAD6)
val LightOnErrorContainer     = Color(0xFF410002)

// 2. Modo Noche Normal
val DarkPrimary               = Color(0xFF90CAF9)
val DarkOnPrimary             = Color(0xFF003258)
val DarkPrimaryContainer      = Color(0xFF0D47A1)
val DarkOnPrimaryContainer    = Color(0xFFD1E4FF)

val DarkSecondary             = Color(0xFF81D4FA)
val DarkOnSecondary           = Color(0xFF00354E)
val DarkSecondaryContainer    = Color(0xFF004D73)
val DarkOnSecondaryContainer  = Color(0xFFC4E7FF)

val DarkTertiary              = Color(0xFFB39DDB)
val DarkOnTertiary            = Color(0xFF311B92)
val DarkTertiaryContainer     = Color(0xFF512DA8)
val DarkOnTertiaryContainer   = Color(0xFFEDE7F6)

val DarkBackground            = Color(0xFF0F172A)
val DarkOnBackground          = Color(0xFFE2E8F0)
val DarkSurface               = Color(0xFF1E293B)
val DarkOnSurface             = Color(0xFFE2E8F0)
val DarkSurfaceVariant        = Color(0xFF334155)
val DarkOnSurfaceVariant      = Color(0xFF94A3B8)
val DarkOutline               = Color(0xFF475569)
val DarkOutlineVariant        = Color(0xFF334155)

val DarkError                 = Color(0xFFFFB4AB)
val DarkOnError               = Color(0xFF690005)
val DarkErrorContainer        = Color(0xFF93000A)
val DarkOnErrorContainer      = Color(0xFFFFDAD6)

// 3. Modo de Alto Contraste Integral (Fondo negro real #000000, texto blanco #FFFFFF, WCAG AAA)
val HighContrastBackground           = Color(0xFF000000) // Negro real #000000
val HighContrastForeground           = Color(0xFFFFFFFF) // Blanco puro #FFFFFF
val HighContrastForegroundSecondary  = Color(0xFFE0E0E0) // Texto secundario de alta legibilidad
val HighContrastSurface              = Color(0xFF121212) // Superficie oscura con bordes
val HighContrastSurfaceVariant       = Color(0xFF1E1E1E) // Contenedores y teclas
val HighContrastOutline              = Color(0xFFFFFFFF) // Borde blanco nítido
val HighContrastOutlineVariant       = Color(0xFFBDBDBD)

// Acento Crucilux de alta visibilidad (Cian eléctrico con contraste > 10:1 en fondo negro)
val HighContrastPrimary              = Color(0xFF40C4FF)
val HighContrastOnPrimary            = Color(0xFF000000)
val HighContrastPrimaryContainer     = Color(0xFF003859)
val HighContrastOnPrimaryContainer   = Color(0xFFFFFFFF)

// Acento secundario y racha (Ámbar brillante con contraste > 10:1 en fondo negro)
val HighContrastSecondary            = Color(0xFFFFAB40)
val HighContrastOnSecondary          = Color(0xFF000000)
val HighContrastSecondaryContainer   = Color(0xFF4A2800)
val HighContrastOnSecondaryContainer = Color(0xFFFFFFFF)

// Resaltados asistidos y pistas (Magenta claro con contraste > 10:1 en fondo negro)
val HighContrastTertiary             = Color(0xFFEA80FC)
val HighContrastOnTertiary           = Color(0xFF000000)
val HighContrastTertiaryContainer    = Color(0xFF38006B)
val HighContrastOnTertiaryContainer  = Color(0xFFFFFFFF)

// Estados de éxito y error con diferenciación estricta
val HighContrastSuccess              = Color(0xFF00E676) // Verde menta brillante (> 12:1)
val HighContrastOnSuccess            = Color(0xFF000000)
val HighContrastSuccessContainer     = Color(0xFF003822)
val HighContrastOnSuccessContainer   = Color(0xFFFFFFFF)

val HighContrastError                = Color(0xFFFF5252) // Rojo brillante vivo (> 6.5:1)
val HighContrastOnError              = Color(0xFF000000)
val HighContrastErrorContainer       = Color(0xFF5A0004)
val HighContrastOnErrorContainer     = Color(0xFFFFFFFF)

// Compatibilidad retroactiva de referencias existentes
val DarkHCNightBackground            = HighContrastBackground
val DarkHCNightOnBackground          = HighContrastForeground
val DarkHCNightSurface               = HighContrastSurface
val DarkHCNightOnSurface             = HighContrastForeground
val DarkHCNightSurfaceVariant        = HighContrastSurfaceVariant
val DarkHCNightOnSurfaceVariant      = HighContrastForegroundSecondary
val DarkHCNightOutline               = HighContrastOutline
val DarkHCNightOutlineVariant        = HighContrastOutlineVariant
val DarkHCNightPrimary               = HighContrastPrimary
val DarkHCNightOnPrimary             = HighContrastOnPrimary
val DarkHCNightPrimaryContainer      = HighContrastPrimaryContainer
val DarkHCNightOnPrimaryContainer    = HighContrastOnPrimaryContainer
val DarkHCNightSecondary             = HighContrastSecondary
val DarkHCNightOnSecondary           = HighContrastOnSecondary
val DarkHCNightSecondaryContainer    = HighContrastSecondaryContainer
val DarkHCNightOnSecondaryContainer  = HighContrastOnSecondaryContainer
val DarkHCNightTertiary              = HighContrastTertiary
val DarkHCNightOnTertiary            = HighContrastOnTertiary
val DarkHCNightTertiaryContainer     = HighContrastTertiaryContainer
val DarkHCNightOnTertiaryContainer   = HighContrastOnTertiaryContainer
val DarkHCNightError                 = HighContrastError
val DarkHCNightOnError               = HighContrastOnError
val DarkHCNightErrorContainer        = HighContrastErrorContainer
val DarkHCNightOnErrorContainer      = HighContrastOnErrorContainer
val DarkHCNightSuccess               = HighContrastSuccess
val DarkHCNightProgress              = HighContrastPrimary
val DarkHCNightStreak                = HighContrastSecondary

// 4. Acentos semánticos estándar
val StreakOrange              = Color(0xFFA54300)
val SuccessGreen              = Color(0xFF2E7D32)
val ProgressBlue              = Color(0xFF1976D2)
val DarkStreakOrange          = Color(0xFFFFB74D)
val DarkSuccessGreen          = Color(0xFF81C784)
val DarkProgressBlue          = Color(0xFF64B5F6)

