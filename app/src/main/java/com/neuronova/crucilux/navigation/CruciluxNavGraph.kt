package com.neuronova.crucilux.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.neuronova.crucilux.data.GameConfigProvider
import com.neuronova.crucilux.ui.screens.GameSetupReadyScreen
import com.neuronova.crucilux.ui.screens.HomeScreen
import com.neuronova.crucilux.ui.screens.PlayScreen
import com.neuronova.crucilux.ui.screens.ProgressScreen
import com.neuronova.crucilux.ui.screens.WelcomeScreen

/** Destinos de navegación de Crucilux. */
sealed class Screen(val route: String) {
    object Welcome        : Screen("welcome")
    object Home           : Screen("home")
    object Play           : Screen("play")
    object Progress       : Screen("progress")
    object GameSetupReady : Screen("game_setup_ready/{category}/{size}/{difficulty}") {
        fun createRoute(category: String, size: String, difficulty: String): String {
            return "game_setup_ready/$category/$size/$difficulty"
        }
    }
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
        startDestination = Screen.Welcome.route,
        modifier         = modifier,
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onComenzar = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
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
            PlayScreen(
                onNavigateToReady = { category, size, difficulty ->
                    navController.navigate(
                        Screen.GameSetupReady.createRoute(category, size, difficulty)
                    ) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Screen.Progress.route) {
            ProgressScreen()
        }
        composable(
            route = Screen.GameSetupReady.route,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType },
                navArgument("size") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: GameConfigProvider.defaultCategory.displayName
            val size = backStackEntry.arguments?.getString("size") ?: GameConfigProvider.defaultSize.label
            val difficulty = backStackEntry.arguments?.getString("difficulty") ?: GameConfigProvider.defaultDifficulty.displayName

            GameSetupReadyScreen(
                category = category,
                size = size,
                difficulty = difficulty,
                onVolver = {
                    navController.popBackStack()
                },
            )
        }
    }
}
