package com.neuronova.crucilux

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavigationRegressionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottom_navigation_returns_to_real_home_repeatedly() {
        composeRule.onNodeWithText("Comenzar").performClick()
        assertHomeSelected()

        // Inicio → Jugar → Inicio
        openPlay()
        openHome()

        // Inicio → Progreso → Inicio
        openProgress()
        openHome()

        // Jugar → Progreso → Inicio
        openPlay()
        openProgress()
        openHome()

        // Progreso → Jugar → Inicio, repetido para detectar desincronización.
        repeat(3) {
            openProgress()
            openPlay()
            openHome()
        }
    }

    private fun openHome() {
        composeRule.onNodeWithContentDescription("Inicio, navegación principal").performClick()
        composeRule.waitForIdle()
        assertHomeSelected()
    }

    private fun openPlay() {
        composeRule.onNodeWithContentDescription("Jugar, navegación principal").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Categorías").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Jugar, navegación principal").assertIsSelected()
    }

    private fun openProgress() {
        composeRule.onNodeWithContentDescription("Progreso, navegación principal").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Progreso").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Progreso, navegación principal").assertIsSelected()
    }

    private fun assertHomeSelected() {
        composeRule.onNodeWithContentDescription("Inicio, navegación principal").assertIsSelected()
    }
}
