package com.example.helpinghand

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class ScreenInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ------------------------------------------------------------
    // 1. Shopping: screen opens + add dialog opens
    // ------------------------------------------------------------
    @Test
    fun shoppingScreenAndDialogOpen() {
        composeTestRule.onNodeWithTag("tile_shopping").performClick()
        composeTestRule.onNodeWithTag("shopping_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("shopping_add_item").performClick()
        composeTestRule.waitForIdle()

        // Avoid asserting inserted list contents (Room/Firebase timing).
        // Just confirm the dialog appears.
        composeTestRule.onNodeWithText("Add New Item").assertIsDisplayed()
    }

    // ------------------------------------------------------------
    // 2. Cleaning dialog
    // ------------------------------------------------------------
    @Test
    fun canOpenCleaningAddDialog() {
        composeTestRule.onNodeWithTag("tile_cleaning").performClick()
        composeTestRule.onNodeWithTag("cleaning_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("cleaning_add_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("New cleaning task").assertIsDisplayed()
    }

    // ------------------------------------------------------------
    // 3. Contacts
    // ------------------------------------------------------------
    @Test
    fun canOpenContactsAddDialog() {
        composeTestRule.onNodeWithTag("tile_contacts").performClick()
        composeTestRule.onNodeWithTag("contacts_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("contacts_add_button").performClick()

        composeTestRule.onNodeWithText("Add Contact").assertIsDisplayed()
    }

    // ------------------------------------------------------------
    // 4. Settings screen
    // ------------------------------------------------------------
    @Test
    fun settingsScreenOpens() {
        composeTestRule.onNodeWithTag("dashboard_settings_icon").performClick()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("switch_dark_mode").assertExists()
    }

    // ------------------------------------------------------------
    // 5. Appointments:
    // ------------------------------------------------------------
    @Test
    fun appointmentsScreenOpens() {
        composeTestRule.onNodeWithTag("tile_appointments").performClick()
        composeTestRule.onNodeWithTag("appointments_screen").assertIsDisplayed()

        composeTestRule.onNodeWithText("Doctor Appointments").assertIsDisplayed()
    }
}
