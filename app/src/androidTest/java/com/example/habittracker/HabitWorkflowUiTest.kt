package com.example.habittracker

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodes
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitWorkflowUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addEditCompleteUncompleteAndDeleteHabit() {
        composeTestRule.onNodeWithText("Your habits").assertIsDisplayed()
        composeTestRule.onNodeWithText("+").performClick()

        composeTestRule.onNodeWithText("Add Habit").assertIsDisplayed()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Workflow Habit")
        composeTestRule.onNodeWithText("Add", useUnmergedTree = true).performClick()

        composeTestRule.onNodeWithText("Workflow Habit").assertIsDisplayed()

        composeTestRule.onAllNodesWithText("•••").onLast().performClick()
        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNode(hasSetTextAction()).performTextClearance()
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Edited Habit")
        composeTestRule.onNodeWithText("Save", useUnmergedTree = true).performClick()

        composeTestRule.onNodeWithText("Edited Habit").assertIsDisplayed()

        composeTestRule.onAllNodes(isToggleable()).onLast().performClick()
        composeTestRule.onNodeWithText("Completed today").assertIsDisplayed()

        composeTestRule.onAllNodes(isToggleable()).onLast().performClick()
        composeTestRule.onNodeWithText("Not completed yet").assertIsDisplayed()

        composeTestRule.onAllNodesWithText("•••").onLast().performClick()
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.onNodeWithText("Delete habit?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete", useUnmergedTree = true).performClick()

        composeTestRule.onNodeWithText("Edited Habit").assertDoesNotExist()
    }
}
