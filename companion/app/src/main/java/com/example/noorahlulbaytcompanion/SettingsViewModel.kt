package com.example.noorahlulbaytcompanion

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _azanBlockingEnabled = MutableStateFlow(true)
    val azanBlockingEnabled: StateFlow<Boolean> = _azanBlockingEnabled.asStateFlow()
    
    private val _browserBlockingEnabled = MutableStateFlow(true)
    val browserBlockingEnabled: StateFlow<Boolean> = _browserBlockingEnabled.asStateFlow()
    
    private val _prayerTimes = MutableStateFlow<List<PrayerTime>>(emptyList())
    val prayerTimes: StateFlow<List<PrayerTime>> = _prayerTimes.asStateFlow()
    
    private val _showPINSetup = MutableStateFlow(false)
    val showPINSetup: StateFlow<Boolean> = _showPINSetup.asStateFlow()
    
    private val _showPINChange = MutableStateFlow(false)
    val showPINChange: StateFlow<Boolean> = _showPINChange.asStateFlow()
    
    private val settingsManager = SettingsManager(application)
    private val prayerTimesWorker = PrayerTimesWorker(application)
    
    init {
        loadSettings()
        loadPrayerTimes()
    }
    
    fun setAzanBlocking(enabled: Boolean) {
        _azanBlockingEnabled.value = enabled
        viewModelScope.launch {
            settingsManager.setAzanBlocking(enabled)
        }
    }
    
    fun setBrowserBlocking(enabled: Boolean) {
        _browserBlockingEnabled.value = enabled
        viewModelScope.launch {
            settingsManager.setBrowserBlocking(enabled)
        }
    }
    
    fun refreshPrayerTimes() {
        viewModelScope.launch {
            prayerTimesWorker.fetchPrayerTimes()
            loadPrayerTimes()
        }
    }
    
    fun showPINSetup() {
        _showPINSetup.value = true
    }
    
    fun hidePINSetup() {
        _showPINSetup.value = false
    }
    
    fun showPINChange() {
        _showPINChange.value = true
    }
    
    fun hidePINChange() {
        _showPINChange.value = false
    }
    
    fun setupPIN(pin: String) {
        viewModelScope.launch {
            settingsManager.setupPIN(pin)
            hidePINSetup()
        }
    }
    
    fun changePIN(oldPin: String, newPin: String): Boolean {
        return viewModelScope.run {
            val success = settingsManager.changePIN(oldPin, newPin)
            if (success) {
                hidePINChange()
            }
            success
        }
    }
    
    fun verifyPIN(pin: String): Boolean {
        return settingsManager.verifyPIN(pin)
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            _azanBlockingEnabled.value = settingsManager.isAzanBlockingEnabled()
            _browserBlockingEnabled.value = settingsManager.isBrowserBlockingEnabled()
        }
    }
    
    private fun loadPrayerTimes() {
        viewModelScope.launch {
            val times = prayerTimesWorker.getPrayerTimes()
            _prayerTimes.value = times.map { (name, time) ->
                PrayerTime(name = name, time = time)
            }
        }
    }
}

data class PrayerTime(
    val name: String,
    val time: String
) 