package com.neuronova.crucilux.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.neuronova.crucilux.ui.screens.HomeScreen
import com.neuronova.crucilux.ui.screens.PlayScreen
import com.neuronova.crucilux.ui.screens.ProgressScreen

/** Destinos de navegación de Crucilux. */
sealed class Screen(val route: String) {
    object Home     : Screen("home")
    object Play     : Screen("play")
    object Progress : Screen("progress")
}

/** Rutas que muestran la barra de navegación inferior. */
val bottomBarRoutes = setOf(
    Screen.Home.route,
    Screen.Play.route,
    Screen.Progress.route,
)

@Composable
fun CruciluxNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Home.route,
        modifier         = modifier,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onComenzar = {
                    navController.navigate(Screen.Play.route) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Screen.Play.route) {
            PlayScreen()
        }
        composable(Screen.Progress.route) {
            ProgressScreen()
        }
    }
}
