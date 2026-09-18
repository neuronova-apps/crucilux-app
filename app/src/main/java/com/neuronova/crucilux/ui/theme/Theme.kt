package com.neuronova.crucilux.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.neuronova.crucilux.data.TextSizePreference

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

// 2. Noche Normal
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

// 3. Modo de Alto Contraste Integral (Fondo negro real #000000, texto blanco #FFFFFF, WCAG AAA)
val HighContrastColorScheme = darkColorScheme(
    primary              = HighContrastPrimary,
    onPrimary            = HighContrastOnPrimary,
    primaryContainer     = HighContrastPrimaryContainer,
    onPrimaryContainer   = HighContrastOnPrimaryContainer,
    secondary            = HighContrastSecondary,
    onSecondary          = HighContrastOnSecondary,
    secondaryContainer   = HighContrastSecondaryContainer,
    onSecondaryContainer = HighContrastOnSecondaryContainer,
    tertiary             = HighContrastTertiary,
    onTertiary           = HighContrastOnTertiary,
    tertiaryContainer    = HighContrastTertiaryContainer,
    onTertiaryContainer  = HighContrastOnTertiaryContainer,
    error                = HighContrastError,
    onError              = HighContrastOnError,
    errorContainer       = HighContrastErrorContainer,
    onErrorContainer     = HighContrastOnErrorContainer,
    background           = HighContrastBackground,
    onBackground         = HighContrastForeground,
    surface              = HighContrastSurface,
    onSurface            = HighContrastForeground,
    surfaceVariant       = HighContrastSurfaceVariant,
    onSurfaceVariant     = HighContrastForegroundSecondary,
    outline              = HighContrastOutline,
    outlineVariant       = HighContrastOutlineVariant,
    surfaceContainer     = HighContrastSurfaceVariant,
    surfaceContainerLow  = HighContrastSurface,
    surfaceContainerHigh = Color(0xFF282828),
)

@Immutable
data class CruciluxSemanticColors(
    val success: Color,
    val progress: Color,
    val streak: Color,
)

val LocalCruciluxSemanticColors = staticCompositionLocalOf {
    CruciluxSemanticColors(
        success = SuccessGreen,
        progress = ProgressBlue,
        streak = StreakOrange,
    )
}

val LocalCruciluxHighContrast = staticCompositionLocalOf { false }

@Immutable
data class CruciluxBoardColors(
    val blockedCellBackground: Color,
    val blockedCellBorder: Color,
    val cellBackground: Color,
    val cellBorder: Color,
    val selectedCellBackground: Color,
    val selectedCellBorder: Color,
    val activeWordBackground: Color,
    val activeWordBorder: Color,
    val validatedCellBackground: Color,
    val validatedCellBorder: Color,
    val incorrectCellBackground: Color,
    val incorrectCellBorder: Color,
    val hintCellBackground: Color,
    val hintCellBorder: Color,
)

val LocalCruciluxBoardColors = staticCompositionLocalOf {
    CruciluxBoardColors(
        blockedCellBackground = Color(0xFF1E293B),
        blockedCellBorder = Color(0xFFCBD5E1),
        cellBackground = Color(0xFFFFFFFF),
        cellBorder = Color(0xFFCBD5E1),
        selectedCellBackground = Color(0xFFBBDEFB),
        selectedCellBorder = Color(0xFF1976D2),
        activeWordBackground = Color(0xFFE3F2FD),
        activeWordBorder = Color(0xFF90CAF9),
        validatedCellBackground = Color(0xFFE8F5E9),
        validatedCellBorder = Color(0xFF2E7D32),
        incorrectCellBackground = Color(0xFFFFEBEE),
        incorrectCellBorder = Color(0xFFBA1A1A),
        hintCellBackground = Color(0xFFEDE7F6),
        hintCellBorder = Color(0xFF7E57C2),
    )
}

object CruciluxThemeColors {
    val success: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalCruciluxSemanticColors.current.success

    val progress: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalCruciluxSemanticColors.current.progress

    val streak: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalCruciluxSemanticColors.current.streak

    val isHighContrast: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalCruciluxHighContrast.current

    val board: CruciluxBoardColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCruciluxBoardColors.current
}

/**
 * Tema principal de Crucilux con soporte completo para:
 * - Día Normal
 * - Día + Alto Contraste
 * - Noche Normal
 * - Noche + Alto Contraste
 * - Ajuste global controlado de escala de texto
 */
