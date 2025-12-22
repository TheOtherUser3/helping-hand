# Helping Hand

Helping Hand is an Android home management app that keeps the everyday chaos of a living space in one organized hub. It’s built for solo living, roommates, and households, with real time syncing across members so everyone stays on the same page.

## Highlights
- One dashboard that links to the four core areas: Food, Cleaning, Contacts, and Appointments
- Household groups with Firebase authentication and Firestore sync
- Offline first data via Room, with cloud sync when online
- Notifications for cleaning reminders (Android 13+ permission request supported)

## Core Features

### Food Management
- **Shopping List**
  - Add, check, and delete shopping items
  - Syncs across household members in real time
- **Meals**
  - Recipe recommendations using the Spoonacular API based on selected shopping items
  - Add missing ingredients from a recipe directly into the shopping list
  - Open a recipe in the browser from the app

### Cleaning Reminders
- Create recurring cleaning tasks with an interval in days
- Tasks show due status 
- Assign tasks to household members (or leave unassigned)
- Reassign tasks at any time
- Sticky section headers group tasks by:
  - Assigned to you
  - Unassigned
  - Other household members
  - (all only shown when the household has more than one member)
- Background notifications for tasks that are due

### Contacts
- Store contacts inside the app without cluttering the device address book
- Add contacts with a name and optional phone number and/or email
- Syncs across household members in real time

### Appointments
- Store appointments and related notes in one place
- Save phone numbers for appointments without adding them to the device contacts
- Syncs across household members in real time

## Household Sync and Authentication
- Users can register and log in with Firebase Authentication
- Data is organized by household so multiple users can share the same lists and reminders
- Household members can be added by unique code, and users can join or leave households from Settings

## Settings
- Manual dark mode toggle
- Optional dynamic theme support
- Optional light sensor based dynamic dark mode (only shown if the device has a light sensor)
- Onboarding shown once per install

## Notifications
- Cleaning reminder notifications are delivered via a periodic WorkManager job
- On Android 13+, the app requests `POST_NOTIFICATIONS` permission once after onboarding  and registration is completed

## Tech
- Kotlin, Jetpack Compose, Material 3
- Room (local persistence)
- Firebase Authentication and Cloud Firestore (household sync)
- WorkManager (scheduled reminders)
- DataStore (settings and onboarding state)
- Spoonacular API (meal recommendations)

## Setup

### Spoonacular API Key
Add your Spoonacular API key to `local.properties`:

```properties
SPOONACULAR_API_KEY=YOUR_KEY_HERE
