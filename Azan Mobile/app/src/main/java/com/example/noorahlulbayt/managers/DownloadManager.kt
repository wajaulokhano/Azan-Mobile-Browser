package com.example.noorahlulbayt.managers

import android.content.Context
import android.os.Environment
import androidx.work.*
import com.example.noorahlulbayt.database.BrowserDatabase
import com.example.noorahlulbayt.models.Download
import com.example.noorahlulbayt.models.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit

class DownloadManager(private val context: Context) {
    
    private val database = BrowserDatabase.getDatabase(context)
    private val workManager = WorkManager.getInstance(context)
    
    fun getAllDownloads(): Flow<List<Download>> {
        return database.downloadDao().getAllDownloads()
    }
    
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<Download>> {
        return database.downloadDao().getDownloadsByStatus(status)
    }
    
    suspend fun startDownload(url: String, filename: String? = null): String {
        val downloadId = UUID.randomUUID().toString()
        val actualFilename = filename ?: getFilenameFromUrl(url)
        
        val download = Download(
            id = downloadId,
            url = url,
            filename = actualFilename,
            status = DownloadStatus.PENDING
        )
        
        database.downloadDao().insertDownload(download)
        
        // Start download work
        val downloadWork = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(
                "download_id" to downloadId,
                "url" to url,
                "filename" to actualFilename
            ))
            .build()
        
        workManager.enqueue(downloadWork)
        
        return downloadId
    }
    
    suspend fun cancelDownload(downloadId: String) {
        val download = database.downloadDao().getDownload(downloadId)
        download?.let {
            val updatedDownload = it.copy(status = DownloadStatus.CANCELLED)
            database.downloadDao().updateDownload(updatedDownload)
        }
        
        // Cancel work
        workManager.cancelWorkById(UUID.fromString(downloadId))
    }
    
    suspend fun deleteDownload(download: Download) {
        // Delete file if exists
        if (download.filePath.isNotEmpty()) {
            File(download.filePath).delete()
        }
        
        database.downloadDao().deleteDownload(download)
    }
    
    suspend fun clearCompletedDownloads() {
        database.downloadDao().deleteDownloadsByStatus(DownloadStatus.COMPLETED)
    }
    
    private fun getFilenameFromUrl(url: String): String {
        return try {
            val urlObj = URL(url)
            val path = urlObj.path
            if (path.isNotEmpty()) {
                val filename = path.substring(path.lastIndexOf('/') + 1)
                if (filename.isNotEmpty()) filename else "download"
            } else {
                "download"
            }
        } catch (e: Exception) {
            "download"
        }
    }
}

class DownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    
    private val database = BrowserDatabase.getDatabase(context)
    
    override fun doWork(): Result {
        val downloadId = inputData.getString("download_id") ?: return Result.failure()
        val url = inputData.getString("url") ?: return Result.failure()
        val filename = inputData.getString("filename") ?: return Result.failure()
        
        return try {
            // Update status to downloading
            runBlocking { updateDownloadStatus(downloadId, DownloadStatus.DOWNLOADING) }
            
            // Create downloads directory
            val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AzanMobileBrowser")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val file = File(downloadsDir, filename)
            
            // Download file
            val connection = URL(url).openConnection()
            val fileSize = connection.contentLength.toLong()
            
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            val inputStream = connection.getInputStream()
            val outputStream = FileOutputStream(file)
            
            val buffer = ByteArray(8192)
            var downloadedBytes = 0L
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloadedBytes += bytesRead
                
                // Update progress
                runBlocking { updateDownloadProgress(downloadId, downloadedBytes, fileSize) }
                
                // Check if cancelled
                if (isStopped) {
                    inputStream.close()
                    outputStream.close()
                    file.delete()
                    runBlocking { updateDownloadStatus(downloadId, DownloadStatus.CANCELLED) }
                    return Result.failure()
                }
            }
            
            inputStream.close()
            outputStream.close()
            
            // Update status to completed
            runBlocking { updateDownloadStatus(downloadId, DownloadStatus.COMPLETED, file.absolutePath) }
            
            Result.success()
        } catch (e: Exception) {
            runBlocking { updateDownloadStatus(downloadId, DownloadStatus.FAILED) }
            Result.failure()
        }
    }
    
    private suspend fun updateDownloadStatus(downloadId: String, status: DownloadStatus, filePath: String = "") {
        val download = database.downloadDao().getDownload(downloadId)
        download?.let {
            val updatedDownload = it.copy(
                status = status,
                filePath = filePath,
                dateCompleted = if (status == DownloadStatus.COMPLETED) Date() else null
            )
            database.downloadDao().updateDownload(updatedDownload)
        }
    }
    
    private suspend fun updateDownloadProgress(downloadId: String, downloadedBytes: Long, fileSize: Long) {
        val download = database.downloadDao().getDownload(downloadId)
        download?.let {
            val updatedDownload = it.copy(
                downloadedBytes = downloadedBytes,
                fileSize = fileSize
            )
            database.downloadDao().updateDownload(updatedDownload)
        }
    }
} 