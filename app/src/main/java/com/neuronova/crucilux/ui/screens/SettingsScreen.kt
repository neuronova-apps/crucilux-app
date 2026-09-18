package com.neuronova.crucilux.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuronova.crucilux.data.TextSizePreference
import com.neuronova.crucilux.data.UserPreferences
import com.neuronova.crucilux.data.UserPreferencesManager
import com.neuronova.crucilux.ui.components.OptionSelectorGroup
import com.neuronova.crucilux.ui.theme.LocalCruciluxHighContrast
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    preferencesManager: UserPreferencesManager,
    onNavigateToAbout: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val isHighContrast = LocalCruciluxHighContrast.current
    var nameInput by remember { mutableStateOf(userPreferences.userName) }
    var saveFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userPreferences.userName) {
        nameInput = userPreferences.userName
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Configuración",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onVolver,
                            modifier = Modifier.semantics {
                                contentDescription = "Volver a Inicio"
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
                if (isHighContrast) {
                    HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outline)
                }
            }
        },
        modifier = modifier.background(MaterialTheme.colorScheme.background),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // ── 1. Sección Perfil: Tu nombre ─────────────────────────────────
            SettingsCard(
                icon = Icons.Default.Person,
                title = "Tu nombre",
                subtitle = "Escribe cómo deseas que Crucilux se dirija a ti (opcional).",
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        saveFeedback = null
                    },
                    placeholder = { Text("Ej. Gabriel, María, Carlos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                if (saveFeedback != null) {
                    Text(
                        text = saveFeedback ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                preferencesManager.setUserName(nameInput)
                                saveFeedback = if (nameInput.isNotBlank()) "Nombre guardado" else "Nombre borrado"
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Guardar")
                    }

                    if (userPreferences.userName.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    preferencesManager.clearUserName()
                                    nameInput = ""
                                    saveFeedback = "Nombre eliminado"
                                }
                            },
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Borrar nombre guardado",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // ── 2. Sección Apariencia: Modo Día / Noche ──────────────────────
            SettingsCard(
                icon = if (userPreferences.isDarkMode) Icons.Default.Brightness4 else Icons.Default.Brightness7,
                title = "Apariencia",
                subtitle = "Selecciona el estilo visual de Crucilux.",
            ) {
                OptionSelectorGroup(
                    options = listOf("Modo día", "Modo noche"),
                    selectedOption = if (userPreferences.isDarkMode) "Modo noche" else "Modo día",
                    onOptionSelected = { option ->
                        val isDark = option == "Modo noche"
                        coroutineScope.launch {
                            preferencesManager.setDarkMode(isDark)
                        }
                    },
                    labelProvider = { it },
                )
            }

            // ── 3. Sección Accesibilidad (Patrón visual unificado de Brailux) ─────────
            SettingsCard(
                icon = Icons.Default.Contrast,
                title = "Accesibilidad",
                subtitle = "Opciones para facilitar la visualización y lectura.",
            ) {
                // 3.1. Fila principal: Modo de alto contraste
                SettingsToggle(
                    title = "Modo de alto contraste",
                    subtitle = "Refuerza fondos oscuros, textos blancos y bordes para máxima visibilidad.",
                    checked = userPreferences.isHighContrast,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            preferencesManager.setHighContrast(enabled)
                        }
                    },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = if (isHighContrast) MaterialTheme.colorScheme.outline
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = if (isHighContrast) 1.5.dp else 1.dp,
                )

                // 3.2. Sección Tamaño de texto
                Text(
                    text = "Tamaño de texto",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = "Ajusta la escala tipográfica en toda la aplicación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .selectableGroup(),
                ) {
                    SelectionOptionRow(
                        label = "Normal",
                        selected = userPreferences.textSize == TextSizePreference.Normal,
                        onSelect = {
                            coroutineScope.launch {
                                preferencesManager.setTextSize(TextSizePreference.Normal)
                            }
                        },
                    )
                    SelectionOptionRow(
                        label = "Grande",
                        selected = userPreferences.textSize == TextSizePreference.Large,
                        onSelect = {
                            coroutineScope.launch {
                                preferencesManager.setTextSize(TextSizePreference.Large)
                            }
                        },
                    )
                    SelectionOptionRow(
                        label = "Muy grande",
                        selected = userPreferences.textSize == TextSizePreference.VeryLarge,
                        onSelect = {
                            coroutineScope.launch {
                                preferencesManager.setTextSize(TextSizePreference.VeryLarge)
                            }
                        },
                    )
                }
            }

            // ── 4. Sección Acerca de Crucilux ───────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClick = onNavigateToAbout)
                    .semantics { contentDescription = "Abrir pantalla Acerca de Crucilux" },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(
                    width = if (isHighContrast) 1.5.dp else 1.dp,
                    color = if (isHighContrast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Column {
                            Text(
                                text = "Acerca de Crucilux",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Versión, créditos y ecosistema",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    val isHighContrast = LocalCruciluxHighContrast.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = if (isHighContrast) 1.5.dp else 1.dp,
            color = if (isHighContrast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            content()
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isHighContrast = LocalCruciluxHighContrast.current
    val state = if (checked) "Activado" else "Desactivado"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) {
                stateDescription = state
            }
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (isHighContrast) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                checkedTrackColor = if (isHighContrast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                checkedBorderColor = if (isHighContrast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

@Composable
private fun SelectionOptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = if (selected) "Seleccionado" else "No seleccionado"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .semantics(mergeDescendants = true) {
                stateDescription = state
            }
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline,
            ),
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

