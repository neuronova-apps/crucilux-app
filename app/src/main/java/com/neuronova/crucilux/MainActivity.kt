package com.neuronova.crucilux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.neuronova.crucilux.navigation.CruciluxNavGraph
import com.neuronova.crucilux.navigation.Screen
import com.neuronova.crucilux.navigation.bottomBarRoutes
import com.neuronova.crucilux.ui.components.CruciluxBottomBar
import com.neuronova.crucilux.ui.theme.CruciluxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CruciluxTheme {
                CruciluxApp()
            }
        }
    }
}

@Composable
private fun CruciluxApp() {
    val navController   = rememberNavController()
    val backStackEntry  by navController.currentBackStackEntryAsState()
    val currentRoute    = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                CruciluxBottomBar(
                    currentRoute = currentRoute,
                    onNavigate   = { screen: Screen ->
                        navController.navigate(screen.route) {
                            // Vuelve al inicio sin acumular destinos en la pila
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        CruciluxNavGraph(
            navController = navController,
            modifier      = Modifier.padding(innerPadding),
        )
    }
}
