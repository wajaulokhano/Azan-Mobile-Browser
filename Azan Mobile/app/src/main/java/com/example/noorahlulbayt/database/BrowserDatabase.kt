package com.example.noorahlulbayt.database

import androidx.room.*
import com.example.noorahlulbayt.models.*
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY dateAdded DESC")
    fun getAllFavorites(): Flow<List<Favorite>>
    
    @Query("SELECT * FROM favorites WHERE url = :url")
    suspend fun getFavorite(url: String): Favorite?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorite)
    
    @Delete
    suspend fun deleteFavorite(favorite: Favorite)
    
    @Query("DELETE FROM favorites WHERE url = :url")
    suspend fun deleteFavoriteByUrl(url: String)
    
    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoritesCount(): Int
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY dateStarted DESC")
    fun getAllDownloads(): Flow<List<Download>>
    
    @Query("SELECT * FROM downloads WHERE status = :status")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<Download>>
    
    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownload(id: String): Download?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: Download)
    
    @Update
    suspend fun updateDownload(download: Download)
    
    @Delete
    suspend fun deleteDownload(download: Download)
    
    @Query("DELETE FROM downloads WHERE status = :status")
    suspend fun deleteDownloadsByStatus(status: DownloadStatus)
    
    @Query("SELECT COUNT(*) FROM downloads WHERE status = :status")
    suspend fun getDownloadsCount(status: DownloadStatus): Int
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY dateVisited DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>
    
    @Query("SELECT * FROM history WHERE url = :url")
    suspend fun getHistoryEntry(url: String): HistoryEntry?
    
    @Query("SELECT * FROM history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%'")
    fun searchHistory(query: String): Flow<List<HistoryEntry>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: HistoryEntry)
    
    @Update
    suspend fun updateHistoryEntry(entry: HistoryEntry)
    
    @Delete
    suspend fun deleteHistoryEntry(entry: HistoryEntry)
    
    @Query("DELETE FROM history WHERE dateVisited < :cutoffDate")
    suspend fun deleteOldHistory(cutoffDate: Date)
    
    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
    
    @Query("SELECT COUNT(*) FROM history")
    suspend fun getHistoryCount(): Int
}

@Database(
    entities = [
        Favorite::class,
        Download::class,
        HistoryEntry::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao
    
    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null
        
        fun getDatabase(context: android.content.Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "browser_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 