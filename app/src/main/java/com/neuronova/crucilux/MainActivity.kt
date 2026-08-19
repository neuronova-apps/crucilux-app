package com.neuronova.crucilux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.neuronova.crucilux.data.UserPreferences
import com.neuronova.crucilux.data.UserPreferencesManager
import com.neuronova.crucilux.navigation.CruciluxNavGraph
import com.neuronova.crucilux.navigation.Screen
import com.neuronova.crucilux.navigation.bottomBarRoutes
import com.neuronova.crucilux.ui.components.CruciluxBottomBar
import com.neuronova.crucilux.ui.theme.CruciluxTheme

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: UserPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = UserPreferencesManager.getInstance(applicationContext)
        enableEdgeToEdge()

        setContent {
            val userPreferences by preferencesManager.userPreferencesFlow
                .collectAsState(initial = UserPreferences())

            CruciluxTheme(
                darkTheme = userPreferences.isDarkMode,
                highContrast = userPreferences.isHighContrast,
            ) {
                CruciluxApp(
                    userPreferences = userPreferences,
                    preferencesManager = preferencesManager,
                )
            }
        }
    }
}

@Composable
private fun CruciluxApp(
    userPreferences: UserPreferences,
    preferencesManager: UserPreferencesManager,
) {
    val navController   = rememberNavController()
    val backStackEntry  by navController.currentBackStackEntryAsState()
    val currentRoute    = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                CruciluxBottomBar(
                    currentRoute = currentRoute,
                    onNavigate   = { screen: Screen ->
                        if (screen.route != currentRoute) {
                            if (screen == Screen.Home) {
                                // Inicio es la raíz real de la navegación principal. Al volver
                                // se elimina cualquier destino superior y nunca se restaura una
                                // copia guardada de Jugar/Progreso encima de Inicio.
                                val returnedHome = navController.popBackStack(
                                    route = Screen.Home.route,
                                    inclusive = false,
                                )
                                if (!returnedHome) {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(navController.graph.id)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        CruciluxNavGraph(
            navController      = navController,
            userPreferences    = userPreferences,
            preferencesManager = preferencesManager,
            modifier           = Modifier.padding(innerPadding),
        )
    }
}
