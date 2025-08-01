package com.example.noorahlulbayt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AzanBlockReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "com.example.noorahlulbayt.BLOCK_BROWSER" -> {
                handleAzanBlock(context)
            }
            "com.example.noorahlulbayt.UNBLOCK_BROWSER" -> {
                handleAzanUnblock(context)
            }
        }
    }
    
    private fun handleAzanBlock(context: Context?) {
        context?.let {
            // Show blocking message
            Toast.makeText(
                it,
                "Browser is not available during prayer time for 10 minutes",
                Toast.LENGTH_LONG
            ).show()
            
            // Send local broadcast to BrowserActivity
            val localIntent = Intent("com.example.noorahlulbayt.BLOCK_BROWSER")
            it.sendBroadcast(localIntent)
        }
    }
    
    private fun handleAzanUnblock(context: Context?) {
        context?.let {
            // Show unblocking message
            Toast.makeText(
                it,
                "Prayer time ended. Browser is now available.",
                Toast.LENGTH_SHORT
            ).show()
            
            // Send local broadcast to BrowserActivity
            val localIntent = Intent("com.example.noorahlulbayt.UNBLOCK_BROWSER")
            it.sendBroadcast(localIntent)
        }
    }
} 