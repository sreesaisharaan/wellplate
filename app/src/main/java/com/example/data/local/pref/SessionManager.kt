package com.example.data.local.pref

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SessionManager(private val context: Context) {
    companion object {
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_HAS_ONBOARDED = booleanPreferencesKey("onboarded")
    }

    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_LOGGED_IN] ?: false }
    val userEmailFlow: Flow<String> = context.dataStore.data.map { it[KEY_USER_EMAIL] ?: "" }
    val userNameFlow: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "" }
    val hasOnboardedFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAS_ONBOARDED] ?: false }

    suspend fun saveSession(email: String, name: String, hasOnboarded: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_USER_NAME] = name
            prefs[KEY_HAS_ONBOARDED] = hasOnboarded
        }
    }

    suspend fun markOnboardingComplete() {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_ONBOARDED] = true
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return context.dataStore.data.first()[KEY_IS_LOGGED_IN] == true
    }

    suspend fun hasOnboarded(): Boolean {
        return context.dataStore.data.first()[KEY_HAS_ONBOARDED] ?: false
    }
}
