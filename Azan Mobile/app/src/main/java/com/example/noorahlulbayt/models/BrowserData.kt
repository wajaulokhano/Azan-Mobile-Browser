package com.example.noorahlulbayt.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey val url: String,
    val title: String,
    val icon: String = "",
    val dateAdded: Date = Date()
)

@Entity(tableName = "downloads")
data class Download(
    @PrimaryKey val id: String,
    val url: String,
    val filename: String,
    val fileSize: Long = 0,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val dateStarted: Date = Date(),
    val dateCompleted: Date? = null,
    val filePath: String = ""
)

@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val dateVisited: Date = Date(),
    val visitCount: Int = 1
)

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
} 