@Composable
fun CruciluxTheme(
    darkTheme: Boolean = false,
    highContrast: Boolean = false,
    textSize: TextSizePreference = TextSizePreference.Normal,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast -> HighContrastColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme    -> DarkColorScheme
        else         -> LightColorScheme
    }

    val semanticColors = when {
        highContrast -> CruciluxSemanticColors(
            success = HighContrastSuccess,
            progress = HighContrastPrimary,
            streak = HighContrastSecondary,
        )
        darkTheme -> CruciluxSemanticColors(
            success = DarkSuccessGreen,
            progress = DarkProgressBlue,
            streak = DarkStreakOrange,
        )
        else -> CruciluxSemanticColors(
            success = SuccessGreen,
            progress = ProgressBlue,
            streak = StreakOrange,
        )
    }

    val boardColors = when {
        highContrast -> CruciluxBoardColors(
            blockedCellBackground   = Color(0xFF000000), // Bloque negro real #000000
            blockedCellBorder       = Color(0xFFFFFFFF), // Borde blanco nítido #FFFFFF
            cellBackground          = Color(0xFF121212), // Superficie oscura
            cellBorder              = Color(0xFFFFFFFF), // Borde blanco
            selectedCellBackground  = Color(0xFF003859), // Azul oscuro destacado
            selectedCellBorder      = Color(0xFF40C4FF), // Borde cian eléctrico
            activeWordBackground    = Color(0xFF002238), // Resaltado de palabra
            activeWordBorder        = Color(0xFF40C4FF),
            validatedCellBackground = Color(0xFF003822), // Verde contenedor
            validatedCellBorder     = Color(0xFF00E676), // Verde brillante
            incorrectCellBackground = Color(0xFF5A0004), // Rojo contenedor
            incorrectCellBorder     = Color(0xFFFF5252), // Rojo vivo
            hintCellBackground      = Color(0xFF38006B), // Pista contenedor
            hintCellBorder          = Color(0xFFEA80FC), // Pista borde
        )
        darkTheme -> CruciluxBoardColors(
            blockedCellBackground   = Color(0xFF0A0F1D), // Bloque oscuro en modo noche normal
            blockedCellBorder       = Color(0xFF334155),
            cellBackground          = DarkSurface,
            cellBorder              = DarkOutline,
            selectedCellBackground  = DarkPrimaryContainer.copy(alpha = 0.85f),
            selectedCellBorder      = DarkPrimary,
            activeWordBackground    = DarkPrimaryContainer.copy(alpha = 0.35f),
            activeWordBorder        = DarkPrimary.copy(alpha = 0.55f),
            validatedCellBackground = DarkSuccessGreen.copy(alpha = 0.25f),
            validatedCellBorder     = DarkSuccessGreen.copy(alpha = 0.75f),
            incorrectCellBackground = DarkErrorContainer.copy(alpha = 0.65f),
            incorrectCellBorder     = DarkError,
            hintCellBackground      = DarkTertiaryContainer.copy(alpha = 0.65f),
            hintCellBorder          = DarkTertiary,
        )
        else -> CruciluxBoardColors(
            blockedCellBackground   = Color(0xFF1E293B), // Bloque negro estándar
            blockedCellBorder       = LightOutline.copy(alpha = 0.25f),
            cellBackground          = LightSurface,
            cellBorder              = LightOutline.copy(alpha = 0.7f),
            selectedCellBackground  = LightPrimaryContainer.copy(alpha = 0.85f),
            selectedCellBorder      = LightPrimary,
            activeWordBackground    = LightPrimaryContainer.copy(alpha = 0.35f),
            activeWordBorder        = LightPrimary.copy(alpha = 0.55f),
            validatedCellBackground = SuccessGreen.copy(alpha = 0.22f),
            validatedCellBorder     = SuccessGreen.copy(alpha = 0.75f),
            incorrectCellBackground = LightErrorContainer.copy(alpha = 0.65f),
            incorrectCellBorder     = LightError,
            hintCellBackground      = LightTertiaryContainer.copy(alpha = 0.65f),
            hintCellBorder          = LightTertiary,
        )
    }

    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * textSize.scaleFactor,
    )

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalCruciluxSemanticColors provides semanticColors,
        LocalCruciluxHighContrast provides highContrast,
        LocalCruciluxBoardColors provides boardColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = CruciluxTypography,
            content     = content,
        )
    }
}
