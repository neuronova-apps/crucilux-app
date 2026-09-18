package com.neuronovaapps.crucilux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.neuronovaapps.crucilux.achievements.AchievementRepository
import com.neuronovaapps.crucilux.achievements.AchievementState
import com.neuronovaapps.crucilux.data.UserPreferences
import com.neuronovaapps.crucilux.data.UserPreferencesManager
import com.neuronovaapps.crucilux.navigation.CruciluxNavGraph
import com.neuronovaapps.crucilux.navigation.Screen
import com.neuronovaapps.crucilux.navigation.bottomBarRoutes
import com.neuronovaapps.crucilux.ui.components.AchievementUnlockBanner
import com.neuronovaapps.crucilux.ui.components.CruciluxBottomBar
import com.neuronovaapps.crucilux.ui.theme.CruciluxTheme
import kotlinx.coroutines.delay

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
                textSize = userPreferences.textSize,
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
    val context = LocalContext.current
    val achievementRepository = remember { AchievementRepository.getInstance(context) }
    val unnotifiedAchievements by achievementRepository.observeUnnotifiedUnlocked()
        .collectAsState(initial = emptyList())

    var currentBannerAchievement by remember { mutableStateOf<AchievementState?>(null) }

    // Secuencia obligatoria:
    // logro pendiente -> mostrar notificación -> confirmar presentación/inicio del evento visual -> markNotificationShown(id)
    LaunchedEffect(unnotifiedAchievements) {
        val pending = unnotifiedAchievements.firstOrNull()
        if (pending != null && currentBannerAchievement == null) {
            // 1. Iniciar presentación visual
            currentBannerAchievement = pending
            // 2. Confirmar presentación/inicio del evento visual
            achievementRepository.markNotificationShown(pending.id)
            // 3. Duración visible discreta
            delay(4000)
            currentBannerAchievement = null
        }
    }

    val navController   = rememberNavController()
    val backStackEntry  by navController.currentBackStackEntryAsState()
    val currentRoute    = backStackEntry?.destination?.route

    Box(modifier = Modifier.fillMaxSize()) {
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

        // Notificación visual discreta in-app en la parte superior
        AnimatedVisibility(
            visible = currentBannerAchievement != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .zIndex(100f),
        ) {
            currentBannerAchievement?.let { achievement ->
                AchievementUnlockBanner(achievement = achievement)
            }
        }
    }
}
