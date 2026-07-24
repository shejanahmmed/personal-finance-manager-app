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
        private val PROFILE_NAME = stringPreferencesKey("profile_name")
        private val PROFILE_IMAGE_PATH = stringPreferencesKey("profile_image_path")
        private val HIDE_CARD_BALANCES = booleanPreferencesKey("hide_card_balances")
        private val BLOCK_SCREENSHOTS = booleanPreferencesKey("block_screenshots")
        private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val APP_LOCK_TYPE = stringPreferencesKey("app_lock_type")
        private val APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        private val AUTO_LOCK_TIMEOUT = stringPreferencesKey("auto_lock_timeout")
    }

    /** Emits whether screenshots and screen recording are blocked. Defaults to false. */
    val blockScreenshots: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BLOCK_SCREENSHOTS] ?: false
    }

    /** Sets the user's screenshot & screen recording blocking preference. */
    suspend fun setBlockScreenshots(block: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[BLOCK_SCREENSHOTS] = block
        }
    }

    /** Emits whether App Lock security is enabled. Defaults to false. */
    val isAppLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[APP_LOCK_ENABLED] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[APP_LOCK_ENABLED] = enabled
        }
    }

    /** Emits selected Lock Type: "PIN" or "FINGERPRINT". Defaults to "PIN". */
    val appLockType: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[APP_LOCK_TYPE] ?: "PIN"
    }

    suspend fun setAppLockType(type: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_LOCK_TYPE] = type
        }
    }

    /** Emits saved 6-digit PIN code. Defaults to "". */
    val appLockPin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[APP_LOCK_PIN] ?: ""
    }

    suspend fun setAppLockPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[APP_LOCK_PIN] = pin
        }
    }

    /** Emits Auto-Lock timeout setting: "IMMEDIATELY", "1_MIN", "3_MIN", or "5_MIN". Defaults to "IMMEDIATELY". */
    val autoLockTimeout: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[AUTO_LOCK_TIMEOUT] ?: "IMMEDIATELY"
    }

    suspend fun setAutoLockTimeout(timeout: String) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_LOCK_TIMEOUT] = timeout
        }
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

    /** Emits the user's profile name. Defaults to "User". */
    val profileName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PROFILE_NAME] ?: "User"
    }

    /** Sets the user's profile name. */
    suspend fun setProfileName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[PROFILE_NAME] = name
        }
    }

    /** Emits the user's profile image path. Defaults to empty string. */
    val profileImagePath: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PROFILE_IMAGE_PATH] ?: ""
    }

    /** Sets the user's profile image path. */
    suspend fun setProfileImagePath(path: String) {
        context.dataStore.edit { prefs ->
            prefs[PROFILE_IMAGE_PATH] = path
        }
    }

    /** Emits whether card balances should be hidden by default. Defaults to false. */
    val hideCardBalances: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[HIDE_CARD_BALANCES] ?: false
    }

    /** Sets the user's hide card balances preference. */
    suspend fun setHideCardBalances(hide: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[HIDE_CARD_BALANCES] = hide
        }
    }
}
