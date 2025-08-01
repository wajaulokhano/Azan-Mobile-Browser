package com.example.noorahlulbaytcompanion

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class BrowserAccessibilityService : AccessibilityService() {
    
    private val blockedBrowsers = listOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.sec.android.app.sbrowser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.brave.browser",
        "com.naver.whale",
        "com.UCMobile.intl"
    )
    
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString()
            
            if (packageName != null && blockedBrowsers.contains(packageName)) {
                handleBlockedBrowser(packageName)
            }
        }
    }
    
    private fun handleBlockedBrowser(packageName: String) {
        // Show toast message
        Toast.makeText(
            this,
            "Use Noor-e-AhlulBayt browser instead",
            Toast.LENGTH_SHORT
        ).show()
        
        // Minimize the blocked browser
        performGlobalAction(GLOBAL_ACTION_HOME)
        
        // Launch our browser
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setPackage("com.example.noorahlulbayt")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            // Browser app not installed, show message
            Toast.makeText(
                this,
                "Please install Noor-e-AhlulBayt browser",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onInterrupt() {
        // Not needed for this service
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "Browser monitoring service started", Toast.LENGTH_SHORT).show()
    }
} 