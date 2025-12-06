package com.example.helpinghand

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class ScreenInteractionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    // ------------------------------------------------------------
    // 1. Add item to shopping list
    // ------------------------------------------------------------
    @Test
    fun canAddItemToShoppingList() {
        composeTestRule.onNodeWithTag("tile_shopping").performClick()
        composeTestRule.onNodeWithTag("shopping_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("shopping_add_item").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("shopping_add_dialog_field")
            .performTextInput("Milk")

        composeTestRule.onNodeWithTag("shopping_add_dialog_confirm").performClick()

        composeTestRule.onNodeWithText("Milk").assertIsDisplayed()
    }

    // ------------------------------------------------------------
    // 2. Cleaning dialog opens
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
    // 3. Contacts dialog opens (FIXED)
    // ------------------------------------------------------------
    @Test
    fun canOpenContactsAddDialog() {
        composeTestRule.onNodeWithTag("tile_contacts").performClick()
        composeTestRule.onNodeWithTag("contacts_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("contacts_add_button").performClick()

        composeTestRule.onNodeWithText("Add Contact").assertIsDisplayed()
    }

    // ------------------------------------------------------------
    // 4. Settings screen opens
    // ------------------------------------------------------------
    @Test
    fun settingsScreenOpens() {
        composeTestRule.onNodeWithTag("dashboard_settings_icon").performClick()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()

        composeTestRule.onNodeWithTag("switch_dark_mode").assertExists()
    }
}
