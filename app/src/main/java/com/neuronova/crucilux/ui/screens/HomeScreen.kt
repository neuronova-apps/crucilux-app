package com.neuronova.crucilux.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuronova.crucilux.ui.theme.ProgressBlue
import com.neuronova.crucilux.ui.theme.StreakOrange
import com.neuronova.crucilux.ui.theme.SuccessGreen

@Composable
fun HomeScreen(onComenzar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        HomeHeader()

        // Identidad visual compacta estilo crucigrama
        CrosswordBrandVisual(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
        )

        Spacer(Modifier.height(10.dp))

        // Tarjeta compacta "Mi actividad"
        StatsCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(14.dp))

        // Botón de acción principal
        Button(
            onClick   = onComenzar,
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(52.dp)
                .semantics { contentDescription = "Comenzar una nueva partida de Crucilux" },
            shape     = RoundedCornerShape(14.dp),
            colors    = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 1.dp,
                pressedElevation = 3.dp,
            ),
        ) {
            Text(
                text       = "Comenzar",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Tarjetas secundarias compactas
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DailyChallengeCard(Modifier.weight(1f))
            AchievementsCard(Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cabecera
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader() {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text          = "CRUCILUX",
                style         = MaterialTheme.typography.headlineLarge,
                color         = MaterialTheme.colorScheme.primary,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "Ejercita tu mente con palabras",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Botón Aa — accesibilidad / apariencia (referencia estilo Sudolux)
        FilledTonalButton(
            onClick        = { /* reservado para opciones de accesibilidad / apariencia */ },
            modifier       = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                .semantics { contentDescription = "Opciones de apariencia y accesibilidad" },
            shape          = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        ) {
            Text(
                text       = "Aa",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Identidad visual de Crucilux (cuadrícula compacta y equilibrada)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CrosswordBrandVisual(modifier: Modifier = Modifier) {
    // Cuadrícula compacta 3×3 con el monograma C-R-U / ·-X-· / L-U-X
    val grid = listOf(
        listOf("C", "R", "U"),
        listOf(" ", "X", " "),
        listOf("L", "U", "X"),
    )

    Box(
        modifier         = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            grid.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { letter ->
                        val active = letter.isNotBlank()
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                    else Color.Transparent,
                                )
                                .then(
                                    if (active) Modifier.border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(4.dp),
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (active) {
                                Text(
                                    text       = letter,
                                    fontSize   = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjeta compacta "Mi actividad" con iconos Material
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsCard(modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text       = "Mi actividad",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                StatItem(
                    icon        = Icons.Default.LocalFireDepartment,
                    iconTint    = StreakOrange,
                    value       = "0",
                    label       = "Racha",
                    contentDesc = "Racha actual: 0 días",
                )
                StatDivider()
                StatItem(
                    icon        = Icons.Default.CheckCircleOutline,
                    iconTint    = SuccessGreen,
                    value       = "0",
                    label       = "Completados",
                    contentDesc = "Crucigramas completados: 0",
                )
                StatDivider()
                StatItem(
                    icon        = Icons.Default.PieChart,
                    iconTint    = ProgressBlue,
                    value       = "0 %",
                    label       = "Progreso",
                    contentDesc = "Progreso general: 0 por ciento",
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    contentDesc: String,
) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            contentDescription = contentDesc
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = iconTint,
            modifier           = Modifier.size(20.dp),
        )
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(38.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Tarjetas secundarias ("Desafío diario" y "Logros")
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DailyChallengeCard(modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.semantics {
            contentDescription = "Desafío diario, próximamente disponible"
        },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.secondary,
                    modifier           = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text       = "Desafío diario",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "Próximamente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AchievementsCard(modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.semantics {
            contentDescription = "Logros, próximamente disponible"
        },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.tertiary,
                    modifier           = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text       = "Logros",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "Próximamente",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
