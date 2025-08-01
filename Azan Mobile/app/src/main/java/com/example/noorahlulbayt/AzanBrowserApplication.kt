package com.example.noorahlulbayt

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.noorahlulbayt.utils.AppLogger

class AzanBrowserApplication : Application(), Configuration.Provider {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logger first
        AppLogger.initialize(this)
        AppLogger.i("Application", "AzanBrowserApplication starting")
        
        try {
            // Initialize WorkManager manually
            val config = workManagerConfiguration
            WorkManager.initialize(this, config)
            AppLogger.i("Application", "WorkManager initialized successfully")
            
        } catch (e: Exception) {
            AppLogger.e("Application", "Failed to initialize WorkManager", e)
        }
        
        AppLogger.i("Application", "AzanBrowserApplication initialization complete")
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}