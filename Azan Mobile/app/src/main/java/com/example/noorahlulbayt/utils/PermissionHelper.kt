package com.example.noorahlulbayt.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {
    private const val STORAGE_PERMISSION_REQUEST_CODE = 1001
    
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:${activity.packageName}")
                activity.startActivityForResult(intent, STORAGE_PERMISSION_REQUEST_CODE)
            } catch (e: Exception) {
                AppLogger.e("PermissionHelper", "Failed to request storage permission", e)
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                activity.startActivityForResult(intent, STORAGE_PERMISSION_REQUEST_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    fun checkAndRequestStoragePermission(activity: Activity): Boolean {
        return if (hasStoragePermission(activity)) {
            AppLogger.i("PermissionHelper", "Storage permission already granted")
            true
        } else {
            AppLogger.w("PermissionHelper", "Storage permission not granted, requesting...")
            requestStoragePermission(activity)
            false
        }
    }
}