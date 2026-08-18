package com.neuronova.crucilux.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// 1. Día Normal
private val LightColorScheme = lightColorScheme(
    primary              = LightPrimary,
    onPrimary            = LightOnPrimary,
    primaryContainer     = LightPrimaryContainer,
    onPrimaryContainer   = LightOnPrimaryContainer,
    secondary            = LightSecondary,
    onSecondary          = LightOnSecondary,
    secondaryContainer   = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary             = LightTertiary,
    onTertiary           = LightOnTertiary,
    tertiaryContainer    = LightTertiaryContainer,
    onTertiaryContainer  = LightOnTertiaryContainer,
    error                = LightError,
    onError              = LightOnError,
    errorContainer       = LightErrorContainer,
    onErrorContainer     = LightOnErrorContainer,
    background           = LightBackground,
    onBackground         = LightOnBackground,
    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVariant,
    onSurfaceVariant     = LightOnSurfaceVariant,
    outline              = LightOutline,
    outlineVariant       = LightOutlineVariant,
)

// 2. Día + Alto Contraste
private val LightHighContrastColorScheme = lightColorScheme(
    primary              = LightHCDayPrimary,
    onPrimary            = LightHCDayOnPrimary,
    primaryContainer     = LightHCDayPrimaryContainer,
    onPrimaryContainer   = LightHCDayOnPrimaryContainer,
    secondary            = LightHCDaySecondary,
    onSecondary          = LightHCDayOnSecondary,
    secondaryContainer   = LightHCDaySecondaryContainer,
    onSecondaryContainer = LightHCDayOnSecondaryContainer,
    tertiary             = LightHCDayTertiary,
    onTertiary           = LightHCDayOnTertiary,
    tertiaryContainer    = LightHCDayTertiaryContainer,
    onTertiaryContainer  = LightHCDayOnTertiaryContainer,
    error                = LightError,
    onError              = LightOnError,
    errorContainer       = LightErrorContainer,
    onErrorContainer     = LightOnErrorContainer,
    background           = LightHCDayBackground,
    onBackground         = LightHCDayOnBackground,
    surface              = LightHCDaySurface,
    onSurface            = LightHCDayOnSurface,
    surfaceVariant       = LightHCDaySurfaceVariant,
    onSurfaceVariant     = LightHCDayOnSurfaceVariant,
    outline              = LightHCDayOutline,
    outlineVariant       = LightHCDayOutlineVariant,
)

// 3. Noche Normal
private val DarkColorScheme = darkColorScheme(
    primary              = DarkPrimary,
    onPrimary            = DarkOnPrimary,
    primaryContainer     = DarkPrimaryContainer,
    onPrimaryContainer   = DarkOnPrimaryContainer,
    secondary            = DarkSecondary,
    onSecondary          = DarkOnSecondary,
    secondaryContainer   = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary             = DarkTertiary,
    onTertiary           = DarkOnTertiary,
    tertiaryContainer    = DarkTertiaryContainer,
    onTertiaryContainer  = DarkOnTertiaryContainer,
    error                = DarkError,
    onError              = DarkOnError,
    errorContainer       = DarkErrorContainer,
    onErrorContainer     = DarkOnErrorContainer,
    background           = DarkBackground,
    onBackground         = DarkOnBackground,
    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVariant,
    onSurfaceVariant     = DarkOnSurfaceVariant,
    outline              = DarkOutline,
    outlineVariant       = DarkOutlineVariant,
)

// 4. Noche + Alto Contraste
private val DarkHighContrastColorScheme = darkColorScheme(
    primary              = DarkHCNightPrimary,
    onPrimary            = DarkHCNightOnPrimary,
    primaryContainer     = DarkHCNightPrimaryContainer,
    onPrimaryContainer   = DarkHCNightOnPrimaryContainer,
    secondary            = DarkHCNightSecondary,
    onSecondary          = DarkHCNightOnSecondary,
    secondaryContainer   = DarkHCNightSecondaryContainer,
    onSecondaryContainer = DarkHCNightOnSecondaryContainer,
    tertiary             = DarkHCNightTertiary,
    onTertiary           = DarkHCNightOnTertiary,
    tertiaryContainer    = DarkHCNightTertiaryContainer,
    onTertiaryContainer  = DarkHCNightOnTertiaryContainer,
    error                = DarkError,
    onError              = DarkOnError,
    errorContainer       = DarkErrorContainer,
    onErrorContainer     = DarkOnErrorContainer,
    background           = DarkHCNightBackground,
    onBackground         = DarkHCNightOnBackground,
    surface              = DarkHCNightSurface,
    onSurface            = DarkHCNightOnSurface,
    surfaceVariant       = DarkHCNightSurfaceVariant,
    onSurfaceVariant     = DarkHCNightOnSurfaceVariant,
    outline              = DarkHCNightOutline,
    outlineVariant       = DarkHCNightOutlineVariant,
)

/**
 * Tema principal de Crucilux con soporte completo para:
 * - Día Normal
 * - Día + Alto Contraste
 * - Noche Normal
 * - Noche + Alto Contraste
 *
 * Modo día es el valor predeterminado.
 */
@Composable
fun CruciluxTheme(
    darkTheme: Boolean = false,
    highContrast: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme && highContrast  -> DarkHighContrastColorScheme
        darkTheme && !highContrast -> DarkColorScheme
        !darkTheme && highContrast -> LightHighContrastColorScheme
        else                       -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = CruciluxTypography,
        content     = content
    )
}
