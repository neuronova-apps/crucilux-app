package com.neuronova.crucilux.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuronova.crucilux.data.GameSessionManager
import com.neuronova.crucilux.data.GameSessionState
import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

/**
 * Pantalla de confirmación "Partida preparada".
 * Muestra el resumen de la temática seleccionada por el usuario
 * y el tablero asignado desde el banco maestro validado (v1.37) con dimensiones dinámicas.
 */
@Composable
fun GameSetupReadyScreen(
    category: String,
    size: String? = null,
    onVolver: () -> Unit,
    onIniciar: (boardId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sessionManager = remember { GameSessionManager.getInstance(context) }
    val sessionState by sessionManager.sessionFlow.collectAsState(initial = GameSessionState())
    val coroutineScope = rememberCoroutineScope()
    var showOverwriteDialog by remember { mutableStateOf(false) }

    val repository = remember { CruciluxBankRepository.getInstance() }
    val assignedBoard = remember(category) {
        repository.obtenerCrucigrama(category)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(36.dp))

        // Icono de estado preparado
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(38.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Partida preparada",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Tu configuración está lista",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        // Tarjeta de resumen de configuración
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Resumen de partida",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                SummaryItemRow(
                    icon = Icons.Default.Category,
                    label = "Categoría",
                    value = category,
                )

                if (assignedBoard != null) {
                    SummaryItemRow(
                        icon = Icons.Default.Tag,
                        label = "Tablero asignado",
                        value = assignedBoard.id,
                    )

                    SummaryItemRow(
                        icon = Icons.Default.GridOn,
                        label = "Dimensiones",
                        value = "${assignedBoard.rows} × ${assignedBoard.cols}",
                    )

                    SummaryItemRow(
                        icon = Icons.AutoMirrored.Filled.List,
                        label = "Palabras a descubrir",
                        value = "${assignedBoard.entries.size} palabras",
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Botón principal: Iniciar partida
        Button(
            onClick = {
                if (assignedBoard != null) {
                    if (sessionState.hasActiveSession && sessionState.boardId != assignedBoard.id) {
                        showOverwriteDialog = true
                    } else {
                        onIniciar(assignedBoard.id)
                    }
                }
            },
            enabled = assignedBoard != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .semantics {
                    contentDescription = if (assignedBoard != null) {
                        "Iniciar partida del tablero ${assignedBoard.id}"
                    } else {
                        "Iniciar partida, cargando tablero"
                    }
                },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Text(
                text = "Iniciar partida",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }

        // Diálogo de confirmación antes de sobrescribir partida guardada
        if (showOverwriteDialog && assignedBoard != null) {
            AlertDialog(
                onDismissRequest = { showOverwriteDialog = false },
                title = {
                    Text(
                        text = "Partida en curso",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Text(
                        text = "Ya tienes una partida guardada (${sessionState.category.ifBlank { "Crucigrama" }}).\n\n¿Deseas continuar con tu partida anterior o iniciar esta nueva partida? Iniciar una nueva reemplazará el guardado previo.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showOverwriteDialog = false
                            coroutineScope.launch {
                                sessionManager.clearSession()
                                onIniciar(assignedBoard.id)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Iniciar nueva")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { showOverwriteDialog = false },
                        ) {
                            Text("Cancelar")
                        }
                        TextButton(
                            onClick = {
                                showOverwriteDialog = false
                                onIniciar(sessionState.boardId)
                            },
                        ) {
                            Text("Continuar partida")
                        }
                    }
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Botón Volver
        Button(
            onClick = onVolver,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .semantics { contentDescription = "Volver a la configuración de partida" },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text(
                text = "Volver",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SummaryItemRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
