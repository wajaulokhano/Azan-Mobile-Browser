package com.example.noorahlulbaytcompanion

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.*
import java.util.concurrent.TimeUnit

class PrayerTimesWorker(private val context: Context) {
    
    private val apiService = Retrofit.Builder()
        .baseUrl("http://api.aladhan.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(PrayerTimesApi::class.java)
    
    suspend fun fetchPrayerTimes() {
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getPrayerTimes(
                    city = "Najaf",
                    country = "Iraq",
                    method = 3
                )
                
                if (response.isSuccessful) {
                    response.body()?.let { prayerTimesResponse ->
                        val timings = prayerTimesResponse.data.timings
                        val date = prayerTimesResponse.data.date.readable
                        
                        // Save to database
                        val database = AppDatabase.getDatabase(context)
                        val prayerTime = PrayerTimeEntity(
                            date = date,
                            fajr = timings.Fajr,
                            dhuhr = timings.Dhuhr,
                            asr = timings.Asr,
                            maghrib = timings.Maghrib,
                            isha = timings.Isha
                        )
                        
                        database.prayerTimeDao().insertPrayerTime(prayerTime)
                        
                        // Schedule Azan blocking
                        scheduleAzanBlocking(timings)
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    suspend fun getPrayerTimes(): List<Pair<String, String>> {
        val database = AppDatabase.getDatabase(context)
        val prayerTime = database.prayerTimeDao().getLatestPrayerTime()
        
        return prayerTime?.let {
            listOf(
                "Fajr" to it.fajr,
                "Dhuhr" to it.dhuhr,
                "Asr" to it.asr,
                "Maghrib" to it.maghrib,
                "Isha" to it.isha
            )
        } ?: emptyList()
    }
    
    private fun scheduleAzanBlocking(timings: Timings) {
        val workManager = WorkManager.getInstance(context)
        
        // Schedule Fajr blocking
        schedulePrayerBlocking("Fajr", timings.Fajr, workManager)
        schedulePrayerBlocking("Dhuhr", timings.Dhuhr, workManager)
        schedulePrayerBlocking("Asr", timings.Asr, workManager)
        schedulePrayerBlocking("Maghrib", timings.Maghrib, workManager)
        schedulePrayerBlocking("Isha", timings.Isha, workManager)
    }
    
    private fun schedulePrayerBlocking(prayerName: String, prayerTime: String, workManager: WorkManager) {
        val prayerDateTime = parsePrayerTime(prayerTime)
        val now = Calendar.getInstance()
        
        if (prayerDateTime.after(now)) {
            val delay = prayerDateTime.timeInMillis - now.timeInMillis
            
            val azanBlockWork = OneTimeWorkRequestBuilder<AzanBlockWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("azan_block_$prayerName")
                .build()
            
            workManager.enqueue(azanBlockWork)
        }
    }
    
    private fun parsePrayerTime(timeString: String): Calendar {
        val calendar = Calendar.getInstance()
        val timeParts = timeString.split(":")
        calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
        calendar.set(Calendar.MINUTE, timeParts[1].toInt())
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar
    }
}

interface PrayerTimesApi {
    @GET("v1/timingsByCity")
    suspend fun getPrayerTimes(
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int
    ): retrofit2.Response<PrayerTimesResponse>
}

data class PrayerTimesResponse(
    val data: PrayerData
)

data class PrayerData(
    val timings: Timings,
    val date: DateInfo
)

data class Timings(
    val Fajr: String,
    val Dhuhr: String,
    val Asr: String,
    val Maghrib: String,
    val Isha: String
)

data class DateInfo(
    val readable: String
)

@Entity(tableName = "prayer_times")
data class PrayerTimeEntity(
    @PrimaryKey val date: String,
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String
)

@Dao
interface PrayerTimeDao {
    @Query("SELECT * FROM prayer_times ORDER BY date DESC LIMIT 1")
    suspend fun getLatestPrayerTime(): PrayerTimeEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTime(prayerTime: PrayerTimeEntity)
    
    @Query("DELETE FROM prayer_times WHERE date < :cutoffDate")
    suspend fun deleteOldPrayerTimes(cutoffDate: String)
}

@Database(entities = [PrayerTimeEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerTimeDao(): PrayerTimeDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prayer_times_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 