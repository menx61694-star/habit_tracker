package com.example.habittracker

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsTodayProgressAndDefaultHabits() {
        composeTestRule.onNodeWithText("Your habits").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today's progress").assertIsDisplayed()
        composeTestRule.onNodeWithText("Drink Water").assertIsDisplayed()
        composeTestRule.onNodeWithText("Walk 30 Minutes").assertIsDisplayed()
    }

    @Test
    fun bottomNavigationOpensHistoryAndSettings() {
        composeTestRule.onNodeWithText("History").performClick()
        composeTestRule.onNodeWithText("History").assertIsDisplayed()

        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeTestRule.onNodeWithText("Backup & restore").assertIsDisplayed()
    }
}
