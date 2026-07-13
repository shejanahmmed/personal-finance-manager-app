package com.shejan.financebuddy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "finance_buddy_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val SMS_SYNC_CHOICE = stringPreferencesKey("sms_sync_choice")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    /** Emits true if the user has already completed onboarding. */
    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED] ?: false
    }

    /** Call this when the user taps "Get Started". */
    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = true
        }
    }

    /** Emits the user's SMS sync preference: "PENDING", "SYNC_PREVIOUS", "START_NEW", or "DISABLED". */
    val smsSyncChoice: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SMS_SYNC_CHOICE] ?: "PENDING"
    }

    /** Sets the user's SMS sync choice. */
    suspend fun setSmsSyncChoice(choice: String) {
        context.dataStore.edit { prefs ->
            prefs[SMS_SYNC_CHOICE] = choice
        }
    }

    /** Emits the user's theme selection: "SYSTEM", "LIGHT", or "DARK". */
    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "SYSTEM"
    }

    /** Sets the user's theme preference. */
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode
        }
    }
}
