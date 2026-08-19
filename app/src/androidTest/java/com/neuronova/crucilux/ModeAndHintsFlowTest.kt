package com.neuronova.crucilux

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neuronova.crucilux.data.repository.CrosswordProgressRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModeAndHintsFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mode_is_selected_before_board_and_fourth_hint_requires_confirmation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            CrosswordProgressRepository.getInstance(context)
                .resetBoardProgress("7X7-01", "Cultura general")
        }

        composeRule.onNodeWithText("Comenzar").performClick()
        composeRule.onNodeWithContentDescription("Jugar, navegación principal").performClick()
        composeRule.onNodeWithText("Cultura general").performClick()
        composeRule.onNodeWithText("01").performClick()

        composeRule.onNodeWithText("¿Cómo quieres jugar?").assertIsDisplayed()
        composeRule.onNodeWithText("CLÁSICA").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("¿Cómo quieres jugar?").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Modo de partida bloqueado: Clásica").assertIsDisplayed()

        repeat(3) {
            composeRule.onNodeWithText("Pista").performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithText("Pistas: ○ ○ ○").assertIsDisplayed()

        composeRule.onNodeWithText("Pista").performClick()
        composeRule.onNodeWithText("Pista adicional").assertIsDisplayed()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.onNodeWithText("Pistas: ○ ○ ○").assertIsDisplayed()

        composeRule.onNodeWithText("Pista").performClick()
        composeRule.onNodeWithText("Usar pista").performClick()
        composeRule.onNodeWithText("XP posibles: 50").assertIsDisplayed()
    }
}
