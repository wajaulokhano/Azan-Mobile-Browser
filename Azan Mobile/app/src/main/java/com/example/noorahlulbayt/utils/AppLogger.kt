package com.example.noorahlulbayt.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

object AppLogger {
    private const val TAG = "AzanBrowser"
    private const val LOG_DIR = "AzanBrowserLogs"
    private const val MAX_LOG_FILES = 5
    private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
    
    private var logFile: File? = null
    private var context: Context? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    fun initialize(context: Context) {
        this.context = context
        setupLogFile()
        setupCrashHandler()
        i("AppLogger", "Logger initialized successfully")
    }
    
    private fun setupLogFile() {
        try {
            // Create logs directory in public Documents folder
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val logDir = File(documentsDir, LOG_DIR)
            
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            // Create today's log file
            val today = fileDateFormat.format(Date())
            logFile = File(logDir, "azan_browser_$today.log")
            
            // Clean up old log files
            cleanupOldLogs(logDir)
            
            // Write startup message
            writeToFile("=== Azan Browser Started at ${dateFormat.format(Date())} ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup log file", e)
        }
    }
    
    private fun cleanupOldLogs(logDir: File) {
        try {
            val logFiles = logDir.listFiles { file -> 
                file.name.startsWith("azan_browser_") && file.name.endsWith(".log")
            }?.sortedByDescending { it.lastModified() }
            
            logFiles?.drop(MAX_LOG_FILES)?.forEach { file ->
                file.delete()
                Log.d(TAG, "Deleted old log file: ${file.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old logs", e)
        }
    }
    
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            try {
                e("CRASH", "Uncaught exception in thread ${thread.name}", exception)
                
                // Write detailed crash info
                writeToFile("\n=== CRASH REPORT ===")
                writeToFile("Time: ${dateFormat.format(Date())}")
                writeToFile("Thread: ${thread.name}")
                writeToFile("Exception: ${exception.javaClass.simpleName}")
                writeToFile("Message: ${exception.message}")
                writeToFile("Stack Trace:")
                
                val stackTrace = exception.stackTraceToString()
                writeToFile(stackTrace)
                writeToFile("=== END CRASH REPORT ===\n")
                
                // Also log device info
                logDeviceInfo()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log crash", e)
            }
            
            // Call the default handler
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
    
    private fun logDeviceInfo() {
        try {
            writeToFile("=== DEVICE INFO ===")
            writeToFile("Android Version: ${android.os.Build.VERSION.RELEASE}")
            writeToFile("API Level: ${android.os.Build.VERSION.SDK_INT}")
            writeToFile("Device: ${android.os.Build.DEVICE}")
            writeToFile("Model: ${android.os.Build.MODEL}")
            writeToFile("Manufacturer: ${android.os.Build.MANUFACTURER}")
            writeToFile("Brand: ${android.os.Build.BRAND}")
            
            context?.let { ctx ->
                val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                writeToFile("App Version: ${packageInfo.versionName}")
                writeToFile("Version Code: ${packageInfo.longVersionCode}")
            }
            
            // Memory info
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() / 1024 / 1024
            val totalMemory = runtime.totalMemory() / 1024 / 1024
            val freeMemory = runtime.freeMemory() / 1024 / 1024
            val usedMemory = totalMemory - freeMemory
            
            writeToFile("Max Memory: ${maxMemory}MB")
            writeToFile("Total Memory: ${totalMemory}MB")
            writeToFile("Used Memory: ${usedMemory}MB")
            writeToFile("Free Memory: ${freeMemory}MB")
            writeToFile("=== END DEVICE INFO ===\n")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log device info", e)
        }
    }
    
    fun d(tag: String, message: String) {
        Log.d(TAG, "[$tag] $message")
        writeToFile("DEBUG [$tag] $message")
    }
    
    fun i(tag: String, message: String) {
        Log.i(TAG, "[$tag] $message")
        writeToFile("INFO [$tag] $message")
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(TAG, "[$tag] $message", throwable)
        writeToFile("WARN [$tag] $message")
        throwable?.let { writeToFile("Exception: ${it.stackTraceToString()}") }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$tag] $message", throwable)
        writeToFile("ERROR [$tag] $message")
        throwable?.let { writeToFile("Exception: ${it.stackTraceToString()}") }
    }
    
    fun logWebViewError(url: String, error: String, errorCode: Int = -1) {
        val message = "WebView Error - URL: $url, Error: $error, Code: $errorCode"
        e("WebView", message)
    }
    
    fun logTabOperation(operation: String, tabIndex: Int, tabCount: Int, url: String = "") {
        val message = "Tab $operation - Index: $tabIndex, Total: $tabCount, URL: $url"
        d("TabManager", message)
    }
    
    fun logFilteringResult(url: String, isBlocked: Boolean, reason: String) {
        val status = if (isBlocked) "BLOCKED" else "ALLOWED"
        val message = "Content $status - URL: $url, Reason: $reason"
        i("ContentFilter", message)
    }
    
    fun logPrayerTimeEvent(event: String, details: String = "") {
        val message = "Prayer Time Event: $event $details"
        i("PrayerTime", message)
    }
    
    private fun writeToFile(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                logFile?.let { file ->
                    // Check file size and rotate if needed
                    if (file.length() > MAX_LOG_SIZE) {
                        rotateLogFile()
                    }
                    
                    FileWriter(file, true).use { writer ->
                        val timestamp = dateFormat.format(Date())
                        writer.appendLine("$timestamp: $message")
                        writer.flush()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to log file", e)
            }
        }
    }
    
    private fun rotateLogFile() {
        try {
            val currentFile = logFile ?: return
            val parent = currentFile.parentFile ?: return
            
            // Rename current file with timestamp
            val timestamp = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
            val rotatedName = currentFile.name.replace(".log", "_$timestamp.log")
            val rotatedFile = File(parent, rotatedName)
            
            currentFile.renameTo(rotatedFile)
            
            // Create new log file
            val today = fileDateFormat.format(Date())
            logFile = File(parent, "azan_browser_$today.log")
            
            writeToFile("=== Log file rotated at ${dateFormat.format(Date())} ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log file", e)
        }
    }
    
    fun getLogFiles(): List<File> {
        return try {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val logDir = File(documentsDir, LOG_DIR)
            
            logDir.listFiles { file -> 
                file.name.startsWith("azan_browser_") && file.name.endsWith(".log")
            }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get log files", e)
            emptyList()
        }
    }
    
    fun getLogDirectory(): String {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(documentsDir, LOG_DIR).absolutePath
    }
    
    fun clearLogs() {
        try {
            getLogFiles().forEach { file ->
                file.delete()
            }
            i("AppLogger", "All log files cleared")
        } catch (e: Exception) {
            e("AppLogger", "Failed to clear logs", e)
        }
    }
}