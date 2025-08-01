package com.example.noorahlulbaytcompanion

import android.content.Context
import androidx.room.*
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
                        
                        // TODO: Save to database (simplified for now)
                        
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
        // Simplified implementation - return mock data for now
        return listOf(
            "Fajr" to "05:30",
            "Dhuhr" to "12:15",
            "Asr" to "15:45",
            "Maghrib" to "18:30",
            "Isha" to "20:00"
        )
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

// Room database components removed for simplified build
// TODO: Re-implement database storage later 