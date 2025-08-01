package com.example.noorahlulbayt.database

import androidx.room.TypeConverter
import com.example.noorahlulbayt.models.DownloadStatus
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromDownloadStatus(status: DownloadStatus): String {
        return status.name
    }

    @TypeConverter
    fun toDownloadStatus(status: String): DownloadStatus {
        return DownloadStatus.valueOf(status)
    }
}
