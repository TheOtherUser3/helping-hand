# Helping Hand

## Overview
**Helping Hand** is a home management app designed to make everyday organization effortless. Whether you live alone, with roommates, or manage a full household, Helping Hand provides a simple way to track, plan, and maintain all aspects of home life, from shopping lists to cleaning reminders to meal recommendations

The goal of Helping Hand is to take the stress out of home management by keeping everything in one convenient, organized hub.

## Target Users
Helping Hand is built for:
- Homeowners and renters of any kind
- Individuals, roommates, or families
- Anyone who wants an easier way to manage recurring household tasks and information

While users with larger homes or households may get even more use from it, the app is designed to benefit anyone managing a living space.

## Core Features
### 1. **Food Management**
- **Shopping List:** Keep a dynamic shopping list that you can easily add to using text, voice, or barcode scanning.
- **Meal Recommendations:** Get recipe ideas based on ingredients you already have or items on your shopping list. Uses a Spoonacular API call to pull meal data.

### 2. **Cleaning Reminders**
- Track cyclical and deep-cleaning tasks such as maintaining the shower, oven, or refrigerator.
- Stay on top of regular cleaning cycles and infrequent deep cleans with smart reminders.
- Form household groups and assign cleaning tasks to individual family members

### 4. **Appointments**
- Store all appointments, dates, and related information in one place.
- Save phone numbers for appointments without cluttering your contact list.

## Sensors and API Integration
- **Meal API:** Retrieve recipe suggestions based on ingredients.
- **Light Sensor** Option to use light sensor (if available) to enable a dynamic dark mode based on room's lighting


## App Structure
Helping Hand is organized around a central hub with four main widgets:

1. **Food Widget** — links to both the Meals and Shopping List pages.  
2. **Cleaning Reminder Widget** — manages all cleaning cycles.
3. **Contacts Widget** - Store rare contacts without cluttering up your main contacts list (school teachers, group members you'll never speak to again after a month, pediatricians)
4. **Appointments Widget** — organizes appointments and related info.  


Each widget connects to its own page, with a return button leading back to the central hub. The hub shows a brief summary of key data for each area.

Example summaries:
- "24 items on your shopping list.”
- "Next [cleaning reminder] due in 3 days

## Team Roles
**Mia Batista** – Creative and UI/UX Lead  
Responsible for UI/UX design and implementation. Leads layout and visual design.

**Dawson Maska** – Backend Lead  
Focuses on algorithm design, navigation, database management, API/sensor integration, and all other backend logic. Also assists in UI implementation to maintain balanced workload.

## Conclusion
Helping Hand is a personal home assistant that brings order to the chaos of daily life. By centralizing all essential household management tools into one streamlined experience, Helping Hand gives users the clarity to stay organized and stress-free.

Keeping your home in order shouldn’t feel like a full-time job — everyone could use a **Helping Hand**.

---

UPDATE 11/11
- To build, add Spoonacular API key into the local.properties
- Current features include the Shopping Cart fully functioning widget and the appearance of the dashboard.  Shopping Cart page is linked to full functioning Meals Page, which suggests meal recommendations fetched from selected items in the shopping list via Spoonacular API call.


UPDATE 12/02
- Current features include all previous features, plus a contacts page with the ability to add a contact with email and/or phone number, a settings page with the option of adding a dark mode, as well as a dynamic dark mode option using the light sensor if their phone has it built in (only shows up if they have one), and a cleaning reminder page to let the user know when they are due to perform their regularly scheduled cleanings that they input (sends notifications to users when they have a cleaning due).  Meal page now has option to add all missing ingredients to shopping list, and clicking a meal brings the user to the Spoonacular page (to access the recipe)
-In Progress Features: Testing, Settings App, (add reactiveness to Contacts widget on dashboard)
-Pending: Onboarding, Login/Registration, (Add request popup for notification access), Startup Screen
- Testing Strategy:

1. **Logging-based debugging:**  
   We added structured logging for API calls, database operations, navigation, ViewModel state updates, sensor values, and Worker tasks. This made it easy to trace app behavior in Logcat.

2. **Crash handling:**  
   Error-prone operations use try/catch blocks. Failures are logged with context and stack traces so crashes are easy to diagnose.

3. **Targeted Compose UI tests:**  
   We added UI tests for core flows such as opening screens, navigating between them, and adding new items through dialogs. (although there is one mistake with the tests we need to fix at the moment)

UPDATE 12/16
Build Instructions: Add SPOONACULAR_API_KEY into the local.properties

Reflection Link:
https://docs.google.com/document/d/1522Dyl-wVY_XefK2DUq_VAcEYfpAbZ1vMk5SGXo3dQk/edit?tab=t.0#heading=h.rslytixogkzo


**Signed,**  
*The Mia Dawgs*
