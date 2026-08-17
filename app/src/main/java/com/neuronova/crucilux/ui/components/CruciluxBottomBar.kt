package com.neuronova.crucilux.ui.components

import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.neuronova.crucilux.navigation.Screen

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val contentDesc: String,
)

private val navItems = listOf(
    BottomNavItem(
        screen         = Screen.Home,
        label          = "Inicio",
        selectedIcon   = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        contentDesc    = "Ir a Inicio",
    ),
    BottomNavItem(
        screen         = Screen.Play,
        label          = "Jugar",
        selectedIcon   = Icons.Filled.PlayArrow,
        unselectedIcon = Icons.Outlined.PlayArrow,
        contentDesc    = "Ir a Jugar",
    ),
    BottomNavItem(
        screen         = Screen.Progress,
        label          = "Progreso",
        selectedIcon   = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart,
        contentDesc    = "Ir a Progreso",
    ),
)

@Composable
fun CruciluxBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier       = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        navItems.forEach { item ->
            val selected = currentRoute == item.screen.route
            NavigationBarItem(
                selected = selected,
                onClick  = { onNavigate(item.screen) },
                icon = {
                    Icon(
                        imageVector        = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.contentDesc,
                    )
                },
                label = {
                    Text(
                        text  = item.label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                alwaysShowLabel = true,
                colors          = NavigationBarItemDefaults.colors(
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    selectedTextColor   = MaterialTheme.colorScheme.primary,
                    indicatorColor      = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
