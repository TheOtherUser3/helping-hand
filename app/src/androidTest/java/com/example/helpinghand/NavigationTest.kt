package com.example.helpinghand

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MainNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dashboardIsShownOnLaunch() {
        composeTestRule
            .onNodeWithTag("dashboard_screen")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("dashboard_title")
            .assertIsDisplayed()
    }

    @Test
    fun canNavigateFromDashboardToShoppingAndBack() {
        // Tap the shopping tile
        composeTestRule
            .onNodeWithTag("tile_shopping")
            .performClick()

        // Now shopping screen should show
        composeTestRule
            .onNodeWithTag("shopping_screen")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("shopping_title")
            .assertIsDisplayed()

        // Back to dashboard
        composeTestRule
            .onNodeWithTag("shopping_back")
            .performClick()

        composeTestRule
            .onNodeWithTag("dashboard_screen")
            .assertIsDisplayed()
    }

    @Test
    fun canNavigateFromDashboardToCleaning() {
        composeTestRule
            .onNodeWithTag("tile_cleaning")
            .performClick()

        composeTestRule
            .onNodeWithTag("cleaning_screen")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("cleaning_title")
            .assertIsDisplayed()
    }

    @Test
    fun canNavigateFromDashboardToContacts() {
        composeTestRule
            .onNodeWithTag("tile_contacts")
            .performClick()

        composeTestRule
            .onNodeWithTag("contacts_screen")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("contacts_title")
            .assertIsDisplayed()
    }

    @Test
    fun canNavigateFromDashboardToSettings() {
        composeTestRule
            .onNodeWithTag("dashboard_settings_icon")
            .performClick()

        composeTestRule
            .onNodeWithTag("settings_screen")
            .assertIsDisplayed()
    }
}
