package com.neuronova.crucilux.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuronova.crucilux.data.GameConfigProvider
import com.neuronova.crucilux.ui.components.CategoryCard
import com.neuronova.crucilux.ui.components.OptionSelectorGroup

@Composable
fun PlayScreen(
    onNavigateToReady: (category: String, size: String, difficulty: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Las opciones se obtienen directamente del proveedor centralizado de configuración
    val categories = GameConfigProvider.provisionalCategories
    val sizes = GameConfigProvider.availableSizes
    val difficulties = GameConfigProvider.availableDifficulties

    // Estado Compose inicializado con los valores predeterminados del proveedor
    var selectedCategory by remember { mutableStateOf(GameConfigProvider.defaultCategory.displayName) }
    var selectedSize by remember { mutableStateOf(GameConfigProvider.defaultSize.label) }
    var selectedDifficulty by remember { mutableStateOf(GameConfigProvider.defaultDifficulty.displayName) }

    val isFormComplete = selectedCategory.isNotBlank() && selectedSize.isNotBlank() && selectedDifficulty.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(20.dp))

        // Cabecera
        Text(
            text = "Nueva partida",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Configura tu crucigrama",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        // Sección 1: Categoría
        SectionHeader(
            title = "Categoría",
            subtitle = "Selecciona la temática de las palabras",
        )
        Spacer(Modifier.height(10.dp))

        // Cuadrícula de 2 columnas para categorías
        val chunkedCategories = categories.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chunkedCategories.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEach { item ->
                        CategoryCard(
                            title = item.displayName,
                            icon = item.icon ?: Icons.Default.Category,
                            isSelected = selectedCategory == item.displayName,
                            onSelect = { selectedCategory = item.displayName },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowItems.size < 2) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // Sección 2: Tamaño (7x7, 10x10, 15x15)
        SectionHeader(
            title = "Tamaño",
            subtitle = "Dimensiones del tablero",
        )
        Spacer(Modifier.height(10.dp))
        OptionSelectorGroup(
            options = sizes.map { it.label },
            selectedOption = selectedSize,
            onOptionSelected = { selectedSize = it },
            labelProvider = { it },
        )

        Spacer(Modifier.height(22.dp))

        // Sección 3: Dificultad (Fácil, Intermedio, Difícil)
        SectionHeader(
            title = "Dificultad",
            subtitle = "Complejidad de las pistas",
        )
        Spacer(Modifier.height(10.dp))
        OptionSelectorGroup(
            options = difficulties.map { it.displayName },
            selectedOption = selectedDifficulty,
            onOptionSelected = { selectedDifficulty = it },
            labelProvider = { it },
        )

        Spacer(Modifier.height(28.dp))

        // Botón principal de acción
        Button(
            onClick = {
                if (isFormComplete) {
                    onNavigateToReady(
                        selectedCategory,
                        selectedSize,
                        selectedDifficulty,
                    )
                }
            },
            enabled = isFormComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .semantics {
                    contentDescription = if (isFormComplete) {
                        "Crear crucigrama con categoría $selectedCategory, tamaño $selectedSize y dificultad $selectedDifficulty"
                    } else {
                        "Crear crucigrama deshabilitado, faltan selecciones obligatorias"
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
                text = "Crear crucigrama",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
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
