package com.example.noorahlulbaytcompanion

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    companion object {
        private val AZAN_BLOCKING = booleanPreferencesKey("azan_blocking")
        private val BROWSER_BLOCKING = booleanPreferencesKey("browser_blocking")
        private val CITY = stringPreferencesKey("city")
        private val COUNTRY = stringPreferencesKey("country")
        private val PIN_KEY = "pin_hash"
    }
    
    suspend fun setAzanBlocking(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AZAN_BLOCKING] = enabled
        }
    }
    
    suspend fun setBrowserBlocking(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BROWSER_BLOCKING] = enabled
        }
    }
    
    suspend fun setCity(city: String) {
        context.dataStore.edit { preferences ->
            preferences[CITY] = city
        }
    }
    
    suspend fun setCountry(country: String) {
        context.dataStore.edit { preferences ->
            preferences[COUNTRY] = country
        }
    }
    
    suspend fun isAzanBlockingEnabled(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[AZAN_BLOCKING] ?: true
        }.first()
    }
    
    suspend fun isBrowserBlockingEnabled(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[BROWSER_BLOCKING] ?: true
        }.first()
    }
    
    suspend fun getCity(): String {
        return context.dataStore.data.map { preferences ->
            preferences[CITY] ?: "Najaf"
        }.first()
    }
    
    suspend fun getCountry(): String {
        return context.dataStore.data.map { preferences ->
            preferences[COUNTRY] ?: "Iraq"
        }.first()
    }
    
    fun setupPIN(pin: String) {
        val hashedPin = hashPin(pin)
        encryptedPrefs.edit().putString(PIN_KEY, hashedPin).apply()
    }
    
    fun changePIN(oldPin: String, newPin: String): Boolean {
        if (verifyPIN(oldPin)) {
            val hashedNewPin = hashPin(newPin)
            encryptedPrefs.edit().putString(PIN_KEY, hashedNewPin).apply()
            return true
        }
        return false
    }
    
    fun verifyPIN(pin: String): Boolean {
        val storedHash = encryptedPrefs.getString(PIN_KEY, null)
        return storedHash != null && storedHash == hashPin(pin)
    }
    
    fun isPINSet(): Boolean {
        return encryptedPrefs.getString(PIN_KEY, null) != null
    }
    
    private fun hashPin(pin: String): String {
        // Simple hash for demo - in production use proper hashing
        return pin.hashCode().toString()
    }
    
    fun getSettingsFlow(): Flow<Settings> {
        return context.dataStore.data.map { preferences ->
            Settings(
                azanBlocking = preferences[AZAN_BLOCKING] ?: true,
                browserBlocking = preferences[BROWSER_BLOCKING] ?: true,
                city = preferences[CITY] ?: "Najaf",
                country = preferences[COUNTRY] ?: "Iraq"
            )
        }
    }
}

data class Settings(
    val azanBlocking: Boolean,
    val browserBlocking: Boolean,
    val city: String,
    val country: String
) 