package com.example.noorahlulbaytcompanion

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class AzanBlockWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    
    override fun doWork(): Result {
        return try {
            // Send broadcast to block browser
            val blockIntent = Intent("com.example.noorahlulbayt.BLOCK_BROWSER")
            context.sendBroadcast(blockIntent)
            
            // Show notification
            Toast.makeText(
                context,
                "Prayer time started. Browser is now blocked for 10 minutes.",
                Toast.LENGTH_LONG
            ).show()
            
            // Schedule unblock after 10 minutes
            scheduleUnblock()
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private fun scheduleUnblock() {
        val unblockWork = androidx.work.OneTimeWorkRequestBuilder<AzanUnblockWorker>()
            .setInitialDelay(10, TimeUnit.MINUTES)
            .build()
        
        androidx.work.WorkManager.getInstance(context).enqueue(unblockWork)
    }
}

class AzanUnblockWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    
    override fun doWork(): Result {
        return try {
            // Send broadcast to unblock browser
            val unblockIntent = Intent("com.example.noorahlulbayt.UNBLOCK_BROWSER")
            context.sendBroadcast(unblockIntent)
            
            // Show notification
            Toast.makeText(
                context,
                "Prayer time ended. Browser is now available.",
                Toast.LENGTH_SHORT
            ).show()
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
} 