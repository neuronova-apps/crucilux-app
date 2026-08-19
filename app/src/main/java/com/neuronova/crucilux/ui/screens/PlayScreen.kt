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

@Composable
fun PlayScreen(
    onNavigateToReady: (category: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Las opciones se obtienen directamente del proveedor centralizado basado en el banco real (v1.37)
    val categories = GameConfigProvider.categories

    // Estado Compose inicializado con la categoría predeterminada
    var selectedCategory by remember { mutableStateOf(GameConfigProvider.defaultCategory.displayName) }

    val isFormComplete = selectedCategory.isNotBlank()

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
            text = "Selecciona una temática para comenzar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        // Sección: Categoría (10 categorías temáticas reales del banco)
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
                            isSelected = selectedCategory.equals(item.displayName, ignoreCase = true),
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

        Spacer(Modifier.height(32.dp))

        // Botón principal de acción
        Button(
            onClick = {
                if (isFormComplete) {
                    onNavigateToReady(selectedCategory)
                }
            },
            enabled = isFormComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .semantics {
                    contentDescription = if (isFormComplete) {
                        "Crear crucigrama con temática $selectedCategory"
                    } else {
                        "Crear crucigrama deshabilitado, selecciona una temática"
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
