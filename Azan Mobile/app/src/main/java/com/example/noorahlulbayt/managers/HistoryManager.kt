package com.example.noorahlulbayt.managers

import android.content.Context
import com.example.noorahlulbayt.database.BrowserDatabase
import com.example.noorahlulbayt.models.HistoryEntry
import kotlinx.coroutines.flow.Flow
import java.util.*

class HistoryManager(private val context: Context) {
    
    private val database = BrowserDatabase.getDatabase(context)
    
    fun getAllHistory(): Flow<List<HistoryEntry>> {
        return database.historyDao().getAllHistory()
    }
    
    suspend fun addHistoryEntry(url: String, title: String) {
        val existingEntry = database.historyDao().getHistoryEntry(url)
        
        if (existingEntry != null) {
            // Update existing entry
            val updatedEntry = existingEntry.copy(
                title = title,
                dateVisited = Date(),
                visitCount = existingEntry.visitCount + 1
            )
            database.historyDao().updateHistoryEntry(updatedEntry)
        } else {
            // Create new entry
            val newEntry = HistoryEntry(
                url = url,
                title = title,
                dateVisited = Date(),
                visitCount = 1
            )
            database.historyDao().insertHistoryEntry(newEntry)
        }
    }
    
    suspend fun removeHistoryEntry(entry: HistoryEntry) {
        database.historyDao().deleteHistoryEntry(entry)
    }
    
    suspend fun clearHistory() {
        database.historyDao().clearAllHistory()
    }
    
    suspend fun clearOldHistory(daysToKeep: Int = 30) {
        val cutoffDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysToKeep)
        }.time
        database.historyDao().deleteOldHistory(cutoffDate)
    }
    
    fun searchHistory(query: String): Flow<List<HistoryEntry>> {
        return database.historyDao().searchHistory(query)
    }
    
    suspend fun getHistoryCount(): Int {
        return database.historyDao().getHistoryCount()
    }
    
    suspend fun getHistoryEntry(url: String): HistoryEntry? {
        return database.historyDao().getHistoryEntry(url)
    }
} 