package com.example.noorahlulbayt

import androidx.room.*
import com.example.noorahlulbayt.database.Converters
import kotlinx.coroutines.flow.Flow
import java.util.*

@Entity(tableName = "blocked_urls")
data class BlockedUrl(
    @PrimaryKey val url: String,
    val isBlocked: Boolean = true,
    val reason: String = "",
    val lastChecked: Date = Date(),
    val expiresAt: Date? = null
)

@Dao
interface BlockedUrlDao {
    @Query("SELECT * FROM blocked_urls WHERE url = :url")
    suspend fun getBlockedUrl(url: String): BlockedUrl?
    
    @Query("SELECT * FROM blocked_urls WHERE isBlocked = 1")
    fun getAllBlockedUrls(): Flow<List<BlockedUrl>>
    
    @Query("SELECT * FROM blocked_urls WHERE expiresAt IS NOT NULL AND expiresAt < :currentDate")
    suspend fun getExpiredUrls(currentDate: Date): List<BlockedUrl>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUrl(blockedUrl: BlockedUrl)
    
    @Delete
    suspend fun deleteBlockedUrl(blockedUrl: BlockedUrl)
    
    @Query("DELETE FROM blocked_urls WHERE expiresAt IS NOT NULL AND expiresAt < :currentDate")
    suspend fun deleteExpiredUrls(currentDate: Date)
    
    @Query("SELECT COUNT(*) FROM blocked_urls WHERE isBlocked = 1")
    suspend fun getBlockedUrlsCount(): Int
}

@Database(entities = [BlockedUrl::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class BlocklistDatabase : RoomDatabase() {
    abstract fun blockedUrlDao(): BlockedUrlDao
    
    companion object {
        @Volatile
        private var INSTANCE: BlocklistDatabase? = null
        
        fun getDatabase(context: android.content.Context): BlocklistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    BlocklistDatabase::class.java,
                    "blocklist_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class BlocklistRepository(private val blockedUrlDao: BlockedUrlDao) {
    
    suspend fun addBlockedUrl(url: String, reason: String = "Profanity detected") {
        val expiresAt = Calendar.getInstance().apply {
            add(Calendar.HOUR, 24) // 24-hour expiry
        }.time
        
        val blockedUrl = BlockedUrl(
            url = url,
            isBlocked = true,
            reason = reason,
            lastChecked = Date(),
            expiresAt = expiresAt
        )
        
        blockedUrlDao.insertBlockedUrl(blockedUrl)
    }
    
    suspend fun isUrlBlocked(url: String): Boolean {
        val blockedUrl = blockedUrlDao.getBlockedUrl(url)
        return blockedUrl?.isBlocked == true
    }
    
    fun getAllBlockedUrls(): Flow<List<BlockedUrl>> {
        return blockedUrlDao.getAllBlockedUrls()
    }
    
    suspend fun removeBlockedUrl(url: String) {
        val blockedUrl = blockedUrlDao.getBlockedUrl(url)
        blockedUrl?.let {
            blockedUrlDao.deleteBlockedUrl(it)
        }
    }
    
    suspend fun cleanupExpiredUrls() {
        val currentDate = Date()
        blockedUrlDao.deleteExpiredUrls(currentDate)
    }
    
    suspend fun getBlockedUrlsCount(): Int {
        return blockedUrlDao.getBlockedUrlsCount()
    }
} 