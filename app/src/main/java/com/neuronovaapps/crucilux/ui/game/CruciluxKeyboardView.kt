package com.neuronovaapps.crucilux.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuronovaapps.crucilux.ui.theme.LocalCruciluxHighContrast

/**
 * Teclado virtual adaptativo en español para Crucilux.
 *
 * Filas:
 * 1. Q W E R T Y U I O P
 * 2. A S D F G H J K L Ñ
 * 3. Z X C V B N M  [⌫ Borrar]
 *
 * Características:
 * - Totalmente adaptado al ancho de pantallas móviles.
 * - Sin números.
 * - Tecla de borrado con icono accesible.
 * - Bordes definidos y estados de pulsación reforzados para alto contraste.
 * - Soporte TalkBack completo.
 */
@Composable
fun CruciluxKeyboardView(
    onLetter: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isHighContrast = LocalCruciluxHighContrast.current
    val row1 = listOf('Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P')
    val row2 = listOf('A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', 'Ñ')
    val row3 = listOf('Z', 'X', 'C', 'V', 'B', 'N', 'M')

    val keyboardShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isHighContrast) {
                    Modifier.border(width = 1.5.dp, color = MaterialTheme.colorScheme.outline, shape = keyboardShape)
                } else {
                    Modifier
                }
            ),
        color = if (isHighContrast) MaterialTheme.colorScheme.surfaceVariant
               else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = keyboardShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            // Fila 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                row1.forEach { letter ->
                    KeyButton(
                        letter = letter,
                        onClick = { onLetter(letter) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Fila 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                row2.forEach { letter ->
                    KeyButton(
                        letter = letter,
                        onClick = { onLetter(letter) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Fila 3 con tecla de borrado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(0.5f))

                row3.forEach { letter ->
                    KeyButton(
                        letter = letter,
                        onClick = { onLetter(letter) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Tecla Borrar
                DeleteButton(
                    onClick = onDelete,
                    enabled = enabled,
                    modifier = Modifier.weight(1.5f),
                )
            }
        }
    }
}

/**
 * Tecla de letra individual.
 */
@Composable
private fun KeyButton(
    letter: Char,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isHighContrast = LocalCruciluxHighContrast.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = when {
        !enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        isPressed && isHighContrast -> MaterialTheme.colorScheme.primaryContainer
        isPressed -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
        isPressed && isHighContrast -> MaterialTheme.colorScheme.primary
        isPressed -> MaterialTheme.colorScheme.primary
        isHighContrast -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    }

    val borderWidth = when {
        isPressed && isHighContrast -> 2.dp
        isPressed -> 1.5.dp
        isHighContrast -> 1.5.dp
        else -> 1.dp
    }

    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isPressed && isHighContrast -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable(
                role = Role.Button,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics {
                contentDescription = "Letra $letter"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isHighContrast) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = 17.sp,
            color = textColor,
        )
    }
}

/**
 * Tecla de borrado (Backspace).
 */
@Composable
private fun DeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isHighContrast = LocalCruciluxHighContrast.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        isPressed && isHighContrast -> MaterialTheme.colorScheme.primaryContainer
        isPressed -> MaterialTheme.colorScheme.surfaceContainerHighest
        isHighContrast -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)
        isPressed && isHighContrast -> MaterialTheme.colorScheme.primary
        isPressed -> MaterialTheme.colorScheme.primary
        isHighContrast -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    }

    val borderWidth = when {
        isPressed && isHighContrast -> 2.dp
        isPressed -> 1.5.dp
        isHighContrast -> 1.5.dp
        else -> 1.dp
    }

    val iconTint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        isPressed && isHighContrast -> MaterialTheme.colorScheme.primary
        isHighContrast -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(8.dp))
            .clickable(
                role = Role.Button,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .semantics {
                contentDescription = "Borrar letra"
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}
